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

import cats.arrow.Profunctor
import cats.data.{ReaderT, Tuple2K}
import cats.tagless.aop.{Aspect, Instrument, Instrumentation}
import cats.{Bifunctor, Contravariant, FlatMap, Functor, Invariant, SemigroupK, Semigroupal}

import scala.reflect.macros.blackbox

class DeriveMacros(val c: blackbox.Context) {
  import c.internal.*
  import c.universe.*

  type Transform[T] = PartialFunction[T, T]
  type TransformParam = PartialFunction[Parameter, Tree]

  /** A reified parameter definition with some useful methods for transforming it. */
  case class Parameter(name: TermName, signature: Type, modifiers: Modifiers) {
    def displayName: String = name.decodedName.toString
  }

  /** A reified method definition with some useful methods for transforming it. */
  case class Method(
      name: TermName,
      signature: Type,
      typeParams: List[TypeDef],
      paramLists: List[List[ValDef]],
      returnType: Type,
      body: Tree
  ) {
    def displayName: String = name.decodedName.toString
    def occursInSignature(symbol: Symbol): Boolean = occursIn(signature)(symbol)
    def occursInReturn(symbol: Symbol): Boolean = occursIn(returnType)(symbol)
    def occursOnlyInReturn(symbol: Symbol): Boolean =
      occursInReturn(symbol) && !signature.paramLists.iterator.flatten.exists(p => occursIn(p.info)(symbol))

    /** Construct a new set of parameter lists after substituting some type symbols. */
    def transformedParamLists(types: Transform[Type]): List[List[ValDef]] =
      for (ps <- paramLists) yield for (p <- ps) yield {
        val oldType = p.tpt.tpe
        val newType = types.applyOrElse(oldType, identity[Type])
        if (newType == oldType) p else ValDef(p.mods, p.name, TypeTree(newType), p.rhs)
      }

    /** Construct a new set of argument lists based on their name and type. */
    def transformedArgLists(f: TransformParam = PartialFunction.empty): List[List[Tree]] = {
      def id(param: Parameter): Tree = Ident(param.name)

      val f_* : Parameter => Tree = {
        case Parameter(pn, RepeatedParam(pt), pm) =>
          q"${f.andThen(arg => q"for ($pn <- $pn) yield $arg").applyOrElse(Parameter(pn, pt, pm), id)}: _*"
        case Parameter(pn, ByNameParam(pt), pm) =>
          f.applyOrElse(Parameter(pn, pt, pm), id)
        case param =>
          f.applyOrElse(param, id)
      }

      for (ps <- paramLists)
        yield for (p <- ps)
          yield f_*(Parameter(p.name, p.tpt.tpe, p.mods))
    }

    /** Transform this method into another by applying transformations to types, arguments and body. */
    def transform(instance: Tree)(types: Transform[Type] = PartialFunction.empty)(
        argLists: TransformParam = PartialFunction.empty
    )(body: Transform[Tree] = PartialFunction.empty): Method = copy(
      paramLists = transformedParamLists(types),
      returnType = types.applyOrElse(returnType, identity[Type]),
      body = body.applyOrElse(delegate(instance, transformedArgLists(argLists)), identity[Tree])
    )

    /** Transform this method into another by applying substitution to types and transformations to arguments / body. */
    def transformSubst(instance: Symbol, types: (Symbol, Symbol)*)(
        argLists: TransformParam = PartialFunction.empty
    )(body: Transform[Tree] = PartialFunction.empty): Method = {
      val (from, to) = types.toList.unzip
      transform(Ident(instance)) { case tpe => tpe.substituteSymbols(from, to) }(argLists)(body)
    }

    /** Delegate this method to an existing instance, optionally providing different argument lists. */
    def delegate(to: Tree, argLists: List[List[Tree]] = transformedArgLists()): Tree = {
      val typeArgs = for (tp <- typeParams) yield typeRef(NoPrefix, tp.symbol, Nil)
      q"$to.$name[..$typeArgs](...$argLists)"
    }

    /** The definition of this method as a Scala tree. */
    def definition: Tree = q"override def $name[..$typeParams](...$paramLists): $returnType = $body"

    /** Summon an implicit instance of `A`'s type constructor applied to `typeArgs` if one exists in scope. */
    def summon[A: TypeTag](typeArgs: Type*): Tree = {
      val tpe = appliedType(typeOf[A].typeConstructor, typeArgs*)
      c.inferImplicitValue(tpe).orElse(abort(s"could not find implicit value of type $tpe in method $displayName"))
    }

    /** Summon an implicit instance of `F[a =>> returnType]` if one exists in scope. */
    def summonF[F[_[_]]](a: Symbol, returnType: Type)(implicit tag: TypeTag[F[Any]]): Tree =
      summon[F[Any]](polyType(a :: Nil, returnType))

    /** Summon an implicit instance of `K[a =>> returnType]` if one exists in scope. */
    def summonK[K[_[_[_]]]](a: Symbol, returnType: Type)(implicit tag: TypeTag[K[Any]]): Tree =
      summon[K[Any]](polyType(a :: Nil, returnType))

    /** Summon an implicit instance of `F[(a, b) =>> returnType]` if one exists in scope. */
    def summonBi[F[_[_, _]]](a: Symbol, b: Symbol, returnType: Type)(implicit tag: TypeTag[F[Any]]): Tree =
      summon[F[Any]](polyType(a :: b :: Nil, returnType))
  }

