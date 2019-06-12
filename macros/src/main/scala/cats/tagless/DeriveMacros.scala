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

import cats.arrow.Profunctor
import cats.data.Tuple2K
import cats.{Contravariant, FlatMap, Functor, Invariant}

import scala.reflect.macros.blackbox

class DeriveMacros(val c: blackbox.Context) {
  import c.internal._
  import c.universe._

  /** A reified method definition with some useful methods for transforming it. */
  case class Method(m: MethodSymbol, tps: List[TypeDef], pss: List[List[ValDef]], rt: Type, body: Tree) {

    /** Does any parameter have `symbol` as the outermost type (possibly higher-kinded)? */
    def paramsContainTopLevel(symbol: Symbol): Boolean =
      pss.exists(_.exists(_.tpt.tpe match {
        case RepeatedParam(tpe) => tpe.typeSymbol == symbol
        case tpe => tpe.typeSymbol == symbol
      }))

    /** Return the list of type parameters as type arguments as seen from the method body. */
    def typeArgs: List[Type] = for (tp <- tps) yield typeRef(NoPrefix, tp.symbol, Nil)

    /** Construct a new set of parameter lists after transforming the original types. */
    def paramLists(f: Type => Type): List[List[ValDef]] = {
      val g: Type => Type = {
        case RepeatedParam(tpe) => RepeatedParam(f(tpe))
        case tpe => f(tpe)
      }

      for (ps <- pss) yield for (p <- ps)
        yield ValDef(p.mods, p.name, TypeTree(g(p.tpt.tpe)), p.rhs)
    }

    /** Construct a new set of argument lists based on their name and type. */
    def argLists(f: (TermName, Type) => Tree): List[List[Tree]] = {
      val g: (TermName, Type) => Tree = {
        case (name, RepeatedParam(tpe)) =>
          val param = ValDef(Modifiers(Flag.PARAM), name, TypeTree(), EmptyTree)
          q"$name.map($param => ${f(name, tpe)}): _*"
        case (name, tpe) =>
          f(name, tpe)
      }

      for (ps <- pss) yield for (p <- ps) yield g(p.name, p.tpt.tpe)
    }

    /** The definition of this method as a Scala tree. */
    def definition: Tree = q"override def ${m.name}[..$tps](...$pss): $rt = $body"
  }

  /** Constructor / extractor for repeated parameter (aka. vararg) types. */
  object RepeatedParam {

    def apply(tpe: Type): Type =
      appliedType(definitions.RepeatedParamClass, tpe)

    def unapply(tpe: Type): Option[Type] =
      if (tpe.typeSymbol == definitions.RepeatedParamClass) Some(tpe.typeArgs.head) else None
  }

  /** Extract a normalized type constructor from the type tag, suitable to work with in macros. */
  def normalizedTypeConstructor(tag: WeakTypeTag[_]): Type =
    tag.tpe.typeConstructor.dealias.etaExpand

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
    val method = member.asMethod
    val signature = method.typeSignatureIn(algebra)
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

