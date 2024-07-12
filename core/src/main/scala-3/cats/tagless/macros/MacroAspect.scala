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
import scala.compiletime.*

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

    def paramAdvice(param: ValDef): Expr[Aspect.Advice[Eval, Dom]] =
      val tpe = param.tpt.tpe
      tpe.widenParamSeq.asType match
        case '[t] =>
          val name = Expr(param.name)
          val value = Ref(param.symbol).asExprOf[t]
          if tpe.isByName then '{ Aspect.Advice.byName[Dom, t]($name, $value)(summonInline) }
          else '{ Aspect.Advice.byValue[Dom, t]($name, $value)(summonInline) }

    alg.transformTo[Alg[[X] =>> Aspect.Weave[F, Dom, Cod, X]]](
      body = {
        case (sym, tpe, body) if tpe <:< WeaveF =>
          val paramss = sym.tree match
            case method: DefDef =>
              method.termParamss.filterNot(clause => clause.isImplicit || clause.isGiven || clause.isErased)
            case _ =>
              Nil

          tpe.typeArgs.last.asType match
            case '[t] =>
              val methodName = Expr(sym.name)
              val domain = Expr.ofList(paramss.map(clause => Expr.ofList(clause.params.map(paramAdvice))))
              val codomain = '{ Aspect.Advice[F, Cod, t]($methodName, ${ body.asExprOf[F[t]] })(summonInline) }
              '{ Aspect.Weave[F, Dom, Cod, t]($algebraName, $domain, $codomain) }.asTerm
      }
    )