  case class MethodDef(name: String, rhs: Type => Option[Tree])
  object MethodDef {
    def apply(name: String)(rhs: PartialFunction[Type, Tree]): MethodDef = apply(name, rhs.lift)
  }

  final class ParamExtractor(symbol: Symbol) {
    def apply(tpe: Type): Type = appliedType(symbol, tpe)
    def unapply(tpe: Type): Option[Type] = if (tpe.typeSymbol == symbol) Some(tpe.typeArgs.head) else None
  }

  /** Constructor / extractor for repeated parameter (aka. vararg) types. */
  val RepeatedParam = new ParamExtractor(definitions.RepeatedParamClass)

  /** Constructor / extractor for by-name parameter types. */
  val ByNameParam = new ParamExtractor(definitions.ByNameParamClass)

  /** Return the dealiased and eta-expanded type constructor of this tag's type. */
  def typeConstructorOf(tag: WeakTypeTag[?]): Type =
    tag.tpe.typeConstructor.dealias.etaExpand

  /** Return the set of overridable members of `tpe`, excluding some undesired cases. */
  // TODO: Figure out what to do about different visibility modifiers.
  def overridableMembersOf(tpe: Type): Iterable[Symbol] = {
    import definitions.*
    val exclude = Set[Symbol](AnyClass, AnyRefClass, AnyValClass, ObjectClass)
    tpe.members.filterNot(m =>
      m.isConstructor || m.isFinal || m.isImplementationArtifact || m.isSynthetic || exclude(m.owner)
    )
  }

  /** Temporarily refresh type parameter names, type-check the `tree` and restore the original names.
    *
    * The purpose is to avoid warnings about type parameter shadowing, which can be problematic when `-Xfatal-warnings`
    * is enabled. We know the warnings are harmless because we deal with types directly. Unfortunately
    * `c.typecheck(tree, silent = true)` does not suppress warnings.
    */
  def typeCheckWithFreshTypeParams(tree: Tree): Tree = {
    val typeParams = tree.collect { case method: DefDef =>
      method.tparams.map(_.symbol)
    }.flatten

    val originalNames = for (tp <- typeParams) yield {
      val original = tp.name.toTypeName
      setName(tp, c.freshName(TypeName(original.toString)))
      original
    }

    val typed = c.typecheck(tree)
    for ((tp, original) <- typeParams.zip(originalNames)) setName(tp, original)
    typed
  }

  /** Abort with a message at the current compiler position. */
  def abort(message: String): Nothing = c.abort(c.enclosingPosition, message)

  /** `tpe.contains` is broken before Scala 2.13. See scala/scala#6122. */
  def occursIn(tpe: Type)(symbol: Symbol): Boolean = tpe.exists(_.typeSymbol == symbol)

  /** Does the given `symbol` have a specified `flag` as modifier? */
  def hasFlag(symbol: Symbol)(flag: FlagSet): Boolean = {
    val flagSet = flags(symbol)
    (flagSet | flag) == flagSet
  }

  private def typeParamsOf(signature: Type) =
    for (t <- signature.typeParams) yield typeDef(t)

  private def typeArgsFrom(signature: Type) =
    for (t <- signature.typeParams) yield typeRef(NoPrefix, t, Nil)

  /** Delegate the definition of type members and aliases in `algebra`. */
  def delegateTypes(algebra: Type, members: Iterable[Symbol])(rhs: (TypeSymbol, List[Type]) => Type): Iterable[Tree] =
    for (member <- members if member.isType) yield {
      val tpe = member.asType
      val signature = tpe.typeSignatureIn(algebra)
      q"type ${tpe.name}[..${typeParamsOf(signature)}] = ${rhs(tpe, typeArgsFrom(signature))}"
    }

  /** Delegate the definition of abstract type members and aliases in `algebra` to an existing `instance`. */
  def delegateAbstractTypes(algebra: Type, members: Iterable[Symbol], instance: Type): Iterable[Tree] =
    delegateTypes(algebra, members.filter(_.isAbstract))(typeRef(instance, _, _))