    val delegate = q"$instance.$method[..$typeArgs](...$argLists)"
    val reified = Method(method, typeParams, paramLists, signature.finalResultType, delegate)
    transform.applyOrElse(reified, identity[Method]).definition
  }

  /** Type-check a definition of type `instance` with stubbed methods to gain more type information. */
  def declare(instance: Type): Tree = {
    val stubs = delegateMethods(instance, overridableMembersOf(instance).filter(_.isAbstract), NoSymbol) {
      case method => method.copy(body = q"_root_.scala.Predef.???")
    }

    val Block(List(declaration), _) = typeCheckWithFreshTypeParams(q"new $instance { ..$stubs }")
    declaration
  }

  /** Implement a possibly refined `algebra` with the provided `members`. */
  def implement(algebra: Type, members: Iterable[Tree]): Tree = {
    // If `members.isEmpty` we need an extra statement to ensure the generation of an anonymous class.
    val nonEmptyMembers = if (members.isEmpty) q"()" :: Nil else members

    algebra match {
      case RefinedType(parents, scope) =>
        val refinements = delegateTypes(algebra, scope.filterNot(_.isAbstract)) {
          (tpe, _) => tpe.typeSignatureIn(algebra).resultType
        }

        q"new ..$parents { ..$refinements; ..$nonEmptyMembers }"
      case _ =>
        q"new $algebra { ..$nonEmptyMembers }"
    }
  }

  /** Create a new instance of `typeClass` for `algebra`.
    * `rhs` should define a mapping for each method (by name) to an implementation function based on type signature.
    */
  def instantiate(typeClass: TypeSymbol, algebra: Type)(rhs: (String, Type => Tree)*): Tree = {
    val impl = rhs.toMap
    val TcA = appliedType(typeClass, algebra)
    val declaration @ ClassDef(_, _, _, Template(parents, self, members)) = declare(TcA)
    val implementations = for (member <- members) yield member match {
      case dd: DefDef =>
        val method = member.symbol.asMethod
        impl.get(method.name.toString).fold(dd)(f => defDef(method, f(method.typeSignatureIn(TcA))))
      case other => other
    }

    val definition = classDef(declaration.symbol, Template(parents, self, implementations))
    typeCheckWithFreshTypeParams(q"{ $definition; new ${declaration.symbol} }")
  }

  // def map[A, B](fa: F[A])(f: A => B): F[B]
  // def mapK[F[_], G[_]](af: A[F])(fk: F ~> G): A[G]
  def mapK(algebra: Type): (String, Type => Tree) =
    "mapK" -> { case PolyType(List(f, g), MethodType(List(af), MethodType(List(fk), _))) =>
      val Af = singleType(NoPrefix, af)
      val members = overridableMembersOf(Af)
      val types = delegateAbstractTypes(Af, members, Af)
      val methods = delegateMethods(Af, members, af.asTerm) {
        case method @ Method(_, _, _, rt, delegate) if rt.typeSymbol == f =>
          method.copy(rt = appliedType(g, rt.typeArgs), body = q"$fk($delegate)")
      }

      implement(appliedType(algebra, g.asType.toTypeConstructor), types ++ methods)
    }

  // def contramap[A, B](fa: F[A])(f: B => A): F[B]
  def contramapK(algebra: Type): (String, Type => Tree) =
    "contramapK" -> { case PolyType(List(f, g), MethodType(List(af), MethodType(List(fk), _))) =>
      val Af = singleType(NoPrefix, af)
      val members = overridableMembersOf(Af)
      val types = delegateAbstractTypes(Af, members, Af)
      val methods = delegateMethods(Af, members, af.asTerm) {
        case method @ Method(m, _, _, _, _) if method.paramsContainTopLevel(f) =>
          val paramLists = method.paramLists(tpe => if (tpe.typeSymbol == f) appliedType(g, tpe.typeArgs) else tpe)
          val argLists = method.argLists((pn, pt) => if (pt.typeSymbol == f) q"$fk($pn)" else Ident(pn))
          val delegate = q"$af.$m[..${method.typeArgs}](...$argLists)"
          method.copy(pss = paramLists, body = delegate)
      }

      implement(appliedType(algebra, g.asType.toTypeConstructor), types ++ methods)
    }

  // def imap[A, B](fa: F[A])(f: A => B)(g: B => A): F[B]
  // def imapK[F[_], G[_]](af: A[F])(fk: F ~> G)(gK: G ~> F): A[G]
  def imapK(algebra: Type): (String, Type => Tree) =
    "imapK" -> { case PolyType(List(f, g), MethodType(List(af), MethodType(List(fk), MethodType(List(gk), _)))) =>
      val Af = singleType(NoPrefix, af)
      val members = overridableMembersOf(Af)
      val types = delegateAbstractTypes(Af, members, Af)
      val methods = delegateMethods(Af, members, af.asTerm) {
        case method @ Method(m, _, _, rt, _) if rt.typeSymbol == f || method.paramsContainTopLevel(f) =>
          val paramLists = method.paramLists(tpe => if (tpe.typeSymbol == f) appliedType(g, tpe.typeArgs) else tpe)
          val argLists = method.argLists((pn, pt) => if (pt.typeSymbol == f) q"$gk($pn)" else Ident(pn))
          val returnType = if (rt.typeSymbol == f) appliedType(g, rt.typeArgs) else rt
          val delegate = q"$af.$m[..${method.typeArgs}](...$argLists)"
          val body = if (rt.typeSymbol == f) q"$fk($delegate)" else delegate
          method.copy(pss = paramLists, rt = returnType, body = body)
      }

      implement(appliedType(algebra, g.asType.toTypeConstructor), types ++ methods)
    }

  // def productK[F[_], G[_]](af: A[F], ag: A[G]): A[Tuple2K[F, G, ?]]
  def productK(algebra: Type): (String, Type => Tree) = {
    val Tuple2K = symbolOf[Tuple2K[Any, Any, Any]]
    "productK" -> { case PolyType(List(f, g), MethodType(List(af, ag), _)) =>
      val F = f.asType.toTypeConstructor
      val G = g.asType.toTypeConstructor
      val Af = singleType(NoPrefix, af)
      val members = overridableMembersOf(Af)
      val types = delegateAbstractTypes(Af, members, Af)
      val methods = delegateMethods(Af, members, af.asTerm) {
        case method @ Method(m, _, _, rt, delegate) if rt.typeSymbol == f =>
          val argLists = method.argLists((pn, _) => Ident(pn))
          val FGA = F :: G :: rt.typeArgs
          val returnType = appliedType(Tuple2K, FGA)
          val body = q"new $Tuple2K[..$FGA]($delegate, $ag.$m[..${method.typeArgs}](...$argLists))"
          method.copy(rt = returnType, body = body)
      }

      val typeParams = Tuple2K.typeParams.drop(2)
      val typeArgs = F :: G :: typeParams.map(_.asType.toType)
      val T2kAlg = appliedType(algebra, polyType(typeParams, appliedType(Tuple2K, typeArgs)))
      implement(T2kAlg, types ++ methods)
    }
  }

  // def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
  def flatMapK(algebra: Type): (String, Type => Tree) =
    "flatMapK" -> { case PolyType(List(f, g), MethodType(List(af), MethodType(List(fk), _))) =>
      val Af = singleType(NoPrefix, af)
      val members = overridableMembersOf(Af)
      val types = delegateAbstractTypes(Af, members, Af)
      val methods = delegateMethods(Af, members, af.asTerm) {
        case method @ Method(m, _, _, rt, delegate) if rt.typeSymbol == f =>
          val argLists = method.argLists((pn, _) => Ident(pn))
          val body = q"$fk($delegate).$m[..${method.typeArgs}](...$argLists)"
          method.copy(rt = appliedType(g, rt.typeArgs), body = body)
      }

      implement(appliedType(algebra, g.asType.toTypeConstructor), types ++ methods)
    }

  // def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B]
  def tailRecM(algebra: Type): (String, Type => Tree) =
    "tailRecM" -> { case PolyType(List(a, b), MethodType(List(x), MethodType(List(f), _))) =>
      val Fa = appliedType(algebra, a.asType.toType)
      val methods = delegateMethods(Fa, overridableMembersOf(Fa), NoSymbol) {
        case method @ Method(m, _, _, rt, _) =>
          val argLists = method.argLists((pn, _) => Ident(pn))
          if (rt.typeSymbol == a) {
            val step = TermName(c.freshName("step"))
            val current = TermName(c.freshName("current"))
            val body = q"""{
              @_root_.scala.annotation.tailrec def $step($current: $a): $b =
                $f($current).$m[..${method.typeArgs}](...$argLists) match {
                  case _root_.scala.Left(next) => $step(next)
                  case _root_.scala.Right(target) => target
                }

              $step($x)
            }"""

            method.copy(rt = appliedType(b, rt.typeArgs), body = body)
          } else {
            val body = q"$f($x).$m[..${method.typeArgs}](...$argLists)"
            method.copy(body = body)
          }
      }

      implement(appliedType(algebra, b.asType.toTypeConstructor), methods)
    }

  // def dimap[A, B, C, D](fab: F[A, B])(f: C => A)(g: B => D): F[C, D]
  def dimap(algebra: Type): (String, Type => Tree) =
    "dimap" -> { case PolyType(List(a, b, c, d), MethodType(List(fab), MethodType(List(f), MethodType(List(g), _)))) =>
      val Fab = singleType(NoPrefix, fab)
      val members = overridableMembersOf(Fab)
      val types = delegateAbstractTypes(Fab, members, Fab)
      val methods = delegateMethods(Fab, members, fab.asTerm) {
        case method @ Method(m, _, _, rt, _) if rt.typeSymbol == b || method.paramsContainTopLevel(a) =>
          val paramLists = method.paramLists(tpe => if (tpe.typeSymbol == a) appliedType(c, tpe.typeArgs) else tpe)
          val argLists = method.argLists((pn, pt) => if (pt.typeSymbol == a) q"$f($pn)" else Ident(pn))
          val returnType = if (rt.typeSymbol == b) appliedType(d, rt.typeArgs) else rt
          val delegate = q"$fab.$m[..${method.typeArgs}](...$argLists)"
          val body = if (rt.typeSymbol == b) q"$g($delegate)" else delegate
          method.copy(pss = paramLists, rt = returnType, body = body)
      }

      val C = c.asType.toTypeConstructor
      val D = d.asType.toTypeConstructor
      implement(appliedType(algebra, C, D), types ++ methods)
    }

  def functor[F[_]](implicit tag: c.WeakTypeTag[F[Any]]): c.Tree = {
    val F = normalizedTypeConstructor(tag)
    instantiate(symbolOf[Functor[Any]], F)(mapK(F).copy(_1 = "map"))
  }

  def contravariant[F[_]](implicit tag: c.WeakTypeTag[F[Any]]): c.Tree = {
    val F = normalizedTypeConstructor(tag)
    instantiate(symbolOf[Contravariant[Any]], F)(contramapK(F).copy(_1 = "contramap"))
  }

  def invariant[F[_]](implicit tag: c.WeakTypeTag[F[Any]]): c.Tree = {
    val F = normalizedTypeConstructor(tag)
    instantiate(symbolOf[Invariant[Any]], F)(imapK(F).copy(_1 = "imap"))
  }

  def profunctor[F[_, _]](implicit tag: c.WeakTypeTag[F[Any, Any]]): c.Tree = {
    val F = normalizedTypeConstructor(tag)
    instantiate(symbolOf[Profunctor[F]], F)(dimap(F))
  }

  def flatMap[F[_]](implicit tag: c.WeakTypeTag[F[Any]]): c.Tree = {
    val F = normalizedTypeConstructor(tag)
    instantiate(symbolOf[FlatMap[Any]], F)(mapK(F).copy(_1 = "map"), flatMapK(F).copy(_1 = "flatMap"), tailRecM(F))
  }

  def functorK[Alg[_[_]]](implicit tag: c.WeakTypeTag[Alg[Any]]): c.Tree = {
    val Alg = normalizedTypeConstructor(tag)
    instantiate(symbolOf[FunctorK[Any]], Alg)(mapK(Alg))
  }

  def invariantK[Alg[_[_]]](implicit tag: c.WeakTypeTag[Alg[Any]]): c.Tree = {
    val Alg = normalizedTypeConstructor(tag)
    instantiate(symbolOf[InvariantK[Any]], Alg)(imapK(Alg))
  }

  def semigroupalK[Alg[_[_]]](implicit tag: c.WeakTypeTag[Alg[Any]]): c.Tree = {
    val Alg = normalizedTypeConstructor(tag)
    instantiate(symbolOf[SemigroupalK[Any]], Alg)(productK(Alg))
  }

  def applyK[Alg[_[_]]](implicit tag: c.WeakTypeTag[Alg[Any]]): c.Tree = {
    val Alg = normalizedTypeConstructor(tag)
    instantiate(symbolOf[ApplyK[Any]], Alg)(mapK(Alg), productK(Alg))
  }
}
