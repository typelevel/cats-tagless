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
import cats.data.Tuple2K
import cats.{Bifunctor, Contravariant, FlatMap, Functor, Invariant}

import scala.reflect.macros.blackbox

class DeriveMacros(val c: blackbox.Context) {
  import c.internal._
  import c.universe._

  /** A reified method definition with some useful methods for transforming it. */
  case class Method(
    name: TermName,
    signature: Type,
    typeParams: List[TypeDef],
    paramLists: List[List[ValDef]],
    returnType: Type,
    body: Tree
  ) {

    def occursInSignature(symbol: Symbol): Boolean = occursIn(signature)(symbol)
    def occursInReturn(symbol: Symbol): Boolean = occursIn(returnType)(symbol)

    /** Construct a new set of parameter lists after substituting some type symbols. */
    def transformedParamLists(from: List[Symbol], to: List[Symbol]): List[List[ValDef]] =
      for (ps <- paramLists) yield for (p <- ps)
        yield ValDef(p.mods, p.name, TypeTree(p.tpt.tpe.substituteSymbols(from, to)), p.rhs)

    /** Construct a new set of argument lists based on their name and type. */
    def transformedArgLists(f: PartialFunction[(TermName, Type), Tree]): List[List[Tree]] = {
      val id: ((TermName, Type)) => Tree = {
        case (pn, _) => Ident(pn)
      }

      val f_* : (TermName, Type) => Tree = {
        case (pn, RepeatedParam(pt)) =>
          q"${f.andThen(arg => q"for ($pn <- $pn) yield $arg").applyOrElse(pn -> pt, id)}: _*"
        case (pn, pt) =>
          f.applyOrElse(pn -> pt, id)
      }

      for (ps <- paramLists) yield for (p <- ps)
        yield f_*(p.name, p.tpt.tpe)
    }

    /** Transform this method into another by applying transformations to types, arguments and body. */
    def transform(instance: Symbol, types: (Symbol, Symbol)*)(
      argLists: PartialFunction[(TermName, Type), Tree]
    )(body: PartialFunction[Tree, Tree]): Method = {
      val (from, to) = types.toList.unzip
      copy(
        paramLists = transformedParamLists(from, to),
        returnType = returnType.substituteSymbols(from, to),
        body = body.applyOrElse(delegate(Ident(instance), transformedArgLists(argLists)), identity[Tree])
      )
    }

    /** Delegate this method to an existing instance, optionally providing different argument lists. */
    def delegate(to: Tree, argLists: List[List[Tree]] = transformedArgLists(PartialFunction.empty)): Tree = {
      val typeArgs = for (tp <- typeParams) yield typeRef(NoPrefix, tp.symbol, Nil)
      q"$to.$name[..$typeArgs](...$argLists)"
    }

    /** The definition of this method as a Scala tree. */
    def definition: Tree = q"override def $name[..$typeParams](...$paramLists): $returnType = $body"
  }

  /** Constructor / extractor for repeated parameter (aka. vararg) types. */
  object RepeatedParam {

    def apply(tpe: Type): Type =
      appliedType(definitions.RepeatedParamClass, tpe)

    def unapply(tpe: Type): Option[Type] =
      if (tpe.typeSymbol == definitions.RepeatedParamClass) Some(tpe.typeArgs.head) else None
  }

  /** Return the set of overridable members of `tpe`, excluding some undesired cases. */
  // TODO: Figure out what to do about different visibility modifiers.
  def overridableMembersOf(tpe: Type): Iterable[Symbol] = {
    import definitions._
    val exclude = Set[Symbol](AnyClass, AnyRefClass, AnyValClass, ObjectClass)
    tpe.members.filterNot(m => m.isConstructor || m.isFinal || m.isImplementationArtifact || m.isSynthetic || exclude(m.owner))
  }

