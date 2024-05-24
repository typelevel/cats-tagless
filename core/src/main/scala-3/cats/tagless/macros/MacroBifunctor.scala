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
import cats.{Bifunctor, Contravariant, Functor}

import scala.annotation.experimental
import scala.quoted.*

@experimental
object MacroBifunctor:
  inline def derive[F[_, _]]: Bifunctor[F] = ${ bifunctor }

  def bifunctor[F[_, _]: Type](using Quotes): Expr[Bifunctor[F]] = '{
    new Bifunctor[F]:
      def bimap[A, B, C, D](fab: F[A, B])(f: A => C, g: B => D): F[C, D] =
        ${ deriveBimap('{ fab }, '{ f }, '{ g }) }
  }

  private[macros] def deriveBimap[F[_, _]: Type, A: Type, B: Type, C: Type, D: Type](
      fab: Expr[F[A, B]],
      f: Expr[A => C],
      g: Expr[B => D]
  )(using q: Quotes): Expr[F[C, D]] =
    import quotes.reflect.*
    given DeriveMacros[q.type] = new DeriveMacros

    val A = TypeRepr.of[A]
    val B = TypeRepr.of[B]
    val C = TypeRepr.of[C]
    val D = TypeRepr.of[D]

    fab.transformTo[F[C, D]](
      args = {
        case (method, tpe, _) if tpe.containsAll(C, D) =>
          val msg = s"Both type parameters ${A.show} and ${B.show} appear in contravariant position in $method"
          report.errorAndAbort(msg)
        case (_, tpe, arg) if tpe.contains(C) =>
          Select
            .unique(tpe.summonLambda[Contravariant](C), "contramap")
            .appliedToTypes(List(C, A))
            .appliedTo(arg)
            .appliedTo(f.asTerm)
        case (_, tpe, arg) if tpe.contains(D) =>
          Select
            .unique(tpe.summonLambda[Contravariant](D), "contramap")
            .appliedToTypes(List(D, B))
            .appliedTo(arg)
            .appliedTo(g.asTerm)
      },
      body = {
        case (_, tpe, body) if tpe.containsAll(C, D) =>
          Select
            .unique(tpe.summonLambda[Bifunctor](C, D), "bimap")
            .appliedToTypes(List(A, B, C, D))
            .appliedTo(body)
            .appliedTo(f.asTerm, g.asTerm)
        case (_, tpe, body) if tpe.contains(C) =>
          Select
            .unique(tpe.summonLambda[Functor](C), "map")
            .appliedToTypes(List(A, C))
            .appliedTo(body)
            .appliedTo(f.asTerm)
        case (_, tpe, body) if tpe.contains(D) =>
          Select
            .unique(tpe.summonLambda[Functor](D), "map")
            .appliedToTypes(List(B, D))
            .appliedTo(body)
            .appliedTo(g.asTerm)
      }
    )
