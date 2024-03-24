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
import scala.quoted.*

private object DeriveMacros:
  // Unfortunately there is no flag for default parameters.
  private val defaultRegex = ".*\\$default\\$\\d+".r

@experimental
private class DeriveMacros[Q <: Quotes](using val q: Q):
  import quotes.reflect.*

  private val nonOverridableOwners =
    TypeRepr.of[(Object, Any, AnyRef, AnyVal)].typeArgs.map(_.typeSymbol).toSet

  private val nonOverridableFlags =
    List(Flags.Final, Flags.Artifact, Flags.Synthetic, Flags.Mutable, Flags.Param)

  extension (term: Term)
    def call(method: Symbol)(argss: List[List[Tree]]): Term =
      argss.foldLeft[Term](term.select(method)): (term, args) =>
        val typeArgs = for case arg: TypeTree <- args yield arg
        val termArgs = for case arg: Term <- args yield arg
        if typeArgs.isEmpty then term.appliedToArgs(termArgs)
        else term.appliedToTypeTrees(typeArgs)

    def transformTo[A: Type](
        args: PartialFunction[(TypeRepr, Term), Term] = PartialFunction.empty,
        body: PartialFunction[(TypeRepr, Term), Term] = PartialFunction.empty
    ): Expr[A] =
      val name = Symbol.freshName("$anon")
      val parents = List(TypeTree.of[Object], TypeTree.of[A])
      val cls = Symbol.newClass(Symbol.spliceOwner, name, parents.map(_.tpe), _.overridableMembers, None)

      def transformArg(param: ValDef | TypeDef, arg: Tree) = (param, arg) match
        case (param: ValDef, arg: Term) => args.applyOrElse((param.tpt.tpe, arg), _ => arg)
        case (_, arg) => arg

      def transformBody(method: DefDef)(argss: List[List[Tree]]) =
        val delegate = term.call(method.symbol):
          for (params, args) <- method.paramss.lazyZip(argss)
          yield for (param, arg) <- params.params.lazyZip(args)
          yield transformArg(param, arg)
        Some(body.applyOrElse((method.returnTpt.tpe, delegate), _ => delegate))

      val methods = cls.declaredMethods.flatMap: sym =>
        PartialFunction.condOpt(sym.tree):
          case method: DefDef => DefDef(sym, transformBody(method))

      val newCls = New(TypeIdent(cls)).select(cls.primaryConstructor).appliedToNone
      Block(ClassDef(cls, parents, methods) :: Nil, newCls).asExprOf[A]

  extension (sym: Symbol)
    def privateIn: Symbol =
      sym.privateWithin.fold(Symbol.noSymbol)(_.typeSymbol)

    def overrideKeeping(flags: Flags*): Flags =
      flags.iterator.filter(sym.flags.is).foldLeft(Flags.Override)(_ | _)

    // TODO: Include type members.
    // TODO: Handle accessibility.
    def overridableMembers: List[Symbol] = for
      cls <- This(sym).tpe :: Nil
      member <- sym.methodMembers.iterator ++ sym.fieldMembers
      if !member.isClassConstructor
      if !nonOverridableFlags.exists(member.flags.is)
      if !nonOverridableOwners.contains(member.owner)
      if !DeriveMacros.defaultRegex.matches(member.name)
    yield
      if member.isDefDef then
        val flags = member.overrideKeeping(Flags.ExtensionMethod, Flags.Infix)
        Symbol.newMethod(sym, member.name, cls.memberType(member), flags, sym.privateIn)
      else if member.isValDef then
        val flags = member.overrideKeeping(Flags.Lazy)
        Symbol.newVal(sym, member.name, cls.memberType(member), flags, sym.privateIn)
      else member

  extension (tpe: TypeRepr)
    def contains(sym: Symbol): Boolean =
      tpe != tpe.substituteTypes(sym :: Nil, TypeRepr.of[Any] :: Nil)

    def bounds: TypeBounds = tpe match
      case bounds: TypeBounds => bounds
      case tpe => TypeBounds(tpe, tpe)

    def summon: Term = Implicits.search(tpe) match
      case success: ImplicitSearchSuccess => success.tree
      case failure: ImplicitSearchFailure => report.errorAndAbort(failure.explanation)
      case _ => report.errorAndAbort(s"No given ${tpe.show} found")

    def lambda(args: List[Symbol]): TypeLambda =
      val n = args.length
      val names = args.map(_.name)
      def bounds(tl: TypeLambda) =
        val params = List.tabulate(n)(tl.param)
        args.map(_.info.substituteTypes(args, params).bounds)
      def result(tl: TypeLambda) =
        val params = List.tabulate(n)(tl.param)
        tpe.substituteTypes(args, params)
      TypeLambda(names, bounds, result)

    def summonLambda[T <: AnyKind: Type](arg: Symbol, args: Symbol*): Term =
      TypeRepr.of[T].appliedTo(tpe.lambda(arg :: args.toList)).summon