  /** Temporarily refresh type parameter names, type-check the `tree` and restore the original names.
    *
    * The purpose is to avoid warnings about type parameter shadowing, which can be problematic when
    * `-Xfatal-warnings` is enabled. We know the warnings are harmless because we deal with types directly.
    * Unfortunately `c.typecheck(tree, silent = true)` does not suppress warnings.
    */
  def typeCheckWithFreshTypeParams(tree: Tree): Tree = {
    val typeParams = tree.collect {
      case method: DefDef => method.tparams.map(_.symbol)
    }.flatten

    val originalNames = for (tp <- typeParams) yield {
      val original = tp.name.toTypeName
      setName(tp, TypeName(c.freshName(original.toString)))
      original
    }

    val typed = c.typecheck(tree)
    for ((tp, original) <- typeParams zip originalNames) setName(tp, original)
    typed
  }

  /** Abort with a message at the current compiler position. */
  def abort(message: String): Nothing = c.abort(c.enclosingPosition, message)

  /** `tpe.contains` is broken before Scala 2.13. See scala/scala#6122. */
  def occursIn(tpe: Type)(symbol: Symbol): Boolean = tpe.exists(_.typeSymbol == symbol)

  /** Summon an implicit instance of `A`'s type constructor applied to `typeArgs` if one exists in scope. */
  def summon[A: TypeTag](typeArgs: Type*): Tree = {
    val tpe = appliedType(typeOf[A].typeConstructor, typeArgs: _*)
    c.inferImplicitValue(tpe).orElse(abort(s"could not find implicit value of type $tpe"))
  }

  /** Delegate the definition of type members and aliases in `algebra`. */
  def delegateTypes(algebra: Type, members: Iterable[Symbol])(rhs: (TypeSymbol, List[Type]) => Type): Iterable[Tree] =
    for (member <- members if member.isType) yield {
      val tpe = member.asType
      val signature = tpe.typeSignatureIn(algebra)
      val typeParams = for (t <- signature.typeParams) yield typeDef(t)
      val typeArgs = for (t <- signature.typeParams) yield typeRef(NoPrefix, t, Nil)
      q"type ${tpe.name}[..$typeParams] = ${rhs(tpe, typeArgs)}"
    }

  /** Delegate the definition of abstract type members and aliases in `algebra` to an existing `instance`. */
  def delegateAbstractTypes(algebra: Type, members: Iterable[Symbol], instance: Type): Iterable[Tree] =
    delegateTypes(algebra, members.filter(_.isAbstract))((tpe, typeArgs) => typeRef(instance, tpe, typeArgs))

  /** Delegate the definition of methods in `algebra` to an existing `instance`. */
  def delegateMethods(algebra: Type, members: Iterable[Symbol], instance: Symbol)(
    transform: PartialFunction[Method, Method]
  ): Iterable[Tree] = for (member <- members if member.isMethod && !member.asMethod.isAccessor) yield {
    val name = member.name.toTermName
    val signature = member.typeSignatureIn(algebra)
    val typeParams = for (tp <- signature.typeParams) yield typeDef(tp)
    val typeArgs = for (tp <- signature.typeParams) yield typeRef(NoPrefix, tp, Nil)
    val paramLists = for (ps <- signature.paramLists) yield for (p <- ps) yield {
      // Only preserve the implicit modifier (e.g. drop the default parameter flag).
      val modifiers = if (p.isImplicit) Modifiers(Flag.IMPLICIT) else Modifiers()
      ValDef(modifiers, p.name.toTermName, TypeTree(p.typeSignatureIn(algebra)), EmptyTree)
    }

    val argLists = for (ps <- signature.paramLists) yield
      for (p <- ps) yield p.typeSignatureIn(algebra) match {
        case RepeatedParam(_) => q"${p.name.toTermName}: _*"
        case _ => Ident(p.name)
      }

    val body = q"$instance.$name[..$typeArgs](...$argLists)"
    val reified = Method(name, signature, typeParams, paramLists, signature.finalResultType, body)
    transform.applyOrElse(reified, identity[Method]).definition
  }

