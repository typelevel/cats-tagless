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

  type Transform = PartialFunction[(TypeRepr, Term), Term]
  type Combine = PartialFunction[(TypeRepr, Seq[Term]), Term]

  private val nonOverridableOwners =
    TypeRepr.of[(Object, Any, AnyRef, AnyVal)].typeArgs.map(_.typeSymbol).toSet

  private val nonOverridableFlags =
    List(Flags.Final, Flags.Artifact, Flags.Synthetic, Flags.Mutable, Flags.Param)

  extension (xf: Transform)
    def transformRepeated(method: Symbol, tpe: TypeRepr, arg: Term): Tree =
      val x = Symbol.freshName("x")
      val resultType = xf(tpe, Select.unique(arg, "head")).tpe
      val lambdaType = MethodType(x :: Nil)(_ => tpe :: Nil, _ => resultType)
      val lambda = Lambda(method, lambdaType, (_, xs) => xf(tpe, xs.head.asExpr.asTerm))
      val result = Select.overloaded(arg, "map", resultType :: Nil, lambda :: Nil)
      val repeatedType = defn.RepeatedParamClass.typeRef.appliedTo(resultType)
      Typed(result, TypeTree.of(using repeatedType.asType))

    def transformArg(method: Symbol, paramAndArg: (Definition, Tree)): Tree =
      paramAndArg match
        case (param: ValDef, arg: Term) =>
          val paramType = param.tpt.tpe.widenParam
          if !xf.isDefinedAt(paramType, arg) then arg
          else if !param.tpt.tpe.isRepeated then xf(paramType, arg)
          else xf.transformRepeated(method, paramType, arg)
        case (_, arg) => arg

  extension (term: Term)
    def call(method: Symbol)(argss: List[List[Tree]]): Term =
      argss.foldLeft[Term](term.select(method)): (term, args) =>
        val typeArgs = for case arg: TypeTree <- args yield arg
        val termArgs = for case arg: Term <- args yield arg
        if typeArgs.isEmpty then term.appliedToArgs(termArgs)
        else term.appliedToTypeTrees(typeArgs)

    def transformTo[A: Type](
        args: Transform = PartialFunction.empty,
        body: Transform = PartialFunction.empty
    ): Expr[A] =
      val name = Symbol.freshName("$anon")
      val parents = List(TypeTree.of[Object], TypeTree.of[A])
      val cls = Symbol.newClass(Symbol.spliceOwner, name, parents.map(_.tpe), _.overridableMembers, None)

      def transformDef(method: DefDef)(argss: List[List[Tree]]): Option[Term] =
        val delegate = term.call(method.symbol):
          for (params, xs) <- method.paramss.zip(argss)
          yield for paramAndArg <- params.params.zip(xs)
          yield args.transformArg(method.symbol, paramAndArg)
        Some(body.applyOrElse((method.returnTpt.tpe, delegate), _ => delegate))

      def transformVal(value: ValDef): Option[Term] =
        val delegate = term.select(value.symbol)
        Some(body.applyOrElse((value.tpt.tpe, delegate), _ => delegate))

      val members: List[Definition] = cls.declarations
        .filterNot(_.isClassConstructor)
        .map: sym =>
          sym.tree match
            case method: DefDef => DefDef(sym, transformDef(method))
            case value: ValDef => ValDef(sym, transformVal(value))
            case _ => report.errorAndAbort(s"Not supported: $sym in ${sym.owner}")

      val newCls = New(TypeIdent(cls)).select(cls.primaryConstructor).appliedToNone
      Block(ClassDef(cls, parents, members) :: Nil, newCls).asExprOf[A]

  extension (terms: Seq[Term])
    def call(method: Symbol)(argss: List[List[Tree]]): Seq[Term] =
      terms.map(_.call(method)(argss))

    def combineTo[A: Type](
        args: Seq[Transform] = Nil,
        body: Combine = PartialFunction.empty
    ): Expr[A] =
      val name = Symbol.freshName("$anon")
      val parents = List(TypeTree.of[Object], TypeTree.of[A])
      val cls = Symbol.newClass(Symbol.spliceOwner, name, parents.map(_.tpe), _.overridableMembers, None)

      def combineDef(method: DefDef)(argss: List[List[Tree]]): Option[Term] =
        val delegates = terms
          .lazyZip(args)
          .map: (term, xf) =>
            term.call(method.symbol):
              for (params, args) <- method.paramss.zip(argss)
              yield for paramAndArg <- params.params.zip(args)
              yield xf.transformArg(method.symbol, paramAndArg)
        Some(body.applyOrElse((method.returnTpt.tpe, delegates), _ => delegates.head))

      def combineVal(value: ValDef): Option[Term] =
        val delegates = terms.map(_.select(value.symbol))
        Some(body.applyOrElse((value.tpt.tpe, delegates), _ => delegates.head))

      val members = cls.declarations
        .filterNot(_.isClassConstructor)
        .map: sym =>
          sym.tree match
            case method: DefDef => DefDef(sym, combineDef(method))
            case value: ValDef => ValDef(sym, combineVal(value))
            case _ => report.errorAndAbort(s"Not supported: $sym in ${sym.owner}")

      val newCls = New(TypeIdent(cls)).select(cls.primaryConstructor).appliedToNone
      Block(ClassDef(cls, parents, members) :: Nil, newCls).asExprOf[A]

  extension (sym: Symbol)
    def privateIn: Symbol =
      sym.privateWithin.fold(Symbol.noSymbol)(_.typeSymbol)

    def overrideKeeping(flags: Flags*): Flags =
      flags.iterator.filter(sym.flags.is).foldLeft(Flags.Override)(_ | _)

    // TODO: Include type members.
    // TODO: Handle accessibility.
    def overridableMembers: List[Symbol] = for
      cls <- This(sym).tpe :: Nil
      member <- Iterator.concat(sym.methodMembers, sym.fieldMembers, sym.typeMembers)
      if !member.isNoSymbol
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

    def isRepeated: Boolean =
      tpe.typeSymbol == defn.RepeatedParamClass

    def bounds: TypeBounds = tpe match
      case bounds: TypeBounds => bounds
      case tpe => TypeBounds(tpe, tpe)

    def widenParam: TypeRepr =
      if tpe.isRepeated then tpe.typeArgs.head else tpe.widenByName

    def summon: Term = Implicits.search(tpe) match
      case success: ImplicitSearchSuccess => success.tree
      case failure: ImplicitSearchFailure => report.errorAndAbort(failure.explanation)
      case _ => report.errorAndAbort(s"Failed to summon: ${tpe.show}")

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

end DeriveMacros
