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

import cats.Semigroup
import cats.tagless.*
import cats.data.Tuple2K

import scala.annotation.experimental
import scala.quoted.*

@experimental
object MacroSemigroupalK:
  inline def derive[Alg[_[_]]]: SemigroupalK[Alg] = ${ semigroupalK }

  def semigroupalK[Alg[_[_]]: Type](using Quotes): Expr[SemigroupalK[Alg]] = '{
    new SemigroupalK[Alg]:
      def productK[F[_], G[_]](af: Alg[F], ag: Alg[G]): Alg[Tuple2K[F, G, *]] =
        ${ deriveProductK('{ af }, '{ ag }) }
  }

  private type FirstK[F[_], G[_]] = [H[_], I[_], A] =>> (H[A], I[A]) match
    case (F[A], G[A]) => F[A]
    case _ => Tuple2K[H, I, A]

  private[macros] def deriveProductK[Alg[_[_]]: Type, F[_]: Type, G[_]: Type](
      eaf: Expr[Alg[F]],
      eag: Expr[Alg[G]]
  )(using q: Quotes): Expr[Alg[Tuple2K[F, G, *]]] =
    import quotes.reflect.*
    given DeriveMacros[q.type] = new DeriveMacros

    val F = TypeRepr.of[F]
    val G = TypeRepr.of[G]
    val T = TypeRepr.of[Tuple2K[F, G, *]]

    extension (tpe: TypeRepr)
      def firstK: TypeRepr =
        tpe.substituteTypes(T.typeSymbol :: Nil, TypeRepr.of[FirstK[F, G]] :: Nil)

    def tuple2K(name: String): Term =
      Select.unique('{ SemigroupalK }.asTerm, name).appliedToTypes(List(F, G))

    List(eaf, eag).combineTo[Alg[Tuple2K[F, G, *]]](
      args = List(
        {
          case (_, tpe, arg) if tpe.contains(F) =>
            Select
              .unique(tpe.firstK.summonLambda[FunctorK](F), "mapK")
              .appliedToTypes(List(T, F))
              .appliedTo(arg)
              .appliedTo(tuple2K("firstK"))
        },
        {
          case (_, tpe, arg) if tpe.contains(F) =>
            Select
              .unique(tpe.firstK.summonLambda[FunctorK](F), "mapK")
              .appliedToTypes(List(T, G))
              .appliedTo(arg)
              .appliedTo(tuple2K("secondK"))
        }
      ),
      body =
        case (_, tpe, af :: ag :: Nil) if tpe.contains(F) =>
          Select
            .unique(tpe.firstK.summonLambda[SemigroupalK](F), "productK")
            .appliedToTypes(List(F, G))
            .appliedTo(af, ag)
        case (_, tpe, af :: ag :: Nil) =>
          tpe.summonOpt[Semigroup].fold(af)(Select.unique(_, "combine").appliedTo(af, ag))
    )
