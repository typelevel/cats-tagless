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

import cats.tagless.*
import cats.tagless.aop.*
import cats.{~>, Eval}

import scala.annotation.experimental
import scala.quoted.*

@experimental
object MacroAspect:
  inline def derive[Alg[_[_]], Dom[_], Cod[_]]: Aspect[Alg, Dom, Cod] = ${ aspect }

  def aspect[Alg[_[_]]: Type, Dom[_]: Type, Cod[_]: Type](using Quotes): Expr[Aspect[Alg, Dom, Cod]] = '{
    new Aspect[Alg, Dom, Cod]:
      def weave[F[_]](af: Alg[F]): Alg[[X] =>> Aspect.Weave[F, Dom, Cod, X]] =
        ${ deriveWeave('{ af }) }

      def mapK[F[_], G[_]](alg: Alg[F])(fk: F ~> G): Alg[G] =
        ${ MacroFunctorK.deriveMapK('{ alg }, '{ fk }) }
  }

  private[macros] def deriveWeave[Alg[_[_]]: Type, Dom[_]: Type, Cod[_]: Type, F[_]: Type](alg: Expr[Alg[F]])(using
      Type[Aspect.Weave[F, Dom, Cod, ?]]
  )(using q: Quotes): Expr[Alg[[X] =>> Aspect.Weave[F, Dom, Cod, X]]] =
    import quotes.reflect.*
    given DeriveMacros[q.type] = new DeriveMacros

    val WeaveF = TypeRepr.of[Aspect.Weave[F, Dom, Cod, ?]]
    val algebraName = Expr(TypeRepr.of[Alg].typeSymbol.name)

    def summon[T: Type](using Quotes): Expr[T] =
      Expr.summon[T].getOrElse(report.errorAndAbort(s"Not found: given ${Type.show[T]}"))

    def paramAdvice(param: ValDef)(using Quotes): Expr[Seq[Aspect.Advice[Eval, Dom]]] =
      val tpe = param.tpt.tpe
      tpe.widenParam.asType match
        case '[t] =>
          val name = Expr(param.name)
          val value = Ref(param.symbol)
          val dom = summon[Dom[t]]
          if tpe.isByName then '{ Aspect.Advice.byName($name, ${ value.asExprOf[t] })(using $dom) :: Nil }
          else if tpe.isRepeated then '{ ${ value.asExprOf[Seq[t]] }.map(Aspect.Advice.byValue($name, _)(using $dom)) }
          else '{ Aspect.Advice.byValue($name, ${ value.asExprOf[t] })(using $dom) :: Nil }

    // This is a hack.
    def addToGivenScope(refs: List[TermRef])(using q: Quotes): Unit =
      if refs.nonEmpty then
        try
          val _ = Expr.summon[Nothing] // fill implicit cache
          val ctxMethod = q.getClass.getMethod("ctx")
          val ctx = ctxMethod.invoke(q)
          val cache = ctxMethod.getReturnType.getDeclaredField("implicitsCache")
          cache.setAccessible(true)
          val contextual = Class.forName("dotty.tools.dotc.typer.Implicits$ContextualImplicits")
          val modified = contextual.getConstructors.head.newInstance(refs, cache.get(ctx), false, ctx)
          cache.set(ctx, modified)
        catch case _: ReflectiveOperationException => ()

    alg.transformTo[Alg[[X] =>> Aspect.Weave[F, Dom, Cod, X]]](
      body =
        case (sym, tpe, body) if tpe <:< WeaveF =>
          val (givens, clauses) = sym.tree match
            case method: DefDef =>
              method.termParamss.partition(c => c.isGiven || c.isImplicit)
            case _ =>
              (Nil, Nil)

          tpe.typeArgs.last.asType match
            case '[t] =>
              given Quotes = sym.asQuotes
              addToGivenScope(givens.flatMap(_.params).map(_.symbol.termRef))
              val methodName = Expr(sym.name)
              val cod = summon[Cod[t]]
              val domain = Expr.ofList(clauses.map(c => '{ List.concat(${ Varargs(c.params.map(paramAdvice)) }*) }))
              val codomain = '{ Aspect.Advice($methodName, ${ body.asExprOf[F[t]] })(using $cod) }
              '{ Aspect.Weave($algebraName, $domain, $codomain) }.asTerm
    )
