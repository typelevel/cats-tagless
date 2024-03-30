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
import cats.data.Tuple2K

import scala.annotation.experimental
import scala.quoted.*

@experimental
object MacroSemigroupalK:
  inline def derive[Alg[_[_]]] = ${ semigroupalK[Alg] }

  def semigroupalK[Alg[_[_]]: Type](using Quotes): Expr[SemigroupalK[Alg]] = '{
    new SemigroupalK[Alg]:
      def productK[F[_], G[_]](af: Alg[F], ag: Alg[G]): Alg[Tuple2K[F, G, *]] =
        ${ deriveProductK('af, 'ag) }
  }

  def deriveProductK[Alg[_[_]]: Type, F[_]: Type, G[_]: Type](
      eaf: Expr[Alg[F]],
      eag: Expr[Alg[G]]
  )(using q: Quotes): Expr[Alg[Tuple2K[F, G, *]]] =
    import quotes.reflect.*
    given DeriveMacros[q.type] = new DeriveMacros

    val F = TypeRepr.of[F]
    val G = TypeRepr.of[G]
    val H = TypeRepr.of[Tuple2K[F, G, *]]
    val f = F.typeSymbol
    val h = H.typeSymbol

    List(eaf.asTerm, eag.asTerm).transform[Alg[Tuple2K[F, G, *]]](
      args = List(
        {
          case (tpe, arg) if tpe.contains(h) =>
            Select
              .unique(tpe.typeArgs.head.appliedTo(tpe.typeArgs.last).summonLambda[FunctorK](f), "mapK")
              .appliedToTypes(List(H, F))
              .appliedTo(arg)
              .appliedTo(
                Select
                  .unique('{ SemigroupalK }.asTerm, "firstK")
                  .appliedToTypes(List(F, G))
              )
        },
        {
          case (tpe, arg) if tpe.contains(h) =>
            Select
              .unique(tpe.typeArgs.head.appliedTo(tpe.typeArgs.last).summonLambda[FunctorK](f), "mapK")
              .appliedToTypes(List(H, G))
              .appliedTo(arg)
              .appliedTo(
                Select
                  .unique('{ SemigroupalK }.asTerm, "secondK")
                  .appliedToTypes(List(F, G))
              )
        }
      ),
      body = {
        case (tpe, argf :: argg :: Nil) if tpe.contains(h) =>
          Select
            .unique(tpe.typeArgs.head.appliedTo(tpe.typeArgs.last).summonLambda[SemigroupalK](f), "productK")
            .appliedToTypes(List(F, G))
            .appliedTo(argf, argg)
      }
    )