  /** Type-check a definition of type `instance` with stubbed methods to gain more type information. */
  def declare(instance: Type): Tree = {
    val members = overridableMembersOf(instance).filter(_.isAbstract)
    val stubs = delegateMethods(instance, members, NoSymbol) { case m => m.copy(body = q"_root_.scala.Predef.???") }
    val Block(List(declaration), _) = typeCheckWithFreshTypeParams(q"new $instance { ..$stubs }")
    declaration
  }

  /** Implement a possibly refined `algebra` with the provided `members`. */
  def implement(algebra: Type)(typeArgs: Symbol*)(members: Iterable[Tree]): Tree = {
    // If `members.isEmpty` we need an extra statement to ensure the generation of an anonymous class.
    val nonEmptyMembers = if (members.isEmpty) q"()" :: Nil else members
    val applied = appliedType(algebra, typeArgs.toList.map(_.asType.toTypeConstructor))

    applied match {
      case RefinedType(parents, scope) =>
        val refinements = delegateTypes(applied, scope.filterNot(_.isAbstract)) {
          (tpe, _) => tpe.typeSignatureIn(applied).resultType
        }

        q"new ..$parents { ..$refinements; ..$nonEmptyMembers }"
      case _ =>
        q"new $applied { ..$nonEmptyMembers }"
    }
  }

  /** Create a new instance of `typeClass` for `algebra`.
    * `rhs` should define a mapping for each method (by name) to an implementation function based on type signature.
    */
  def instantiate[T: WeakTypeTag](tag: WeakTypeTag[_])(rhs: (Type => (String, Type => Tree))*): Tree = {
    val algebra = tag.tpe.typeConstructor.dealias.etaExpand
    val Ta = appliedType(symbolOf[T], algebra)
    val impl = rhs.map(_.apply(algebra)).toMap
    val declaration @ ClassDef(_, _, _, Template(parents, self, members)) = declare(Ta)
    val implementations = for (member <- members) yield member match {
      case dd: DefDef =>
        val method = member.symbol.asMethod
        impl.get(method.name.toString).fold(dd)(f => defDef(method, f(method.typeSignatureIn(Ta))))
      case other => other
    }

    val definition = classDef(declaration.symbol, Template(parents, self, implementations))
    typeCheckWithFreshTypeParams(q"{ $definition; new ${declaration.symbol} }")
  }

  // def map[A, B](fa: F[A])(f: A => B): F[B]
  def map(algebra: Type): (String, Type => Tree) =
    "map" -> { case PolyType(List(a, b), MethodType(List(fa), MethodType(List(f), _))) =>
      val Fa = singleType(NoPrefix, fa)
      val members = overridableMembersOf(Fa)
      val types = delegateAbstractTypes(Fa, members, Fa)
      val methods = delegateMethods(Fa, members, fa) {
        case method if method.occursInSignature(a) =>
          method.transform(fa, a -> b) {
            case (pn, pt) if occursIn(pt)(a) =>
              val F = summon[Contravariant[Any]](polyType(a :: Nil, pt))
              q"$F.contramap[$b, $a]($pn)($f)"
          } {
            case delegate if method.occursInReturn(a) =>
              val F = summon[Functor[Any]](polyType(a :: Nil, method.returnType))
              q"$F.map[$a, $b]($delegate)($f)"
          }
      }

      implement(algebra)(b)(types ++ methods)
    }

  // def mapK[F[_], G[_]](af: A[F])(fk: F ~> G): A[G]
  def mapK(algebra: Type): (String, Type => Tree) =
    "mapK" -> { case PolyType(List(f, g), MethodType(List(af), MethodType(List(fk), _))) =>
      val Af = singleType(NoPrefix, af)
      val members = overridableMembersOf(Af)
      val types = delegateAbstractTypes(Af, members, Af)
      val methods = delegateMethods(Af, members, af) {
        case method if method.occursInReturn(f) =>
          method.transform(af, f -> g)(PartialFunction.empty) { case delegate =>
            val F = summon[FunctorK[Any]](polyType(f :: Nil, method.returnType))
            q"$F.mapK[$f, $g]($delegate)($fk)"
          }
        case method if method.occursInSignature(f) =>
          abort(s"Type parameter $f appears in contravariant position in method ${method.name}")
      }

      implement(algebra)(g)(types ++ methods)
    }