  /** Delegate the definition of methods in `algebra` to an existing `instance`. */
  def delegateMethods(algebra: Type, members: Iterable[Symbol], instance: Symbol)(
      transform: Transform[Method]
  ): Iterable[Tree] = for (member <- members if member.isMethod && !member.asMethod.isAccessor) yield {
    val name = member.name.toTermName
    val signature = member.typeSignatureIn(algebra)
    val paramLists = for (ps <- signature.paramLists) yield for (p <- ps) yield {
      // Only preserve the by-name and implicit modifiers (e.g. drop the default parameter flag).
      val flags = List(Flag.BYNAMEPARAM, Flag.IMPLICIT).filter(hasFlag(p))
      val modifiers = Modifiers(flags.foldLeft(Flag.PARAM)(_ | _))
      ValDef(modifiers, p.name.toTermName, TypeTree(p.typeSignatureIn(algebra)), EmptyTree)
    }

    val argLists = for (ps <- signature.paramLists) yield for (p <- ps) yield p.typeSignatureIn(algebra) match {
      case RepeatedParam(_) => q"${p.name.toTermName}: _*"
      case _ => Ident(p.name)
    }

    val body = q"$instance.$name[..${typeArgsFrom(signature)}](...$argLists)"
    val reified = Method(name, signature, typeParamsOf(signature), paramLists, signature.finalResultType, body)
    transform.applyOrElse(reified, identity[Method]).definition
  }

  /** Type-check a definition of type `instance` with stubbed methods to gain more type information. */
  def declare(instance: Type): Tree = {
    val members = overridableMembersOf(instance).filter(_.isAbstract)
    val stubs = delegateMethods(instance, members, NoSymbol) { case m => m.copy(body = q"_root_.scala.Predef.???") }
    val Block(List(declaration), _) = typeCheckWithFreshTypeParams(q"new $instance { ..$stubs }"): @unchecked
    declaration
  }

  /** Implement a possibly refined `algebra` with the provided `members`. */
  def implement(algebra: Type)(typeArgs: Symbol*)(members: Iterable[Tree]): Tree = {
    // If `members.isEmpty` we need an extra statement to ensure the generation of an anonymous class.
    val nonEmptyMembers = if (members.isEmpty) q"()" :: Nil else members
    val applied = appliedType(algebra, typeArgs.toList.map(_.asType.toTypeConstructor))
    applied match {
      case RefinedType(parents, scope) =>
        val refinements = delegateTypes(applied, scope.filterNot(_.isAbstract)) { (tpe, _) =>
          tpe.typeSignatureIn(applied).resultType
        }

        q"new ..$parents { ..$refinements; ..$nonEmptyMembers }"
      case _ =>
        q"new $applied { ..$nonEmptyMembers }"
    }
  }

  /** Create a new instance of `typeClass` for `algebra`. `rhs` should define a mapping for each method (by name) to an
    * implementation function based on type signature.
    */
  def instantiate[T: WeakTypeTag](tag: WeakTypeTag[?], typeArgs: Type*)(methods: (Type => MethodDef)*): Tree = {
    val algebra = typeConstructorOf(tag)
    val Ta = appliedType(symbolOf[T], algebra :: typeArgs.toList)
    val rhsMap = methods.iterator.map(_.apply(algebra)).flatMap(MethodDef.unapply).toMap
    val declaration @ ClassDef(_, _, _, Template(parents, self, members)) = declare(Ta): @unchecked
    val implementations = members.map {
      case member: DefDef =>
        val method = member.symbol.asMethod
        val impl = for {
          rhsOf <- rhsMap.get(method.name.toString)
          rhs <- rhsOf(method.typeSignatureIn(Ta))
        } yield defDef(method, rhs)
        impl.getOrElse(member)
      case member => member
    }

    val definition = classDef(declaration.symbol, Template(parents, self, implementations))
    typeCheckWithFreshTypeParams(q"{ $definition; new ${declaration.symbol} }")
  }

  // def map[A, B](fa: F[A])(f: A => B): F[B]
  def map(algebra: Type): MethodDef = MethodDef("map") {
    case PolyType(List(a, b), MethodType(List(fa), MethodType(List(f), _))) =>
      val Fa = singleType(NoPrefix, fa)
      val members = overridableMembersOf(Fa)
      val types = delegateAbstractTypes(Fa, members, Fa)
      val methods = delegateMethods(Fa, members, fa) {
        case method if method.occursInSignature(a) =>
          method.transformSubst(fa, a -> b) {
            case Parameter(pn, pt, _) if occursIn(pt)(a) =>
              val F = method.summonF[Contravariant](a, pt)
              q"$F.contramap[$b, $a]($pn)($f)"
          } {
            case delegate if method.occursInReturn(a) =>
              val F = method.summonF[Functor](a, method.returnType)
              q"$F.map[$a, $b]($delegate)($f)"
          }
      }

      implement(algebra)(b)(types ++ methods)
  }

  // def mapK[F[_], G[_]](af: A[F])(fk: F ~> G): A[G]
  def mapK(algebra: Type): MethodDef = MethodDef("mapK") {
    case PolyType(List(f, g), MethodType(List(af), MethodType(List(fk), _))) =>
      val Af = singleType(NoPrefix, af)
      val members = overridableMembersOf(Af)
      val types = delegateAbstractTypes(Af, members, Af)
      val methods = delegateMethods(Af, members, af) {
        case method if method.occursInSignature(f) =>
          method.transformSubst(af, f -> g) {
            case Parameter(pn, pt, _) if occursIn(pt)(f) =>
              val F = method.summonK[ContravariantK](f, pt)
              q"$F.contramapK[$g, $f]($pn)($fk)"
          } {
            case delegate if method.occursInReturn(f) =>
              val F = method.summonK[FunctorK](f, method.returnType)
              q"$F.mapK[$f, $g]($delegate)($fk)"
          }
      }

      implement(algebra)(g)(types ++ methods)
  }

