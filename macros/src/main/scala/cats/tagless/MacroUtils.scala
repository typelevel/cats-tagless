/*
 * Copyright 2017 Kailuo Wang
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

private[tagless] abstract class MacroUtils {
  val c: blackbox.Context

  import c.universe._

  class TypeDefinition(val defn: ClassDef, maybeCompanion: Option[ModuleDef]) {
    val name = defn.name
    val tparams = defn.tparams
    val hasOneTypeParam = tparams.lengthCompare(1) == 0
    val impl = defn.impl

    def ident = Ident(name)
    def typeSig = AppliedTypeTree(ident, tArgs(tparams))
    def applied(targs: Ident*) = AppliedTypeTree(ident, targs.toList)
    def companion = maybeCompanion.getOrElse(
        q"object ${name.toTermName} { }".asInstanceOf[ModuleDef]
      )

    lazy val abstractTypeMembers: Seq[TypeDef] = defn.impl.body.collect { case dt: TypeDef if dt.mods.hasFlag(Flag.DEFERRED) => dt }
    def hasAbstractTypeMembers = abstractTypeMembers.nonEmpty

    private def Name0(name: TypeName) = TypeName(name.decodedName.toString + "0")
    lazy val fullyRefinedTypeMembers: Seq[TypeDef] = {
      abstractTypeMembers.map { td =>
        val typeDefBody =
          if(td.tparams.nonEmpty)
            AppliedTypeTree(Ident(Name0(td.name)), tArgs(td.tparams))
          else
            Ident(Name0(td.name))
        TypeDef(Modifiers(), td.name, td.tparams, typeDefBody)
      }
    }

    lazy val refinedTParams: Seq[TypeDef] =
      abstractTypeMembers.map(
        defn =>
          TypeDef(Modifiers(Flag.PARAM), Name0(defn.name), defn.tparams, defn.rhs)
      )

    def newDependentTypeMembers(definedAt: TermName) = {
      abstractTypeMembers.map(
        t => TypeDef(Modifiers(), t.name, t.tparams,  tq"$definedAt.${t.name}[..${tArgs(t.tparams)}]")
      )
    }
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

  def enrich(defn: Seq[c.Tree])(f: TypeDefinition => Seq[Tree]) : Tree = {
    defn match {
      case TypeDefinition.FromAnnottees(td) => val trees = f(td)
        q"..$trees"
      case _ => abort(s"$defn is not a class or trait.")
    }
  }

  case class AlgDefn(cls: TypeDefinition, effectType: TypeDef, extraTParams: Seq[TypeDef], enriched: List[Tree] = Nil) {
    def typeName = cls.ident
    def name = cls.name.decodedName.toString

    private def tArgs(effTpeName: TypeName): List[Ident] = cls.tparams.map {
      case `effectType` => Ident(effTpeName)
      case tp =>  Ident(tp.name)
    }

    def newTypeSig(newEffectTypeName: TypeName): AppliedTypeTree = AppliedTypeTree(typeName, tArgs(newEffectTypeName))
    def newTypeSig(newEffectTypeName: String): AppliedTypeTree = newTypeSig(TypeName(newEffectTypeName))
    def newTypeSig(newEffectType: TypeDef): AppliedTypeTree = newTypeSig(newEffectType.name)

    private def refinedTypeSig(newEffectTypeName: TypeName, refinedTypes: Seq[TypeDef]): TypTree = {
      val typeSig = newTypeSig(newEffectTypeName)
      if (refinedTypes.isEmpty) typeSig else tq"$typeSig { ..$refinedTypes }".asInstanceOf[CompoundTypeTree]
    }
    private def refinedTypeSig(newEffectTypeName: String,  refinedTypes: Seq[TypeDef]): TypTree =
      refinedTypeSig(TypeName(newEffectTypeName), refinedTypes)

    def fullyRefinedTypeSig(newEffectTypeName: String) = refinedTypeSig(newEffectTypeName, cls.fullyRefinedTypeMembers)
    def fullyRefinedTypeSig(newEffectType: TypeDef) = refinedTypeSig(newEffectType.name, cls.fullyRefinedTypeMembers)

    def dependentRefinedTypeSig(newEffectType: TypeDef, dependent: TermName): TypTree =
      refinedTypeSig(newEffectType.name, cls.newDependentTypeMembers(dependent))


    def withStats(stats: Tree) = copy(enriched = stats :: enriched)
    def trees = {
      val enrichedCompanion = addStats(cls.companion, enriched)
      Seq(cls.defn, enrichedCompanion)
    }


    def forVaryingEffectType(gen: (Tree, Seq[TypeDef]) => Tree) = {
      val typeLambdaVaryingEffect = if (cls.hasOneTypeParam) typeName else tq"({type λ[T] = ${newTypeSig("T")}})#λ"
      val newDef = gen(typeLambdaVaryingEffect, extraTParams)
      withStats(newDef)
    }
    def forVaryingHigherKindedEffectType(gen: (Tree, Seq[TypeDef]) => Tree) = {
      val typeLambdaVaryingHigherKindedEffect = if (cls.hasOneTypeParam) typeName else tq"({type λ[Ƒ[_]] = ${newTypeSig("Ƒ")}})#λ"
      val newDef = gen(typeLambdaVaryingHigherKindedEffect, extraTParams)
      withStats(newDef)
    }
    def forVaryingHigherKindedEffectTypeFullyRefined(gen: (Tree, Seq[TypeDef]) => Tree) = if (cls.hasAbstractTypeMembers) {
      val typeLambda =
        if (cls.hasOneTypeParam)
          tq"$typeName { ..${cls.fullyRefinedTypeMembers} }"
        else tq"({type λ[Ƒ[_]] = ${fullyRefinedTypeSig("Ƒ")}})#λ"
      val fullyRefinedTypeParams = extraTParams ++ cls.refinedTParams

      val newDef = gen(typeLambda, fullyRefinedTypeParams)
      withStats(newDef)
    } else forVaryingHigherKindedEffectType(gen)

    def forAlgebraType(gen: (AppliedTypeTree, Seq[TypeDef]) => Tree) = withStats(gen(cls.typeSig, cls.tparams))
  }

  object AlgDefn {
    def from(cls: TypeDefinition, higherKinded: Boolean = true): Option[AlgDefn] = {
      { if (higherKinded)
        cls.tparams.find(_.tparams.nonEmpty)
      else
        cls.tparams.lastOption
      }.map(effectType =>
        AlgDefn(cls, effectType, cls.tparams.filterNot(Set(effectType)))
      )
    }
  }

  def enrichAlgebra(defn: Seq[c.Tree], higherKinded: Boolean = true)(f: AlgDefn => AlgDefn): Tree = {
    enrich(defn){ cls =>
      val ad = AlgDefn.from(cls, higherKinded).getOrElse(abort(if (higherKinded) s"${cls.name} does not have an higher-kinded type parameter, e.g. F[_]"
                                                               else s"${cls.name} does not have any type parameter"))
      val enriched = f(ad)
      enriched.trees
    }
  }

  def abort(msg: String) = c.abort(c.enclosingPosition, msg)

  def addStats(obj: ModuleDef, stats: Seq[Tree]): ModuleDef = if (stats.isEmpty) obj else {
    val impl = obj.impl
    ModuleDef(obj.mods, obj.name, Template(impl.parents, impl.self, impl.body ++ stats))
  }

  def createTypeParam(name: Name, tparamsTemplate: List[TypeDef]) = {
    val tparams = tparamsTemplate.map {
      case TypeDef(mods, _, _, typeBounds @ TypeBoundsTree(_, _)) =>
        TypeDef(
          Modifiers(Flag.PARAM | mods.flags),
          typeNames.WILDCARD,
          Nil,
          typeBounds
        )
      case _ =>
        TypeDef(
          Modifiers(Flag.PARAM),
          typeNames.WILDCARD,
          Nil,
          TypeBoundsTree(EmptyTree, EmptyTree)
        )
    }
    TypeDef(Modifiers(Flag.PARAM), name.toTypeName, tparams, TypeBoundsTree(EmptyTree, EmptyTree))
  }

  def createTypeParam(name: Name, arity: Int, flags: FlagSet = NoFlags): TypeDef = {
    val tparams = List.fill(arity)(TypeDef(Modifiers(Flag.PARAM | flags), typeNames.WILDCARD, Nil, TypeBoundsTree(EmptyTree, EmptyTree)))
    TypeDef(Modifiers(Flag.PARAM), name.toTypeName, tparams, TypeBoundsTree(EmptyTree, EmptyTree))
  }
  def createTypeParam(name: String, arity: Int): TypeDef = createTypeParam(TypeName(name), arity)
  def createFreshTypeParam(name: String, arity: Int): TypeDef = createTypeParam(c.freshName(name), arity)

  def tArgs(tparam: TypeDef): Ident = Ident(tparam.name)
  def tArgs(tparam1: TypeDef, tparam2: TypeDef): List[Ident] = List(tArgs(tparam1), tArgs(tparam2))
  def tArgs(tparams: List[TypeDef]): List[Ident] = tparams.map(tArgs)

  def arguments(params: Seq[Tree]): Seq[TermName] =
    params.collect {
      case ValDef(_, name, _, _) => name
    }

  lazy val autoDerive: Boolean = c.prefix.tree match {
    case q"new ${_}(${arg: Boolean})"                  => arg
    case q"new ${_}(autoDerivation = ${arg: Boolean})" => arg
    case _                                             => true
  }

}