  // def contramap[A, B](fa: F[A])(f: B => A): F[B]
  def contramap(algebra: Type): (String, Type => Tree) =
    "contramap" -> { case PolyType(List(a, b), MethodType(List(fa), MethodType(List(f), _))) =>
      val Fa = singleType(NoPrefix, fa)
      val members = overridableMembersOf(Fa)
      val types = delegateAbstractTypes(Fa, members, Fa)
      val methods = delegateMethods(Fa, members, fa) {
        case method if method.occursInSignature(a) =>
          method.transform(fa, a -> b) {
            case (pn, pt) if occursIn(pt)(a) =>
              val F = summon[Functor[Any]](polyType(a :: Nil, pt))
              q"$F.map[$b, $a]($pn)($f)"
          } {
            case delegate if method.occursInReturn(a) =>
              val F = summon[Contravariant[Any]](polyType(a :: Nil, method.returnType))
              q"$F.contramap[$a, $b]($delegate)($f)"
          }
      }

      implement(algebra)(b)(types ++ methods)
    }

  // def imap[A, B](fa: F[A])(f: A => B)(g: B => A): F[B]
  def imap(algebra: Type): (String, Type => Tree) =
    "imap" -> { case PolyType(List(a, b), MethodType(List(fa), MethodType(List(f), MethodType(List(g), _)))) =>
      val Fa = singleType(NoPrefix, fa)
      val members = overridableMembersOf(Fa)
      val types = delegateAbstractTypes(Fa, members, Fa)
      val methods = delegateMethods(Fa, members, fa) {
        case method if method.occursInSignature(a) =>
          method.transform(fa, a -> b) {
            case (pn, pt) if occursIn(pt)(a) =>
              val F = summon[Invariant[Any]](polyType(a :: Nil, pt))
              q"$F.imap[$b, $a]($pn)($g)($f)"
          } {
            case delegate if method.occursInReturn(a) =>
              val F = summon[Invariant[Any]](polyType(a :: Nil, method.returnType))
              q"$F.imap[$a, $b]($delegate)($f)($g)"
          }
      }

      implement(algebra)(b)(types ++ methods)
    }

  // def imapK[F[_], G[_]](af: A[F])(fk: F ~> G)(gK: G ~> F): A[G]
  def imapK(algebra: Type): (String, Type => Tree) =
    "imapK" -> { case PolyType(List(f, g), MethodType(List(af), MethodType(List(fk), MethodType(List(gk), _)))) =>
      val Af = singleType(NoPrefix, af)
      val members = overridableMembersOf(Af)
      val types = delegateAbstractTypes(Af, members, Af)
      val methods = delegateMethods(Af, members, af) {
        case method if method.occursInSignature(f) =>
          method.transform(af, f -> g) {
            case (pn, pt) if occursIn(pt)(f) =>
              val F = summon[InvariantK[Any]](polyType(f :: Nil, pt))
              q"$F.imapK[$g, $f]($pn)($gk)($fk)"
          } {
            case delegate if method.occursInReturn(f) =>
              val F = summon[InvariantK[Any]](polyType(f :: Nil, method.returnType))
              q"$F.imapK[$f, $g]($delegate)($fk)($gk)"
          }
      }

      implement(algebra)(g)(types ++ methods)
    }