  // def contramap[A, B](fa: F[A])(f: B => A): F[B]
  def contramap(algebra: Type): MethodDef = MethodDef("contramap") {
    case PolyType(List(a, b), MethodType(List(fa), MethodType(List(f), _))) =>
      val Fa = singleType(NoPrefix, fa)
      val members = overridableMembersOf(Fa)
      val types = delegateAbstractTypes(Fa, members, Fa)
      val methods = delegateMethods(Fa, members, fa) {
        case method if method.occursInSignature(a) =>
          method.transformSubst(fa, a -> b) {
            case Parameter(pn, pt, _) if occursIn(pt)(a) =>
              val F = method.summonF[Functor](a, pt)
              q"$F.map[$b, $a]($pn)($f)"
          } {
            case delegate if method.occursInReturn(a) =>
              val F = method.summonF[Contravariant](a, method.returnType)
              q"$F.contramap[$a, $b]($delegate)($f)"
          }
      }

      implement(algebra)(b)(types ++ methods)
  }

  // def contramapK[F, G](af: A[F])(fk: G => F): A[G]
  def contramapK(algebra: Type): MethodDef = MethodDef("contramapK") {
    case PolyType(List(f, g), MethodType(List(af), MethodType(List(fk), _))) =>
      val Af = singleType(NoPrefix, af)
      val members = overridableMembersOf(Af)
      val types = delegateAbstractTypes(Af, members, Af)
      val methods = delegateMethods(Af, members, af) {
        case method if method.occursInSignature(f) =>
          method.transformSubst(af, f -> g) {
            case Parameter(pn, pt, _) if occursIn(pt)(f) =>
              val F = method.summonK[FunctorK](f, pt)
              q"$F.mapK[$g, $f]($pn)($fk)"
          } {
            case delegate if method.occursInReturn(f) =>
              val F = method.summonK[ContravariantK](f, method.returnType)
              q"$F.contramapK[$f, $g]($delegate)($fk)"
          }
      }

      implement(algebra)(g)(types ++ methods)
  }

  // def imap[A, B](fa: F[A])(f: A => B)(g: B => A): F[B]
  def imap(algebra: Type): MethodDef = MethodDef("imap") {
    case PolyType(List(a, b), MethodType(List(fa), MethodType(List(f), MethodType(List(g), _)))) =>
      val Fa = singleType(NoPrefix, fa)
      val members = overridableMembersOf(Fa)
      val types = delegateAbstractTypes(Fa, members, Fa)
      val methods = delegateMethods(Fa, members, fa) {
        case method if method.occursInSignature(a) =>
          method.transformSubst(fa, a -> b) {
            case Parameter(pn, pt, _) if occursIn(pt)(a) =>
              val F = method.summonF[Invariant](a, pt)
              q"$F.imap[$b, $a]($pn)($g)($f)"
          } {
            case delegate if method.occursInReturn(a) =>
              val F = method.summonF[Invariant](a, method.returnType)
              q"$F.imap[$a, $b]($delegate)($f)($g)"
          }
      }

      implement(algebra)(b)(types ++ methods)
  }

  // def imapK[F[_], G[_]](af: A[F])(fk: F ~> G)(gK: G ~> F): A[G]
  def imapK(algebra: Type): MethodDef = MethodDef("imapK") {
    case PolyType(List(f, g), MethodType(List(af), MethodType(List(fk), MethodType(List(gk), _)))) =>
      val Af = singleType(NoPrefix, af)
      val members = overridableMembersOf(Af)
      val types = delegateAbstractTypes(Af, members, Af)
      val methods = delegateMethods(Af, members, af) {
        case method if method.occursInSignature(f) =>
          method.transformSubst(af, f -> g) {
            case Parameter(pn, pt, _) if occursIn(pt)(f) =>
              val F = method.summonK[InvariantK](f, pt)
              q"$F.imapK[$g, $f]($pn)($gk)($fk)"
          } {
            case delegate if method.occursInReturn(f) =>
              val F = method.summonK[InvariantK](f, method.returnType)
              q"$F.imapK[$f, $g]($delegate)($fk)($gk)"
          }
      }

      implement(algebra)(g)(types ++ methods)
  }

  // def ap[A, B](ff: F[A => B])(fa: F[A]): F[B]
  def ap(algebra: Type): MethodDef = MethodDef("ap") {
    case PolyType(List(a, b), MethodType(List(ff), MethodType(List(fa), _))) =>
      val A = a.asType.toType
      val B = b.asType.toType
      val Fa = singleType(NoPrefix, fa)
      val members = overridableMembersOf(Fa)
      val types = delegateAbstractTypes(Fa, members, Fa)
      val methods = delegateMethods(Fa, members, fa) {
        case method if method.occursOnlyInReturn(a) =>
          val returnType = method.returnType.map(t => if (t.typeSymbol == a) B else t)
          val Ap = method.summonF[cats.Apply](a, method.returnType)
          val body = q"$Ap.ap[$A, $B](${method.delegate(Ident(ff))})(${method.body})"
          method.copy(returnType = returnType, body = body)
        case method if method.occursInSignature(a) =>
          abort(s"Type parameter $A occurs in contravariant position in method ${method.displayName}")
      }

      implement(algebra)(b)(types ++ methods)
  }

