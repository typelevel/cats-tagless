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
    val T2K = TypeRepr.of[Tuple2K[F, G, *]]
    val f = F.typeSymbol
    val t2k = T2K.typeSymbol

    type FirstK[F[_], G[_], A] = F[A]
    extension (tpe: TypeRepr)
      def firstK: TypeRepr =
        tpe.substituteTypes(t2k :: Nil, TypeRepr.of[FirstK] :: Nil)

    def tuple2K(name: String): Term =
      Select.unique('{ SemigroupalK }.asTerm, name).appliedToTypes(List(F, G))

    List(eaf.asTerm, eag.asTerm).combineTo[Alg[Tuple2K[F, G, *]]](
      args = List(
        {
          case (tpe, arg) if tpe.contains(t2k) =>
            Select
              .unique(tpe.firstK.summonLambda[FunctorK](f), "mapK")
              .appliedToTypes(List(T2K, F))
              .appliedTo(arg)
              .appliedTo(tuple2K("firstK"))
        },
        {
          case (tpe, arg) if tpe.contains(t2k) =>
            Select
              .unique(tpe.firstK.summonLambda[FunctorK](f), "mapK")
              .appliedToTypes(List(T2K, G))
              .appliedTo(arg)
              .appliedTo(tuple2K("secondK"))
        }
      ),
      body = {
        case (tpe, argf :: argg :: Nil) if tpe.contains(t2k) =>
          Select
            .unique(tpe.firstK.summonLambda[SemigroupalK](f), "productK")
            .appliedToTypes(List(F, G))
            .appliedTo(argf, argg)
        case (tpe, argf :: argg :: Nil) =>
          Implicits.search(TypeRepr.of[Semigroup].appliedTo(tpe)) match
            case success: ImplicitSearchSuccess => Select.unique(success.tree, "combine").appliedTo(argf, argg)
            case _ => argf
      }
    )
