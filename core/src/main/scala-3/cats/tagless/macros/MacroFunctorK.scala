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
import cats.~>

import scala.annotation.experimental
import scala.quoted.*

@experimental
object MacroFunctorK:
  inline def derive[Alg[_[_]]]: FunctorK[Alg] = ${ functorK[Alg] }

  def functorK[Alg[_[_]]: Type](using Quotes): Expr[FunctorK[Alg]] = '{
    new FunctorK[Alg]:
      def mapK[F[_], G[_]](alg: Alg[F])(fk: F ~> G): Alg[G] =
        ${ deriveMapK('{ alg }, '{ fk }) }
  }

  private[macros] def deriveMapK[Alg[_[_]]: Type, F[_]: Type, G[_]: Type](
      alg: Expr[Alg[F]],
      fk: Expr[F ~> G]
  )(using q: Quotes): Expr[Alg[G]] =
    import quotes.reflect.*
    given DeriveMacros[q.type] = new DeriveMacros

    val F = TypeRepr.of[F]
    val G = TypeRepr.of[G]

    alg.transformTo[Alg[G]](
      args = {
        case (_, tpe, arg) if tpe.contains(G) =>
          Select
            .unique(tpe.summonLambda[ContravariantK](G), "contramapK")
            .appliedToTypes(List(G, F))
            .appliedTo(arg)
            .appliedTo(fk.asTerm)
      },
      body = {
        case (_, tpe, body) if tpe.contains(G) =>
          Select
            .unique(tpe.summonLambda[FunctorK](G), "mapK")
            .appliedToTypes(List(F, G))
            .appliedTo(body)
            .appliedTo(fk.asTerm)
      }
    )
