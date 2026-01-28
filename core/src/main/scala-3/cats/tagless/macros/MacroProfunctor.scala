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
  inline def derive[F[_, _]]: Profunctor[F] = ${ profunctor }

  def profunctor[F[_, _]: Type](using Quotes): Expr[Profunctor[F]] = '{
    new Profunctor[F]:
      def dimap[A, B, C, D](fab: F[A, B])(f: C => A)(g: B => D): F[C, D] =
        ${ deriveDimap('fab, 'f, 'g) }
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

    fab.transformTo[F[C, D]](
      args =
        case (_, tpe, arg) if tpe.containsAll(C, D) =>
          Select
            .unique(tpe.summonLambda[Profunctor](D, C), "dimap")
            .appliedToTypes(List(D, C, B, A))
            .appliedTo(arg)
            .appliedTo(g.asTerm)
            .appliedTo(f.asTerm)
        case (_, tpe, arg) if tpe.contains(C) =>
          Select
            .unique(tpe.summonLambda[Functor](C), "map")
            .appliedToTypes(List(C, A))
            .appliedTo(arg)
            .appliedTo(f.asTerm)
        case (_, tpe, arg) if tpe.contains(D) =>
          Select
            .unique(tpe.summonLambda[Contravariant](D), "contramap")
            .appliedToTypes(List(D, B))
            .appliedTo(arg)
            .appliedTo(g.asTerm),
      body =
        case (_, tpe, body) if tpe.containsAll(C, D) =>
          Select
            .unique(tpe.summonLambda[Profunctor](C, D), "dimap")
            .appliedToTypes(List(A, B, C, D))
            .appliedTo(body)
            .appliedTo(f.asTerm)
            .appliedTo(g.asTerm)
        case (_, tpe, body) if tpe.contains(C) =>
          Select
            .unique(tpe.summonLambda[Contravariant](C), "contramap")
            .appliedToTypes(List(A, C))
            .appliedTo(body)
            .appliedTo(f.asTerm)
        case (_, tpe, body) if tpe.contains(D) =>
          Select
            .unique(tpe.summonLambda[Functor](D), "map")
            .appliedToTypes(List(B, D))
            .appliedTo(body)
            .appliedTo(g.asTerm)
    )