  // def product[A, B](fa F[A], fb: F[B]): F[(A, B)]
  def product(algebra: Type): MethodDef = MethodDef("product") {
    case PolyType(List(a, b), MethodType(List(fa, fb), _)) =>
      val A = a.asType.toType
      val B = b.asType.toType
      val P = appliedType(symbolOf[(Any, Any)], A, B)
      val Fa = singleType(NoPrefix, fa)
      val members = overridableMembersOf(Fa)
      val types = delegateAbstractTypes(Fa, members, Fa)
      val tuple: Transform[Type] = {
        case tpe if occursIn(tpe)(a) =>
          tpe.map(t => if (t.typeSymbol == a) P else t)
      }

      val methods = delegateMethods(Fa, members, fa) {
        case method if method.occursInSignature(a) =>
          def map(f: Tree, t: Type): TransformParam = {
            case Parameter(pn, pt, _) if occursIn(pt)(a) =>
              val F = method.summonF[Functor](a, pt)
              q"$F.map[$P, $t]($pn)($f)"
          }

          val ma = method.transform(q"$fa")(tuple)(map(q"_._1", A))()
          if (method.occursInReturn(a)) {
            val mb = method.transform(q"$fb")(tuple)(map(q"_._2", B))()
            val S = method.summonF[Semigroupal](a, method.returnType)
            ma.copy(body = q"$S.product[$A, $B](${ma.body}, ${mb.body})")
          } else ma
      }

      implement(appliedType(algebra, P))()(types ++ methods)
  }

  // def productK[F[_], G[_]](af: A[F], ag: A[G]): A[Tuple2K[F, G, *]]
  def productK(algebra: Type): MethodDef = MethodDef("productK") {
    case PolyType(List(f, g), MethodType(List(af, ag), _)) =>
      val Tuple2K = symbolOf[Tuple2K[Any, Any, Any]]
      val SemiK = typeOf[SemigroupalK.type].termSymbol
      val F = f.asType.toTypeConstructor
      val G = g.asType.toTypeConstructor
      val t2k = polyType(F.typeParams, appliedType(Tuple2K, F :: G :: F.typeParams.map(_.asType.toType)))
      val Af = singleType(NoPrefix, af)
      val members = overridableMembersOf(Af)
      val types = delegateAbstractTypes(Af, members, Af)
      val tuple: Transform[Type] = {
        case tpe if occursIn(tpe)(f) =>
          tpe.map(t => if (t.typeSymbol == f) appliedType(t2k, t.typeArgs) else t)
      }

      val firstK = q"$SemiK.firstK[$F, $G]"
      val secondK = q"$SemiK.secondK[$F, $G]"
      val methods = delegateMethods(Af, members, af) {
        case method if method.occursInSignature(f) =>
          def mapK(fk: Tree): TransformParam = {
            case Parameter(pn, pt, _) if occursIn(pt)(f) =>
              val Fk = method.summonK[FunctorK](f, pt)
              q"$Fk.mapK($pn)($fk)"
          }

          val mf = method.transform(q"$af")(tuple)(mapK(firstK))()
          if (method.occursInReturn(f)) {
            val mg = method.transform(q"$ag")(tuple)(mapK(secondK))()
            val Sk = method.summonK[SemigroupalK](f, method.returnType)
            mf.copy(body = q"$Sk.productK[$F, $G](${mf.body}, ${mg.body})")
          } else mf
      }

      val typeParams = Tuple2K.typeParams.drop(2)
      val typeArgs = F :: G :: typeParams.map(_.asType.toType)
      val Tuple2kAlg = appliedType(algebra, polyType(typeParams, appliedType(Tuple2K, typeArgs)))
      implement(Tuple2kAlg)()(types ++ methods)
  }

  // def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
  def flatMap_(algebra: Type): MethodDef = MethodDef("flatMap") {
    case PolyType(List(a, b), MethodType(List(fa), MethodType(List(f), _))) =>
      val Fa = singleType(NoPrefix, fa)
      val members = overridableMembersOf(Fa)
      val types = delegateAbstractTypes(Fa, members, Fa)
      val methods = delegateMethods(Fa, members, fa) {
        case method if method.returnType.typeSymbol == a =>
          val body = method.delegate(q"$f(${method.body})")
          method.copy(returnType = b.asType.toType, body = body)
        case method if method.occursInSignature(a) =>
          val A = a.asType.toType
          abort(s"Type parameter $A can only occur as a top level return type in method ${method.displayName}")
      }

      implement(algebra)(b)(types ++ methods)
  }

