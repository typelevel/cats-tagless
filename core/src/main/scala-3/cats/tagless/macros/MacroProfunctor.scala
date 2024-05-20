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
import cats.{Contravariant, Functor}

import scala.annotation.experimental
import scala.quoted.*
import cats.arrow.Profunctor

@experimental
object MacroProfunctor:
  inline def derive[F[_, _]] = ${ profunctor[F] }

  def profunctor[F[_, _]: Type](using Quotes): Expr[Profunctor[F]] = '{
    new Profunctor[F]:
      def dimap[A, B, C, D](fab: F[A, B])(f: C => A)(g: B => D): F[C, D] =
        ${ deriveDimap('{ fab }, '{ f }, '{ g }) }
  }

  private[macros] def deriveDimap[F[_, _]: Type, A: Type, B: Type, C: Type, D: Type](
      fab: Expr[F[A, B]],
      f: Expr[C => A],
      g: Expr[B => D]
  )(using q: Quotes): Expr[F[C, D]] =
    import quotes.reflect.*
    given DeriveMacros[q.type] = new DeriveMacros

    val A = TypeRepr.of[A]
    val B = TypeRepr.of[B]
    val C = TypeRepr.of[C]
    val D = TypeRepr.of[D]
    val c = C.typeSymbol
    val d = D.typeSymbol

    fab.asTerm.transformTo[F[C, D]](
      args = {
        case (tpe, arg) if tpe.contains(c) && tpe.contains(d) =>
          Select
            .unique(tpe.summonLambda[Profunctor](d, c), "dimap")
            .appliedToTypes(List(D, C, B, A))
            .appliedTo(arg)
            .appliedTo(g.asTerm)
            .appliedTo(f.asTerm)
        case (tpe, arg) if tpe.contains(c) =>
          Select
            .unique(tpe.summonLambda[Functor](c), "map")
            .appliedToTypes(List(C, A))
            .appliedTo(arg)
            .appliedTo(f.asTerm)
        case (tpe, arg) if tpe.contains(d) =>
          Select
            .unique(tpe.summonLambda[Contravariant](d), "contramap")
            .appliedToTypes(List(D, B))
            .appliedTo(arg)
            .appliedTo(g.asTerm)
      },
      body = {
        case (tpe, body) if tpe.contains(c) && tpe.contains(d) =>
          Select
            .unique(tpe.summonLambda[Profunctor](c, d), "dimap")
            .appliedToTypes(List(A, B, C, D))
            .appliedTo(body)
            .appliedTo(f.asTerm)
            .appliedTo(g.asTerm)
        case (tpe, body) if tpe.contains(c) =>
          Select
            .unique(tpe.summonLambda[Contravariant](c), "contramap")
            .appliedToTypes(List(A, C))
            .appliedTo(body)
            .appliedTo(f.asTerm)
        case (tpe, body) if tpe.contains(d) =>
          Select
            .unique(tpe.summonLambda[Functor](d), "map")
            .appliedToTypes(List(B, D))
            .appliedTo(body)
            .appliedTo(g.asTerm)
      }
    )
