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
object MacroInvariantK:
  inline def derive[Alg[_[_]]]: InvariantK[Alg] = ${ invariantK }

  def invariantK[Alg[_[_]]: Type](using Quotes): Expr[InvariantK[Alg]] = '{
    new InvariantK[Alg]:
      def imapK[F[_], G[_]](alg: Alg[F])(fk: F ~> G)(gk: G ~> F): Alg[G] =
        ${ deriveIMapK('{ alg }, '{ fk }, '{ gk }) }
  }

  private[macros] def deriveIMapK[Alg[_[_]]: Type, F[_]: Type, G[_]: Type](
      alg: Expr[Alg[F]],
      fk: Expr[F ~> G],
      gk: Expr[G ~> F]
  )(using q: Quotes): Expr[Alg[G]] =
    import quotes.reflect.*
    given DeriveMacros[q.type] = new DeriveMacros

    val F = TypeRepr.of[F]
    val G = TypeRepr.of[G]
    val g = G.typeSymbol

    alg.asTerm.transformTo[Alg[G]](
      args = {
        case (_, tpe, arg) if tpe.contains(g) =>
          Select
            .unique(tpe.summonLambda[InvariantK](g), "imapK")
            .appliedToTypes(List(G, F))
            .appliedTo(arg)
            .appliedTo(gk.asTerm)
            .appliedTo(fk.asTerm)
      },
      body = {
        case (_, tpe, body) if tpe.contains(g) =>
          Select
            .unique(tpe.summonLambda[InvariantK](g), "imapK")
            .appliedToTypes(List(F, G))
            .appliedTo(body)
            .appliedTo(fk.asTerm)
            .appliedTo(gk.asTerm)
      }
    )
