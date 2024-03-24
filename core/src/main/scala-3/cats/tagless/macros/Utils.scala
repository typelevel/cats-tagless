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

import scala.annotation.experimental

import quoted.*
import compiletime.asMatchable

private object Utils:
  // Unfortunately there is no flag for default parameters.
  val defaultRegex = ".*\\$default\\$\\d+".r
  def make(using q: Quotes): Utils[q.type] = new Utils

private class Utils[Q <: Quotes](using val q: Q):
  import quotes.reflect.*

  val nonOverridableOwners =
    TypeRepr.of[(Object, Any, AnyRef, AnyVal)].typeArgs.map(_.typeSymbol).toSet

  val nonOverridableFlags =
    List(Flags.Final, Flags.Artifact, Flags.Synthetic, Flags.Mutable)

  def overrideFlags(member: Symbol)(keep: Flags*): Flags =
    keep.iterator.filter(member.flags.is).foldLeft(Flags.Override)(_ | _)

  def call(alg: Term, method: Symbol, argss: List[List[Tree]]): Term =
    argss.foldLeft[Term](Select(alg, method)): (term, args) =>
      val typeArgs = for case t: TypeTree <- args yield t
      val termArgs = for case t: Term <- args yield t
      if typeArgs.nonEmpty then TypeApply(term, typeArgs) else Apply(term, termArgs)

  // TODO: Include type members.
  def membersAsSeenFrom(tpe: TypeRepr)(cls: Symbol): List[Symbol] =
    for member <- overridableMembersOf(cls) yield
      val privateWithin = member.privateWithin.fold(Symbol.noSymbol)(_.typeSymbol)
      if member.flags.is(Flags.Method) then
        val flags = overrideFlags(member)(keep = Flags.ExtensionMethod, Flags.Infix)
        Symbol.newMethod(cls, member.name, tpe.memberType(member), flags, privateWithin)
      else
        val flags = overrideFlags(member)(keep = Flags.Lazy)
        Symbol.newVal(cls, member.name, tpe.memberType(member), flags, privateWithin)

  // TODO: Include type members.
  // TODO: Handle accessibility.
  def overridableMembersOf(cls: Symbol): List[Symbol] =
    (cls.methodMembers ++ cls.fieldMembers).filterNot: member =>
      member.isClassConstructor
        || nonOverridableFlags.exists(member.flags.is)
        || nonOverridableOwners.contains(member.owner)
        || Utils.defaultRegex.matches(member.name)

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
    refinedTypeFind("λ")

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
