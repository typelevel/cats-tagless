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
import cats.{Functor, Contravariant}

import scala.annotation.experimental
import scala.quoted.*

@experimental
object MacroContravariant:
  inline def derive[F[_]] = ${ contramap[F] }

  def contramap[F[_]: Type](using Quotes): Expr[Contravariant[F]] = '{
    new Contravariant[F]:
      def contramap[A, B](fa: F[A])(f: B => A): F[B] =
        ${ deriveContramap('{ fa }, '{ f }) }
  }

  private[macros] def deriveContramap[F[_]: Type, A: Type, B: Type](
      fa: Expr[F[A]],
      f: Expr[B => A]
  )(using q: Quotes): Expr[F[B]] =
    import quotes.reflect.*
    given DeriveMacros[q.type] = new DeriveMacros

    val A = TypeRepr.of[A]
    val B = TypeRepr.of[B]
    val b = B.typeSymbol

    fa.asTerm.transformTo[F[B]](
      args = {
        case (tpe, arg) if tpe.contains(b) =>
          Select
            .unique(tpe.summonLambda[Functor](b), "map")
            .appliedToTypes(List(B, A))
            .appliedTo(arg)
            .appliedTo(f.asTerm)
      },
      body = {
        case (tpe, body) if tpe.contains(b) =>
          Select
            .unique(tpe.summonLambda[Contravariant](b), "contramap")
            .appliedToTypes(List(A, B))
            .appliedTo(body)
            .appliedTo(f.asTerm)
      }
    )
