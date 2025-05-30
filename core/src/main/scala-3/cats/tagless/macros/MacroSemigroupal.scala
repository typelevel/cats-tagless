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

import cats.{Functor, Semigroup, Semigroupal}
import cats.tagless.*

import scala.annotation.experimental
import scala.quoted.*

@experimental
object MacroSemigroupal:
  inline def derive[F[_]]: Semigroupal[F] = ${ semigroupal }

  def semigroupal[F[_]: Type](using Quotes): Expr[Semigroupal[F]] = '{
    new Semigroupal[F]:
      def product[A, B](fa: F[A], fb: F[B]): F[(A, B)] =
        ${ deriveProduct('{ fa }, '{ fb }) }
  }

  private type First[A, B] = [X, Y] =>> (X, Y) match
    case (A, B) => A
    case _ => (X, Y)

  private[macros] def deriveProduct[F[_]: Type, A: Type, B: Type](
      fa: Expr[F[A]],
      fb: Expr[F[B]]
  )(using q: Quotes): Expr[F[(A, B)]] =
    import quotes.reflect.*
    given DeriveMacros[q.type] = new DeriveMacros

    val A = TypeRepr.of[A]
    val B = TypeRepr.of[B]
    val T = TypeRepr.of[(A, B)]

    extension (tpe: TypeRepr)
      def first: TypeRepr =
        tpe.substituteTypes(T.typeSymbol :: Nil, TypeRepr.of[First[A, B]] :: Nil)

    def tuple(method: Symbol, name: String, result: TypeRepr): Term =
      val tpe = MethodType("t" :: Nil)(_ => T :: Nil, _ => result)
      Lambda(method, tpe, (_, args) => Select.unique(args.head.asExpr.asTerm, name))

    List(fa, fb).combineTo[F[(A, B)]](
      args = List(
        {
          case (method, tpe, arg) if tpe.contains(A) =>
            Select
              .unique(tpe.first.summonLambda[Functor](A), "map")
              .appliedToTypes(List(T, A))
              .appliedTo(arg)
              .appliedTo(tuple(method, "_1", A))
        },
        {
          case (method, tpe, arg) if tpe.contains(A) =>
            Select
              .unique(tpe.first.summonLambda[Functor](A), "map")
              .appliedToTypes(List(T, B))
              .appliedTo(arg)
              .appliedTo(tuple(method, "_2", B))
        }
      ),
      body =
        case (_, tpe, af :: ag :: Nil) if tpe.contains(A) =>
          Select
            .unique(tpe.first.summonLambda[Semigroupal](A), "product")
            .appliedToTypes(List(A, B))
            .appliedTo(af, ag)
        case (_, tpe, af :: ag :: Nil) =>
          tpe.summonOpt[Semigroup].fold(af)(Select.unique(_, "combine").appliedTo(af, ag))
    )
