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

    val Alg = TypeRepr.of[Alg]
    val F = TypeRepr.of[F]
    val WeaveF = TypeRepr.of[Aspect.Weave[F, Dom, Cod, ?]]

    val algebraName = Alg.typeSymbol.name

    def paramAdvice(param: ValDef): Expr[Aspect.Advice[Eval, Dom]] =
      val isRepeated = param.tpt.tpe.isRepeated
      val isByName = param.isByName
      val paramValue = Ref(param.symbol)

      param.tpt.tpe.widenParam.asType match
        case '[t] if isByName =>
          '{
            Aspect.Advice.byName[Dom, t](
              name = ${ Expr(param.name) },
              thunk = ${ paramValue.asExprOf[t] }
            )(using summonInline)
          }
        case '[t] if isRepeated =>
          '{
            Aspect.Advice.byValue[Dom, List[t]](
              name = ${ Expr(param.name) },
              value = ${ paramValue.asExprOf[Seq[t]] }.toList
            )(using summonInline)
          }
        case '[t] =>
          '{
            Aspect.Advice.byValue[Dom, t](
              name = ${ Expr(param.name) },
              value = ${ paramValue.asExprOf[t] }
            )(using summonInline)
          }

    def codomainAdvice[T: Type](methodName: String, body: Term): Expr[Aspect.Advice.Aux[F, Cod, T]] =
      '{
        Aspect.Advice[F, Cod, T](
          adviceName = ${ Expr(methodName) },
          adviceTarget = ${ body.asExprOf[F[T]] }
        )(using summonInline)
      }

    def weave[T: Type](
        methodName: String,
        domain: Expr[List[List[Aspect.Advice[Eval, Dom]]]],
        body: Term
    ): Expr[Aspect.Weave[F, Dom, Cod, T]] =
      val codomain = codomainAdvice[T](methodName, body)
      '{
        Aspect.Weave[F, Dom, Cod, T](
          algebraName = ${ Expr(algebraName) },
          domain = $domain,
          codomain = $codomain
        )
      }

    alg.transformTo[Alg[[X] =>> Aspect.Weave[F, Dom, Cod, X]]](
      body = {
        case (sym, tpe, body) if tpe <:< WeaveF =>
          val methodName = sym.name

          val paramss = sym.tree match
            case method: DefDef =>
              // TODO should erased clauses be ignored here as well?
              method.termParamss.filter(clause => !(clause.isImplicit || clause.isGiven || clause.isErased))
            case _ => List.empty

          val domain = Expr.ofList(paramss.map(clause => Expr.ofList(clause.params.map(paramAdvice))))

          val resultType = tpe.typeArgs.lastOption
          resultType.map(_.asType) match
            case Some('[t]) =>
              weave[t](methodName, domain, body).asTerm.changeOwner(sym)

            case _ =>
              report.errorAndAbort(s"Expected method ${sym.name} to return $F[?] but found ${tpe.show}")
      }
    )