  // def productK[F[_], G[_]](af: A[F], ag: A[G]): A[Tuple2K[F, G, ?]]
  def productK(algebra: Type): (String, Type => Tree) = {
    val Tuple2K = symbolOf[Tuple2K[Any, Any, Any]]
    "productK" -> { case PolyType(List(f, g), MethodType(List(af, ag), _)) =>
      val F = f.asType.toTypeConstructor
      val G = g.asType.toTypeConstructor
      val t2k = polyType(F.typeParams, appliedType(Tuple2K, F :: G :: F.typeParams.map(_.asType.toType)))
      val Af = singleType(NoPrefix, af)
      val members = overridableMembersOf(Af)
      val types = delegateAbstractTypes(Af, members, Af)
      val methods = delegateMethods(Af, members, af) {
        case method if method.occursInReturn(f) =>
          val returnType = method.returnType.map(t => if (t.typeSymbol == f) appliedType(t2k, t.typeArgs) else t)
          val Sk = summon[SemigroupalK[Any]](polyType(f :: Nil, method.returnType))
          val body = q"$Sk.productK[$F, $G](${method.delegate(q"$af")}, ${method.delegate(q"$ag")})"
          method.copy(returnType = returnType, body = body)
        case method if method.occursInSignature(f) =>
          abort(s"Type parameter $f appears in contravariant position in method ${method.name}")
      }

      val typeParams = Tuple2K.typeParams.drop(2)
      val typeArgs = F :: G :: typeParams.map(_.asType.toType)
      val Tuple2kAlg = appliedType(algebra, polyType(typeParams, appliedType(Tuple2K, typeArgs)))
      implement(Tuple2kAlg)()(types ++ methods)
    }
  }

  // def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
  def flatMap_(algebra: Type): (String, Type => Tree) =
    "flatMap" -> { case PolyType(List(a, b), MethodType(List(fa), MethodType(List(f), _))) =>
      val Fa = singleType(NoPrefix, fa)
      val members = overridableMembersOf(Fa)
      val types = delegateAbstractTypes(Fa, members, Fa)
      val methods = delegateMethods(Fa, members, fa) {
        case method if method.returnType.typeSymbol == a =>
          val body = method.delegate(q"$f(${method.body})")
          method.copy(returnType = b.asType.toType, body = body)
        case method if method.occursInSignature(a) =>
          abort(s"Type parameter $a can only appear as a top level return type in method ${method.name}")
      }

      implement(algebra)(b)(types ++ methods)
    }

  // def tailRecM[A, B](x: A)(f: A => F[Either[A, B]]): F[B]
  def tailRecM(algebra: Type): (String, Type => Tree) =
    "tailRecM" -> { case PolyType(List(a, b), MethodType(List(x), MethodType(List(f), _))) =>
      val Fa = appliedType(algebra, a.asType.toType)
      val methods = delegateMethods(Fa, overridableMembersOf(Fa), NoSymbol) {
        case method if method.returnType.typeSymbol == a =>
          val step = TermName(c.freshName("step"))
          val current = TermName(c.freshName("current"))
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
          abort(s"Type parameter $a can only appear as a top level return type in method ${method.name}")
        case method =>
          method.copy(body = method.delegate(q"$f($x)"))
      }

      implement(algebra)(b)(methods)
    }

