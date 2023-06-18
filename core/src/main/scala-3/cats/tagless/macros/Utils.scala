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

package cats.tagless.macros

import cats.data.{Cokleisli, Tuple2K}
import cats.tagless.ApplyK

import quoted.*
import compiletime.asMatchable
import scala.annotation.experimental

object Utils:
  val classNameCokleisli = classOf[Cokleisli[?, ?, ?]].getName
  val classNameTuple2K = classOf[Tuple2K[?, ?, ?]].getName
  val classNameApplyK = classOf[ApplyK[?]].getName
  val λlit = "λ"

  def methodApply[Alg[_[_]]: Type, F[_]: Type](e: Expr[Alg[F]])(using Quotes)(
      method: quotes.reflect.Symbol,
      argss: List[List[quotes.reflect.Tree]]
  ): quotes.reflect.Term =
    import quotes.reflect.*

    argss.foldLeft[Term](Select(e.asTerm, method)): (term, args) =>
      val typeArgs = args.collect { case t: TypeTree => t }
      val termArgs = args.collect { case t: Term => t }
      if typeArgs.nonEmpty then TypeApply(term, typeArgs) else Apply(term, termArgs)

  def memberSymbolsAsSeen[Alg[_[_]]: Type, F[_]: Type](using
      Quotes
  ): quotes.reflect.Symbol => List[quotes.reflect.Symbol] =
    import quotes.reflect.*
    clz =>
      val algFApplied = TypeRepr.of[Alg[F]]
      val methods = definedMethodsInTypeSym(using quotes)(clz)
      methods.map { method =>
        val asSeenApplied = algFApplied.memberType(method)
        Symbol.newMethod(
          clz,
          method.name,
          asSeenApplied,
          flags =
            Flags.Method, // method.flags is Flags.Deferred | Flags.Method, we'd like to unset the Deferred flag here // method.flags. &~ Flags.Deferred?
          privateWithin = method.privateWithin.fold(Symbol.noSymbol)(_.typeSymbol)
        )
      }

  // https://github.com/lampepfl/dotty/issues/11685
  def definedMethodsInTypeSym(using Quotes): quotes.reflect.Symbol => List[quotes.reflect.Symbol] =
    import quotes.reflect.*
    (cls: quotes.reflect.Symbol) =>
      for
        member <- cls.methodMembers
        // is abstract method, not implemented
        if member.flags.is(Flags.Deferred)

        // TODO: is that public?
        // TODO? if member.privateWithin
        if !member.flags.is(Flags.Private)
        if !member.flags.is(Flags.Protected)
        if !member.flags.is(Flags.PrivateLocal)

        if !member.isClassConstructor
        if !member.flags.is(Flags.Synthetic)
      yield member

  // https://github.com/lampepfl/dotty/discussions/16305
  // IdK[Int] is encoded as
  // java.lang.Object {
  //   type λ >: [F >: scala.Nothing <: [_$3 >: scala.Nothing <: scala.Any] => scala.Any] => F[scala.Int] <: [F >: scala.Nothing <: [_$3 >: scala.Nothing <: scala.Any] => scala.Any] => F[scala.Int]
  // }
  // i.e. a Refinement type (Object + the type declaration)
  //
  // val applyKTypeRepr = TypeRepr.of[IdK].appliedTo(inner) match
  //   case repr: Refinement => TypeRepr.of[ApplyK].appliedTo(repr.info)
  //   case repr             => report.errorAndAbort(s"IdK has no proper Refinement type: ${repr}")
  //
  // val instanceK = Implicits.search(applyKTypeRepr) match
  //   case res: ImplicitSearchSuccess => res.tree
  //   case _                          => report.errorAndAbort(s"No ${applyKTypeRepr.show} implicit found.")
  //
  // https://github.com/lampepfl/dotty/blob/1130c52a6476d473b41a598e23f0415e0f8d76dc/tests/run-macros/refined-selectable-macro/Macro_1.scala#L22-L37
  def refinedTypes(using Quotes): quotes.reflect.TypeRepr => List[(String, quotes.reflect.TypeRepr)] =
    import quotes.reflect.*
    (tr: TypeRepr) =>
      tr.asMatchable match
        case Refinement(parent, name, info) => (name, info) :: refinedTypes(parent)
        case repr => Nil

  def refinedTypeFind(searchName: String)(using
      Quotes
  ): quotes.reflect.TypeRepr => Option[(String, quotes.reflect.TypeRepr)] =
    import quotes.reflect.*
    (tr: TypeRepr) =>
      tr.asMatchable match
        case Refinement(parent, name, info) if name == searchName => Some(name, info)
        case Refinement(parent, _, _) => refinedTypeFind(searchName)(parent)
        case repr => None

  def refinedTypeFindλ(using Quotes): quotes.reflect.TypeRepr => Option[(String, quotes.reflect.TypeRepr)] =
    refinedTypeFind(λlit)

  def summon(using Quotes): quotes.reflect.TypeRepr => quotes.reflect.Term =
    import quotes.reflect.*
    (typeRepr: TypeRepr) =>
      Implicits.search(typeRepr) match
        case res: ImplicitSearchSuccess => res.tree
        case _ => report.errorAndAbort(s"No ${typeRepr.show} implicit found.")

  def typeReprFor[Alg[_[_[_]]]: Type, U[_]: Type](using
      Quotes
  ): List[quotes.reflect.TypeRepr] => quotes.reflect.TypeRepr =
    import quotes.reflect.*
    (inner: List[TypeRepr]) =>
      val typeRepr = TypeRepr.of[U]
      typeRepr.appliedTo(inner) match
        case repr: Refinement => TypeRepr.of[Alg].appliedTo(repr.info)
        case repr => report.errorAndAbort(s"$typeRepr has no proper Refinement type: $repr")

  @experimental
  def newInstanceCall(using
      Quotes
  ): (quotes.reflect.Symbol, List[quotes.reflect.TypeRepr], List[quotes.reflect.Term]) => quotes.reflect.Apply =
    import quotes.reflect.*
    (cls, typeArgs, valArgs) =>
      New(Inferred(cls.typeRef))
        .select(cls.primaryConstructor)
        .appliedToTypes(typeArgs)
        .appliedToArgs(valArgs)
