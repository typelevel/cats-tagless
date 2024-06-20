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

import cats.tagless.aop.*
import cats.~>

import scala.annotation.experimental
import scala.quoted.*

@experimental
object MacroInstrument:
  inline def derive[Alg[_[_]]]: Instrument[Alg] = ${ instrument }

  def instrument[Alg[_[_]]: Type](using Quotes): Expr[Instrument[Alg]] = '{
    new Instrument[Alg]:
      def instrument[F[_]](af: Alg[F]): Alg[[X] =>> Instrumentation[F, X]] =
        ${ deriveInstrument('{ af }) }

      def mapK[F[_], G[_]](alg: Alg[F])(fk: F ~> G): Alg[G] =
        ${ MacroFunctorK.deriveMapK('{ alg }, '{ fk }) }
  }

  private[macros] def deriveInstrument[Alg[_[_]]: Type, F[_]: Type](alg: Expr[Alg[F]])(using
      Type[[X] =>> Instrumentation[F, X]]
  )(using q: Quotes): Expr[Alg[[X] =>> Instrumentation[F, X]]] =
    import quotes.reflect.*
    given DeriveMacros[q.type] = new DeriveMacros

    val F = TypeRepr.of[F]
    val Alg = TypeRepr.of[Alg]

    alg.transformTo[Alg[[X] =>> Instrumentation[F, X]]](
      body = {
        case (sym, tpe, body) if tpe.contains(F) =>
          val resultType = tpe.typeArgs.tail

          val newBody = resultType.map(_.asType) match
            case '[t] :: Nil =>
              '{ Instrumentation[F, t](${ body.asExprOf[F[t]] }, ${ Expr(Alg.typeSymbol.name) }, ${ Expr(sym.name) }) }

            case _ =>
              report.errorAndAbort(s"Expected method ${sym.name} to return $F[?] but found ${tpe.show}")

          newBody.asTerm
      }
    )