  // def dimap[A, B, C, D](fab: F[A, B])(f: C => A)(g: B => D): F[C, D]
  def dimap(algebra: Type): (String, Type => Tree) =
    "dimap" -> { case PolyType(List(a, b, c, d), MethodType(List(fab), MethodType(List(f), MethodType(List(g), _)))) =>
      val Fab = singleType(NoPrefix, fab)
      val members = overridableMembersOf(Fab)
      val types = delegateAbstractTypes(Fab, members, Fab)
      val methods = delegateMethods(Fab, members, fab) {
        case method if method.occursInSignature(a) || method.occursInSignature(b) =>
          method.transform(fab, a -> c, b -> d) {
            case (pn, pt) if occursIn(pt)(a) && occursIn(pt)(b) =>
              val F = summon[Profunctor[Any]](polyType(b :: a :: Nil, pt))
              q"$F.dimap[$d, $c, $b, $a]($pn)($g)($f)"
            case (pn, pt) if occursIn(pt)(a) =>
              val F = summon[Functor[Any]](polyType(a :: Nil, pt))
              q"$F.map[$c, $a]($pn)($f)"
            case (pn, pt) if occursIn(pt)(b) =>
              val F = summon[Contravariant[Any]](polyType(b :: Nil, pt))
              q"$F.contramap[$d, $b]($pn)($g)"
          } {
            case delegate if method.occursInReturn(a) && method.occursInReturn(b) =>
              val F = summon[Profunctor[Any]](polyType(a :: b :: Nil, method.returnType))
              q"$F.dimap[$a, $b, $c, $d]($delegate)($f)($g)"
            case delegate if method.occursInReturn(a) =>
              val F = summon[Contravariant[Any]](polyType(a :: Nil, method.returnType))
              q"$F.contramap[$a, $c]($delegate)($f)"
            case delegate if method.occursInReturn(b) =>
              val F = summon[Functor[Any]](polyType(b :: Nil, method.returnType))
              q"$F.map[$b, $d]($delegate)($g)"
          }
      }

      implement(algebra)(c, d)(types ++ methods)
    }

  // def bimap[A, B, C, D](fab: F[A, B])(f: A => C, g: B => D): F[C, D]
  def bimap(algebra: Type): (String, Type => Tree) =
    "bimap" -> { case PolyType(List(a, b, c, d), MethodType(List(fab), MethodType(List(f, g), _))) =>
      val Fab = singleType(NoPrefix, fab)
      val members = overridableMembersOf(Fab)
      val types = delegateAbstractTypes(Fab, members, Fab)
      val methods = delegateMethods(Fab, members, fab) {
        case method if method.occursInSignature(a) || method.occursInSignature(b) =>
          method.transform(fab, a -> c, b -> d) {
            case (_, pt) if occursIn(pt)(a) && occursIn(pt)(b) =>
              abort(s"Both type parameters $a and $b appear in contravariant position in method ${method.name}")
            case (pn, pt) if occursIn(pt)(a) =>
              val F = summon[Contravariant[Any]](polyType(a :: Nil, pt))
              q"$F.contramap[$c, $a]($pn)($f)"
            case (pn, pt) if occursIn(pt)(b) =>
              val F = summon[Contravariant[Any]](polyType(b :: Nil, pt))
              q"$F.contramap[$d, $b]($pn)($g)"
          } {
            case delegate if method.occursInReturn(a) && method.occursInReturn(b) =>
              val F = summon[Bifunctor[Any]](polyType(a :: b :: Nil, method.returnType))
              q"$F.bimap[$a, $b, $c, $d]($delegate)($f, $g)"
            case delegate if method.occursInReturn(a) =>
              val F = summon[Functor[Any]](polyType(a :: Nil, method.returnType))
              q"$F.map[$a, $c]($delegate)($f)"
            case delegate if method.occursInReturn(b) =>
              val F = summon[Functor[Any]](polyType(b :: Nil, method.returnType))
              q"$F.map[$b, $d]($delegate)($g)"
          }
      }

      implement(algebra)(c, d)(types ++ methods)
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

  def flatMap[F[_]](implicit tag: WeakTypeTag[F[Any]]): Tree =
    instantiate[FlatMap[F]](tag)(map, flatMap_, tailRecM)

  def functorK[Alg[_[_]]](implicit tag: WeakTypeTag[Alg[Any]]): Tree =
    instantiate[FunctorK[Alg]](tag)(mapK)

  def invariantK[Alg[_[_]]](implicit tag: WeakTypeTag[Alg[Any]]): Tree =
    instantiate[InvariantK[Alg]](tag)(imapK)

  def semigroupalK[Alg[_[_]]](implicit tag: WeakTypeTag[Alg[Any]]): Tree =
    instantiate[SemigroupalK[Alg]](tag)(productK)

  def applyK[Alg[_[_]]](implicit tag: WeakTypeTag[Alg[Any]]): Tree =
    instantiate[ApplyK[Alg]](tag)(mapK, productK)
}