  // def tailRecM[A, B](x: A)(f: A => F[Either[A, B]]): F[B]
  def tailRecM(algebra: Type): MethodDef = MethodDef("tailRecM") {
    case PolyType(List(a, b), MethodType(List(x), MethodType(List(f), _))) =>
      val Fa = appliedType(algebra, a.asType.toType)
      val methods = delegateMethods(Fa, overridableMembersOf(Fa), NoSymbol) {
        case method if method.returnType.typeSymbol == a =>
          val step = c.freshName(TermName("step"))
          val current = c.freshName(TermName("current"))
          val body = q"""{
            @_root_.scala.annotation.tailrec def $step($current: $a): $b =
              ${method.delegate(q"$f($current)")} match {
                case _root_.scala.Left(next) => $step(next)
                case _root_.scala.Right(target) => target
              }

            $step($x)
          }"""

          method.copy(returnType = b.asType.toType, body = body)
        case method if method.occursInSignature(a) =>
          val A = a.asType.toType
          abort(s"Type parameter $A can only occur as a top level return type in method ${method.displayName}")
        case method =>
          method.copy(body = method.delegate(q"$f($x)"))
      }

      implement(algebra)(b)(methods)
  }

  // def dimap[A, B, C, D](fab: F[A, B])(f: C => A)(g: B => D): F[C, D]
  def dimap(algebra: Type): MethodDef = MethodDef("dimap") {
    case PolyType(List(a, b, c, d), MethodType(List(fab), MethodType(List(f), MethodType(List(g), _)))) =>
      val Fab = singleType(NoPrefix, fab)
      val members = overridableMembersOf(Fab)
      val types = delegateAbstractTypes(Fab, members, Fab)
      val methods = delegateMethods(Fab, members, fab) {
        case method if method.occursInSignature(a) || method.occursInSignature(b) =>
          method.transformSubst(fab, a -> c, b -> d) {
            case Parameter(pn, pt, _) if occursIn(pt)(a) && occursIn(pt)(b) =>
              val F = method.summonBi[Profunctor](b, a, pt)
              q"$F.dimap[$d, $c, $b, $a]($pn)($g)($f)"
            case Parameter(pn, pt, _) if occursIn(pt)(a) =>
              val F = method.summonF[Functor](a, pt)
              q"$F.map[$c, $a]($pn)($f)"
            case Parameter(pn, pt, _) if occursIn(pt)(b) =>
              val F = method.summonF[Contravariant](b, pt)
              q"$F.contramap[$d, $b]($pn)($g)"
          } {
            case delegate if method.occursInReturn(a) && method.occursInReturn(b) =>
              val F = method.summonBi[Profunctor](a, b, method.returnType)
              q"$F.dimap[$a, $b, $c, $d]($delegate)($f)($g)"
            case delegate if method.occursInReturn(a) =>
              val F = method.summonF[Contravariant](a, method.returnType)
              q"$F.contramap[$a, $c]($delegate)($f)"
            case delegate if method.occursInReturn(b) =>
              val F = method.summonF[Functor](b, method.returnType)
              q"$F.map[$b, $d]($delegate)($g)"
          }
      }

      implement(algebra)(c, d)(types ++ methods)
  }

  // def bimap[A, B, C, D](fab: F[A, B])(f: A => C, g: B => D): F[C, D]
  def bimap(algebra: Type): MethodDef = MethodDef("bimap") {
    case PolyType(List(a, b, c, d), MethodType(List(fab), MethodType(List(f, g), _))) =>
      val Fab = singleType(NoPrefix, fab)
      val members = overridableMembersOf(Fab)
      val types = delegateAbstractTypes(Fab, members, Fab)
      val methods = delegateMethods(Fab, members, fab) {
        case method if method.occursInSignature(a) || method.occursInSignature(b) =>
          method.transformSubst(fab, a -> c, b -> d) {
            case Parameter(_, pt, _) if occursIn(pt)(a) && occursIn(pt)(b) =>
              val A = a.asType.toType
              val B = b.asType.toType
              abort(s"Both type parameters $A and $B occur in contravariant position in method ${method.displayName}")
            case Parameter(pn, pt, _) if occursIn(pt)(a) =>
              val F = method.summonF[Contravariant](a, pt)
              q"$F.contramap[$c, $a]($pn)($f)"
            case Parameter(pn, pt, _) if occursIn(pt)(b) =>
              val F = method.summonF[Contravariant](b, pt)
              q"$F.contramap[$d, $b]($pn)($g)"
          } {
            case delegate if method.occursInReturn(a) && method.occursInReturn(b) =>
              val F = method.summonBi[Bifunctor](a, b, method.returnType)
              q"$F.bimap[$a, $b, $c, $d]($delegate)($f, $g)"
            case delegate if method.occursInReturn(a) =>
              val F = method.summonF[Functor](a, method.returnType)
              q"$F.map[$a, $c]($delegate)($f)"
            case delegate if method.occursInReturn(b) =>
              val F = method.summonF[Functor](b, method.returnType)
              q"$F.map[$b, $d]($delegate)($g)"
          }
      }

      implement(algebra)(c, d)(types ++ methods)
  }

