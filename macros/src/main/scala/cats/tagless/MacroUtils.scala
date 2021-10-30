/*
 * Copyright 2019 cats-tagless maintainers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cats.tagless
import scala.collection.immutable.Seq
import scala.reflect.macros.blackbox

abstract private[tagless] class MacroUtils {
  val c: blackbox.Context

  import c.universe._

  class TypeDefinition(val defn: ClassDef, maybeCompanion: Option[ModuleDef]) {
    val name = defn.name
    val tparams = defn.tparams
    val impl = defn.impl

    def ident = Ident(name)
    def typeSig = AppliedTypeTree(ident, tArgs(tparams))
    def applied(targs: Ident*) = AppliedTypeTree(ident, targs.toList)
    def companion = maybeCompanion.getOrElse(
      q"object ${name.toTermName} { }".asInstanceOf[ModuleDef]
    )

    // TODO: this will not work for inherited type members. We'll need to figure out later how to make it work.
    lazy val abstractTypeMembers: Seq[TypeDef] = defn.impl.body.collect {
      case dt: TypeDef if dt.mods.hasFlag(Flag.DEFERRED) => dt
    }
    def hasAbstractTypeMembers = abstractTypeMembers.nonEmpty

    private def Name0(name: TypeName) = TypeName(name.decodedName.toString + "0")
    lazy val fullyRefinedTypeMembers: Seq[TypeDef] =
      abstractTypeMembers.map { td =>
        val typeDefBody =
          if (td.tparams.nonEmpty)
            AppliedTypeTree(Ident(Name0(td.name)), tArgs(td.tparams))
          else
            Ident(Name0(td.name))
        TypeDef(Modifiers(), td.name, td.tparams, typeDefBody)
      }

    lazy val refinedTParams: Seq[TypeDef] =
      abstractTypeMembers.map(defn => TypeDef(Modifiers(Flag.PARAM), Name0(defn.name), defn.tparams, defn.rhs))

    def newDependentTypeMembers(definedAt: TermName) =
      abstractTypeMembers.map(t =>
        TypeDef(Modifiers(), t.name, t.tparams, tq"$definedAt.${t.name}[..${tArgs(t.tparams)}]")
      )
  }

  object TypeDefinition {
    object FromAnnottees {
      def unapply(arg: Seq[c.Tree]): Option[TypeDefinition] = arg match {
        case Seq(t: ClassDef, companion: ModuleDef) => Some(new TypeDefinition(t, Some(companion)))
        case Seq(t: ClassDef) => Some(new TypeDefinition(t, None))
        case _ => None
      }
    }
  }

  def enrich(defn: Seq[c.Tree])(f: TypeDefinition => Seq[Tree]): Tree =
    defn match {
      case TypeDefinition.FromAnnottees(td) =>
        val trees = f(td)
        q"..$trees"
      case _ => abort(s"$defn is not a class or trait.")
    }

  sealed abstract class AlgDefn(numberOfEffectTypeParams: Int) {
    private val needsTypeLambda = cls.tparams.lengthCompare(numberOfEffectTypeParams) != 0

    def cls: TypeDefinition
    final def typeName = cls.ident
    final def name = cls.name.decodedName.toString

    def extraTypeParams: Seq[TypeDef]

    protected def typeLambdaVaryingEffect: Tree
    final def forVaryingEffectType(gen: (Tree, Seq[TypeDef]) => Tree) =
      gen(if (!needsTypeLambda) typeName else typeLambdaVaryingEffect, extraTypeParams)

    protected def typeLambdaVaryingEffectFullyRefined: Tree
    final def forVaryingEffectTypeFullyRefined(gen: (Tree, Seq[TypeDef]) => Tree) = if (cls.hasAbstractTypeMembers) {
      val typeLambda =
        if (!needsTypeLambda)
          tq"$typeName { ..${cls.fullyRefinedTypeMembers} }"
        else
          typeLambdaVaryingEffectFullyRefined
      val fullyRefinedTypeParams = extraTypeParams ++ cls.refinedTParams
      gen(typeLambda, fullyRefinedTypeParams)
    } else forVaryingEffectType(gen)

    final def forAlgebraType(gen: (AppliedTypeTree, Seq[TypeDef]) => Tree) = gen(cls.typeSig, cls.tparams)
  }

  object AlgDefn {

    case class UnaryAlg(
        override val cls: TypeDefinition,
        effectType: TypeDef,
        effectTypeArity: Int,
        override val extraTypeParams: Seq[TypeDef]
    ) extends AlgDefn(1) {
      private def tArgs(effTpeName: TypeName): List[Ident] = cls.tparams.map {
        case `effectType` => Ident(effTpeName)
        case tp => Ident(tp.name)
      }

      def newTypeSig(newEffectTypeName: TypeName): AppliedTypeTree = AppliedTypeTree(typeName, tArgs(newEffectTypeName))
      def newTypeSig(newEffectTypeName: String): AppliedTypeTree = newTypeSig(TypeName(newEffectTypeName))
      def newTypeSig(newEffectType: TypeDef): AppliedTypeTree = newTypeSig(newEffectType.name)

      private def refinedTypeSig(newEffectTypeName: TypeName, refinedTypes: Seq[TypeDef]): TypTree = {
        val typeSig = newTypeSig(newEffectTypeName)
        if (refinedTypes.isEmpty) typeSig else tq"$typeSig { ..$refinedTypes }".asInstanceOf[CompoundTypeTree]
      }
      private def refinedTypeSig(newEffectTypeName: String, refinedTypes: Seq[TypeDef]): TypTree =
        refinedTypeSig(TypeName(newEffectTypeName), refinedTypes)

      def fullyRefinedTypeSig(newEffectTypeName: String) =
        refinedTypeSig(newEffectTypeName, cls.fullyRefinedTypeMembers)
      def fullyRefinedTypeSig(newEffectType: TypeDef) = refinedTypeSig(newEffectType.name, cls.fullyRefinedTypeMembers)

      def dependentRefinedTypeSig(newEffectType: TypeDef, dependent: TermName): TypTree =
        refinedTypeSig(newEffectType.name, cls.newDependentTypeMembers(dependent))

      override protected def typeLambdaVaryingEffect =
        tq"({type λ[${createTypeParam("Ƒ", effectTypeArity)}] = ${newTypeSig("Ƒ")}})#λ"
      override protected def typeLambdaVaryingEffectFullyRefined =
        tq"({type λ[${createTypeParam("Ƒ", effectTypeArity)}] = ${fullyRefinedTypeSig("Ƒ")}})#λ"
    }

    case class BinaryAlg(
        override val cls: TypeDefinition,
        effectType1: (TypeDef, Int),
        effectType2: (TypeDef, Int),
        override val extraTypeParams: Seq[TypeDef]
    ) extends AlgDefn(2) {
      private def tArgs(effTpeName1: TypeName, effTpeName2: TypeName): List[Ident] = cls.tparams.map(tp =>
        if (tp == effectType1._1) Ident(effTpeName1)
        else if (tp == effectType2._1) Ident(effTpeName2)
        else Ident(tp.name)
      )

      def newTypeSig(newEffectTypeName1: TypeName, newEffectTypeName2: TypeName): AppliedTypeTree =
        AppliedTypeTree(typeName, tArgs(newEffectTypeName1, newEffectTypeName2))
      def newTypeSig(newEffectTypeName1: String, newEffectTypeName2: String): AppliedTypeTree =
        newTypeSig(TypeName(newEffectTypeName1), TypeName(newEffectTypeName2))

      private def refinedTypeSig(
          newEffectTypeName1: TypeName,
          newEffectTypeName2: TypeName,
          refinedTypes: Seq[TypeDef]
      ): TypTree = {
        val typeSig = newTypeSig(newEffectTypeName1, newEffectTypeName2)
        if (refinedTypes.isEmpty) typeSig else tq"$typeSig { ..$refinedTypes }".asInstanceOf[CompoundTypeTree]
      }
      private def refinedTypeSig(
          newEffectTypeName1: String,
          newEffectTypeName2: String,
          refinedTypes: Seq[TypeDef]
      ): TypTree =
        refinedTypeSig(TypeName(newEffectTypeName1), TypeName(newEffectTypeName2), refinedTypes)

      def fullyRefinedTypeSig(newEffectTypeName1: String, newEffectTypeName2: String) =
        refinedTypeSig(newEffectTypeName1, newEffectTypeName2, cls.fullyRefinedTypeMembers)

      private def asTypeParams(name1: String, name2: String) =
        List(createTypeParam(name1, effectType1._2), createTypeParam(name2, effectType2._2))
      override protected def typeLambdaVaryingEffect =
        tq"({type λ[..${asTypeParams("Ƒ1", "Ƒ2")}] = ${newTypeSig("Ƒ1", "Ƒ2")}})#λ"
      override protected def typeLambdaVaryingEffectFullyRefined =
        tq"({type λ[..${asTypeParams("Ƒ1", "Ƒ2")}] = ${fullyRefinedTypeSig("Ƒ1", "Ƒ2")}})#λ"
    }

  }

  sealed abstract class AlgebraResolver {
    type Defn <: AlgDefn
    def apply(cls: TypeDefinition): Either[String, Defn]
  }

  object AlgebraResolver {
    type Unary = AlgebraResolver {
      type Defn = AlgDefn.UnaryAlg
    }
    type Binary = AlgebraResolver {
      type Defn = AlgDefn.BinaryAlg
    }

    def FirstHigherKindedTypeParam: Unary = new AlgebraResolver {
      override type Defn = AlgDefn.UnaryAlg
      override def apply(cls: TypeDefinition) =
        cls.tparams
          .find(_.tparams.lengthCompare(1) == 0)
          .fold[Either[String, Defn]](
            Left(s"${cls.name} does not have a higher-kinded type parameter of shape F[_]")
          )(effectType => Right(AlgDefn.UnaryAlg(cls, effectType, 1, cls.tparams.filterNot(Set(effectType)))))
    }

    def LastRegularTypeParam: Unary = new AlgebraResolver {
      override type Defn = AlgDefn.UnaryAlg
      override def apply(cls: TypeDefinition) =
        cls.tparams.lastOption
          .filter(_.tparams.isEmpty)
          .fold[Either[String, Defn]](
            Left(s"${cls.name} must have a type paramter of shape F as its last type parameter")
          )(effectType => Right(AlgDefn.UnaryAlg(cls, effectType, 0, cls.tparams.dropRight(1))))
    }

    def AnyLastTypeParam: Unary = new AlgebraResolver {
      override type Defn = AlgDefn.UnaryAlg
      override def apply(cls: TypeDefinition) =
        cls.tparams.lastOption.fold[Either[String, Defn]](
          Left(s"${cls.name} does not have any type parameter")
        )(effectType => Right(AlgDefn.UnaryAlg(cls, effectType, effectType.tparams.length, cls.tparams.dropRight(1))))
    }

    def TwoLastRegularTypeParams: Binary = new AlgebraResolver {
      override type Defn = AlgDefn.BinaryAlg
      override def apply(cls: TypeDefinition) =
        cls.tparams.takeRight(2).filter(_.tparams.isEmpty) match {
          case t1 :: t2 :: Nil =>
            Right(AlgDefn.BinaryAlg(cls, (t1, 0), (t2, 0), cls.tparams.dropRight(2)))
          case _ =>
            Left(
              s"${cls.name} must have two type parameters of shape F as its last type parameters"
            )
        }
    }

  }

  def enrichAlgebra[A <: AlgebraResolver](defn: Seq[c.Tree], resolver: A = AlgebraResolver.FirstHigherKindedTypeParam)(
      f: resolver.Defn => Seq[Tree]
  ): Tree =
    enrich(defn) { cls =>
      resolver(cls) match {
        case Right(ad) =>
          val enrichedCompanion = addStats(cls.companion, f(ad))
          Seq(cls.defn, enrichedCompanion)
        case Left(err) => abort(err)
      }
    }

  def abort(msg: String) = c.abort(c.enclosingPosition, msg)

  def addStats(obj: ModuleDef, stats: Seq[Tree]): ModuleDef = if (stats.isEmpty) obj
  else {
    val impl = obj.impl
    ModuleDef(obj.mods, obj.name, Template(impl.parents, impl.self, impl.body ++ stats))
  }

  def createTypeParam(name: Name, arity: Int): TypeDef = {
    val tparams =
      List.fill(arity)(TypeDef(Modifiers(Flag.PARAM), typeNames.WILDCARD, Nil, TypeBoundsTree(EmptyTree, EmptyTree)))
    TypeDef(Modifiers(Flag.PARAM), name.toTypeName, tparams, TypeBoundsTree(EmptyTree, EmptyTree))
  }
  def createTypeParam(name: String, arity: Int): TypeDef = createTypeParam(TypeName(name), arity)
  def createFreshTypeParam(name: String, arity: Int): TypeDef = createTypeParam(c.freshName(name), arity)

  def tArgs(tparam: TypeDef): Ident = Ident(tparam.name)
  def tArgs(tparam1: TypeDef, tparam2: TypeDef): List[Ident] = List(tArgs(tparam1), tArgs(tparam2))
  def tArgs(tparams: List[TypeDef]): List[Ident] = tparams.map(tArgs)

  def arguments(params: Seq[Tree]): Seq[Tree] =
    params.collect {
      case ValDef(_, name, AppliedTypeTree(ref: RefTree, _ :: Nil), _)
          if ref.name == definitions.RepeatedParamClass.name =>
        q"$name: _*"
      case ValDef(_, name, _, _) => Ident(name)
    }

  def argumentLists(paramLists: Seq[Seq[Tree]]): Seq[Seq[Tree]] =
    paramLists.map(arguments)

  def typeClassInstance(name: TermName, typeParams: Seq[TypeDef], resultType: Tree, rhs: Tree): Tree =
    if (typeParams.isEmpty) q"implicit val $name: $resultType = $rhs"
    else q"implicit def $name[..$typeParams]: $resultType = $rhs"

  lazy val autoDerive: Boolean = c.prefix.tree match {
    case q"new ${_}(${arg: Boolean})" => arg
    case q"new ${_}(autoDerivation = ${arg: Boolean})" => arg
    case _ => true
  }

}
