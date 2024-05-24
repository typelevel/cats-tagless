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
import cats.{Apply, Semigroup}

import scala.annotation.experimental
import scala.quoted.*

@experimental
object MacroApply:
  inline def derive[F[_]]: Apply[F] = ${ apply }

  def apply[F[_]: Type](using Quotes): Expr[Apply[F]] = '{
    new Apply[F]:
      def map[A, B](fa: F[A])(f: A => B): F[B] =
        ${ MacroFunctor.deriveMap('{ fa }, '{ f }) }
      def ap[A, B](ff: F[A => B])(fa: F[A]): F[B] =
        ${ deriveAp('{ ff }, '{ fa }) }
  }

  private[macros] def deriveAp[F[_]: Type, A: Type, B: Type](
      ff: Expr[F[A => B]],
      fa: Expr[F[A]]
  )(using q: Quotes): Expr[F[B]] =
    import quotes.reflect.*
    given DeriveMacros[q.type] = new DeriveMacros

    val A = TypeRepr.of[A]
    val B = TypeRepr.of[B]
    val b = B.typeSymbol

    List(ff.asTerm, fa.asTerm).combineTo[F[B]](
      args = List.fill(2):
        case (method, tpe, _) if tpe.contains(b) =>
          report.errorAndAbort(s"Type parameter ${A.show} appears in contravariant position in $method"),
      body = {
        case (_, tpe, ff :: fa :: Nil) if tpe.contains(b) =>
          Select
            .unique(tpe.summonLambda[cats.Apply](b), "ap")
            .appliedToTypes(List(A, B))
            .appliedTo(ff)
            .appliedTo(fa)
        case (_, tpe, ff :: fa :: Nil) =>
          tpe.summonOpt[Semigroup].fold(ff)(Select.unique(_, "combine").appliedTo(ff, fa))
      }
    )
