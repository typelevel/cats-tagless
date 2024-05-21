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
    val a = A.typeSymbol
    val t = T.typeSymbol

    extension (tpe: TypeRepr)
      def first: TypeRepr =
        tpe.substituteTypes(t :: Nil, TypeRepr.of[First[A, B]] :: Nil)

    def tuple(name: String, result: TypeRepr): Term =
      val tpe = MethodType("t" :: Nil)(_ => T :: Nil, _ => result)
      Lambda(Symbol.spliceOwner, tpe, (_, args) => Select.unique(args.head.asExpr.asTerm, name))

    List(fa.asTerm, fb.asTerm).combineTo[F[(A, B)]](
      args = List(
        {
          case (tpe, arg) if tpe.contains(a) =>
            Select
              .unique(tpe.first.summonLambda[Functor](a), "map")
              .appliedToTypes(List(T, A))
              .appliedTo(arg)
              .appliedTo(tuple("_1", A))
        },
        {
          case (tpe, arg) if tpe.contains(a) =>
            Select
              .unique(tpe.first.summonLambda[Functor](a), "map")
              .appliedToTypes(List(T, B))
              .appliedTo(arg)
              .appliedTo(tuple("_2", B))
        }
      ),
      body = {
        case (tpe, af :: ag :: Nil) if tpe.contains(a) =>
          Select
            .unique(tpe.first.summonLambda[Semigroupal](a), "product")
            .appliedToTypes(List(A, B))
            .appliedTo(af, ag)
        case (tpe, af :: ag :: Nil) =>
          tpe.summonOpt[Semigroup].fold(af)(Select.unique(_, "combine").appliedTo(af, ag))
      }
    )
