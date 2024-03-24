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

import quoted.*
import scala.annotation.experimental
import compiletime.asMatchable

@experimental
object MacroFunctorK:
  inline def derive[Alg[_[_]]] = ${ functorK[Alg] }

  def functorK[Alg[_[_]]: Type](using Quotes): Expr[FunctorK[Alg]] = '{
    new FunctorK[Alg]:
      def mapK[F[_], G[_]](alg: Alg[F])(fk: F ~> G): Alg[G] =
        ${ deriveMapK('alg, 'fk) }
  }

  def deriveMapK[Alg[_[_]]: Type, F[_]: Type, G[_]: Type](
      alg: Expr[Alg[F]],
      fk: Expr[F ~> G]
  )(using q: Quotes): Expr[Alg[G]] =
    import quotes.reflect.*
    given DeriveMacros[q.type] = new DeriveMacros

    val F = TypeRepr.of[F]
    val G = TypeRepr.of[G]
    val g = G.typeSymbol

    alg.asTerm.transformTo[Alg[G]](
      args = {
        case (tpe, tree) if tpe.contains(g) =>
          Select
            .unique(tpe.summonLambda[ContravariantK](g), "contramapK")
            .appliedToTypes(List(G, F))
            .appliedTo(tree)
            .appliedTo(fk.asTerm)
      },
      body = {
        case (tpe, tree) if tpe.contains(g) =>
          Select
            .unique(tpe.summonLambda[FunctorK](g), "mapK")
            .appliedToTypes(List(F, G))
            .appliedTo(tree)
            .appliedTo(fk.asTerm)
      }
    )