  // def instrument[F[_]](af: Alg[F]): Alg[Instrumentation[F, *]]
  def instrumentation(algebra: Type): MethodDef = MethodDef("instrument") {
    case PolyType(List(f), MethodType(List(af), _)) =>
      val Instrumentation = symbolOf[Instrumentation[Any, Any]]
      val F = f.asType.toTypeConstructor
      val Af = singleType(NoPrefix, af)
      val members = overridableMembersOf(Af)
      val types = delegateAbstractTypes(Af, members, Af)
      val algebraName = algebra.typeSymbol.name.decodedName.toString

      val methods = delegateMethods(Af, members, af) {
        case method if method.returnType.typeSymbol == f =>
          val body = q"${reify(aop.Instrumentation)}(${method.body}, $algebraName, ${method.displayName})"
          val returnType = appliedType(Instrumentation, F :: method.returnType.typeArgs)
          method.copy(body = body, returnType = returnType)
        case method if method.occursInSignature(f) =>
          abort(s"Type parameter $F can only occur as a top level return type in method ${method.displayName}")
      }

      val InstrumentationType = appliedType(Instrumentation, F :: F.typeParams.map(_.asType.toType))
      val InstrumentedAlg = appliedType(algebra, polyType(F.typeParams, InstrumentationType))
      implement(InstrumentedAlg)()(types ++ methods)
  }

  // def weave[F[_]](af: Alg[F]): Alg[Aspect.Weave[F, Dom, Cod, *]]
  def weave(Dom: Type, Cod: Type)(algebra: Type): MethodDef = MethodDef("weave") {
    case PolyType(List(f), MethodType(List(af), _)) =>
      val AspectWeave = symbolOf[Aspect.Weave[Any, Any, Any, Any]]
      val F = f.asType.toTypeConstructor
      val Af = singleType(NoPrefix, af)
      val members = overridableMembersOf(Af)
      val types = delegateAbstractTypes(Af, members, Af)
      val algebraName = algebra.typeSymbol.name.decodedName.toString

      val methods = delegateMethods(Af, members, af) {
        case method if method.returnType.typeSymbol == f =>
          val AspectAdvice = reify(Aspect.Advice)
          val arguments = method.transformedArgLists { case param @ Parameter(pn, pt, pm) =>
            val constructor = TermName(if (pm.hasFlag(Flag.BYNAMEPARAM)) "byName" else "byValue")
            q"$AspectAdvice.$constructor[$Dom, $pt](${param.displayName}, $pn)"
          }

          val hasImplicits = method.signature.paramLists.lastOption.flatMap(_.headOption).exists(_.isImplicit)
          val domain = if (hasImplicits) arguments.dropRight(1) else arguments
          val typeArgs = method.returnType.typeArgs
          val codomain = q"$AspectAdvice[$F, $Cod, ..$typeArgs](${method.displayName}, ${method.body})"
          val body = q"${reify(Aspect.Weave)}[$F, $Dom, $Cod, ..$typeArgs]($algebraName, $domain, $codomain)"
          val returnType = appliedType(AspectWeave, F :: Dom :: Cod :: typeArgs)
          method.copy(body = body, returnType = returnType)
        case method if method.occursInSignature(f) =>
          abort(s"Type parameter $F can only occur as a top level return type in method ${method.displayName}")
      }

      val WeaveType = appliedType(AspectWeave, F :: Dom :: Cod :: F.typeParams.map(_.asType.toType))
      val WeavedAlg = appliedType(algebra, polyType(F.typeParams, WeaveType))
      implement(WeavedAlg)()(types ++ methods)
  }

  def const[Alg[_[_]], A: WeakTypeTag](value: Tree)(implicit tag: WeakTypeTag[Alg[Any]]): Tree = {
    val algebra = typeConstructorOf(tag)
    val f = algebra.typeParams.head
    val F = f.asType.toTypeConstructor
    val Af = appliedType(algebra, F)
    val tmp = c.freshName(TermName("value"))
    val abstractMembers = overridableMembersOf(Af).filter(_.isAbstract)
    val methods = delegateMethods(Af, abstractMembers, NoSymbol) {
      case method if method.returnType.typeSymbol == f =>
        method.copy(returnType = weakTypeOf[A], body = q"$tmp")
      case method if method.occursInSignature(f) =>
        abort(s"Type parameter $F can only occur as a top level return type in method ${method.displayName}")
      case method =>
        abort(s"Abstract method ${method.displayName} cannot be derived because it does not return in $F")
    }

    val Const = weakTypeOf[Const[A]].member(TypeName("λ")).typeSignature
    q"val $tmp = $value; ${implement(appliedType(algebra, Const))()(methods)}"
  }

  def void[Alg[_[_]]](implicit tag: WeakTypeTag[Alg[Any]]): Tree =
    const[Alg, Unit](q"()")

