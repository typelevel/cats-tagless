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
import cats.{Semigroup, SemigroupK}

import scala.annotation.experimental
import scala.quoted.*

@experimental
object MacroSemigroupK:
  inline def derive[F[_]]: SemigroupK[F] = ${ semigroupK }

  def semigroupK[F[_]: Type](using Quotes): Expr[SemigroupK[F]] = '{
    new SemigroupK[F]:
      def combineK[A](x: F[A], y: F[A]): F[A] =
        ${ deriveCombineK('x, 'y) }
  }

  private[macros] def deriveCombineK[F[_]: Type, A: Type](
      x: Expr[F[A]],
      y: Expr[F[A]]
  )(using q: Quotes): Expr[F[A]] =
    import quotes.reflect.*
    given DeriveMacros[q.type] = new DeriveMacros

    val A = TypeRepr.of[A]

    List(x, y).combineTo[F[A]](
      body =
        case (_, tpe, x :: y :: Nil) if tpe.contains(A) =>
          Select
            .unique(tpe.summonLambda[SemigroupK](A), "combineK")
            .appliedToTypes(List(A))
            .appliedTo(x, y)
        case (_, tpe, x :: y :: Nil) =>
          tpe.summonOpt[Semigroup].fold(x)(Select.unique(_, "combine").appliedTo(x, y))
    )
