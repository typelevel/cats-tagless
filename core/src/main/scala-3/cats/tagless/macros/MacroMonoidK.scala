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
import cats.{MonoidK, Monoid}

import scala.annotation.experimental
import scala.quoted.*

@experimental
object MacroMonoidK:
  inline def derive[F[_]]: MonoidK[F] = ${ monoidK }

  def monoidK[F[_]: Type](using Quotes): Expr[MonoidK[F]] = '{
    new MonoidK[F]:
      def empty[A]: F[A] = ${ deriveEmpty }
      def combineK[A](x: F[A], y: F[A]): F[A] =
        ${ MacroSemigroupK.deriveCombineK('{ x }, '{ y }) }
  }

  private[macros] def deriveEmpty[F[_]: Type, A: Type](using q: Quotes): Expr[F[A]] =
    import quotes.reflect.*
    given DeriveMacros[q.type] = new DeriveMacros

    val A = TypeRepr.of[A]

    '{ null.asInstanceOf[F[A]] }.transformTo[F[A]](
      body =
        case (_, tpe, _) if tpe.contains(A) =>
          Select
            .unique(tpe.summonLambda[MonoidK](A), "empty")
            .appliedToTypes(List(A))
        case (_, tpe, _) =>
          Select.unique(TypeRepr.of[Monoid].appliedTo(tpe).summon, "empty")
    )