  def readerT[Alg[_[_]], F[_]](implicit tag: WeakTypeTag[Alg[Any]], fTag: WeakTypeTag[F[Any]]): Tree = {
    val algebra = typeConstructorOf(tag)
    val F = typeConstructorOf(fTag)
    val f = F.typeSymbol
    val Af = appliedType(algebra, F)
    val af = c.freshName(TermName("af"))
    val ReaderT = typeOf[ReaderT[Any, Any, Any]].typeConstructor
    val abstractMembers = overridableMembersOf(Af).filter(_.isAbstract)
    val methods = delegateMethods(Af, abstractMembers, NoSymbol) {
      case method if method.returnType.typeSymbol == f =>
        method.transform(q"$af") {
          case tpe if tpe.typeSymbol == f =>
            appliedType(ReaderT, F :: Af :: tpe.typeArgs)
        } {
          case Parameter(pn, pt, _) if pt.typeSymbol == f =>
            q"$pn.run($af)"
        } { case delegate =>
          val typeArgs = F :: Af :: method.returnType.typeArgs
          q"${reify(cats.data.ReaderT)}[..$typeArgs](($af: $Af) => $delegate)"
        }
      case method =>
        abort(s"Abstract method ${method.displayName} cannot be derived because it does not return in $F")
    }

    val b = ReaderT.typeParams.last.asType
    val typeArg = polyType(b :: Nil, appliedType(ReaderT, F, Af, b.toType))
    implement(appliedType(algebra, typeArg))()(methods)
  }

  // def combineK[A](x: F[A], y: F[A]): F[A]
  def combineK(algebra: Type): MethodDef = MethodDef("combineK") { case PolyType(List(a), MethodType(List(x, y), _)) =>
    val Fa = singleType(NoPrefix, x)
    val members = overridableMembersOf(Fa)
    val types = delegateAbstractTypes(Fa, members, Fa)
    val methods = delegateMethods(Fa, members, x) {
      case method if method.occursInSignature(a) =>
        method.transform(q"$x")()() {
          case delegate if method.occursInReturn(a) =>
            val my = method.transform(q"$y")()()()
            val Sk = method.summonF[SemigroupK](a, method.returnType)
            q"$Sk.combineK[$a]($delegate, ${my.body})"
        }
    }

    implement(algebra)(a)(types ++ methods)
  }

  def functor[F[_]](implicit tag: WeakTypeTag[F[Any]]): Tree =
    instantiate[Functor[F]](tag)(map)

  def contravariant[F[_]](implicit tag: WeakTypeTag[F[Any]]): Tree =
    instantiate[Contravariant[F]](tag)(contramap)

  def invariant[F[_]](implicit tag: WeakTypeTag[F[Any]]): Tree =
    instantiate[Invariant[F]](tag)(imap)

  def profunctor[F[_, _]](implicit tag: WeakTypeTag[F[Any, Any]]): Tree =
    instantiate[Profunctor[F]](tag)(dimap)

  def bifunctor[F[_, _]](implicit tag: WeakTypeTag[F[Any, Any]]): Tree =
    instantiate[Bifunctor[F]](tag)(bimap)

  def semigroupal[F[_]](implicit tag: WeakTypeTag[F[Any]]): Tree =
    instantiate[Semigroupal[F]](tag)(product)

  def apply[F[_]](implicit tag: WeakTypeTag[F[Any]]): Tree =
    instantiate[cats.Apply[F]](tag)(map, ap)

  def flatMap[F[_]](implicit tag: WeakTypeTag[F[Any]]): Tree =
    instantiate[FlatMap[F]](tag)(map, flatMap_, tailRecM)

  def functorK[Alg[_[_]]](implicit tag: WeakTypeTag[Alg[Any]]): Tree =
    instantiate[FunctorK[Alg]](tag)(mapK)

  def contravariantK[Alg[_[_]]](implicit tag: WeakTypeTag[Alg[Any]]): Tree =
    instantiate[ContravariantK[Alg]](tag)(contramapK)

  def invariantK[Alg[_[_]]](implicit tag: WeakTypeTag[Alg[Any]]): Tree =
    instantiate[InvariantK[Alg]](tag)(imapK)

  def semigroupalK[Alg[_[_]]](implicit tag: WeakTypeTag[Alg[Any]]): Tree =
    instantiate[SemigroupalK[Alg]](tag)(productK)

  def applyK[Alg[_[_]]](implicit tag: WeakTypeTag[Alg[Any]]): Tree =
    instantiate[ApplyK[Alg]](tag)(mapK, productK)

  def semigroupK[F[_]](implicit tag: WeakTypeTag[F[Any]]): Tree =
    instantiate[SemigroupK[F]](tag)(combineK)

  def instrument[Alg[_[_]]](implicit tag: WeakTypeTag[Alg[Any]]): Tree =
    instantiate[Instrument[Alg]](tag)(instrumentation, mapK)

  def aspect[Alg[_[_]], Dom[_], Cod[_]](implicit
      tag: WeakTypeTag[Alg[Any]],
      dom: WeakTypeTag[Dom[Any]],
      cod: WeakTypeTag[Cod[Any]]
  ): Tree = {
    val Dom = typeConstructorOf(dom)
    val Cod = typeConstructorOf(cod)
    instantiate[Aspect[Alg, Dom, Cod]](tag, Dom, Cod)(weave(Dom, Cod), mapK)
  }
}
