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
import cats.FlatMap

import scala.annotation.experimental
import scala.quoted.*
import scala.annotation.tailrec

@experimental
object MacroFlatMap:
  inline def derive[F[_]]: FlatMap[F] = ${ flatMap }

  def flatMap[F[_]: Type](using Quotes): Expr[FlatMap[F]] = '{
    new FlatMap[F]:
      def map[A, B](fa: F[A])(f: A => B): F[B] =
        ${ MacroFunctor.deriveMap('{ fa }, '{ f }) }
      def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] =
        ${ deriveFlatMap('{ fa }, '{ f }) }
      def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B] =
        ${ deriveTailRecM('{ a }, '{ f }) }
  }

  private[macros] def deriveFlatMap[F[_]: Type, A: Type, B: Type](
      fa: Expr[F[A]],
      f: Expr[A => F[B]]
  )(using q: Quotes): Expr[F[B]] =
    import quotes.reflect.*
    given DeriveMacros[q.type] = new DeriveMacros

    val A = TypeRepr.of[A]
    val B = TypeRepr.of[B]

    fa.transformTo[F[B]](
      args =
        case (method, tpe, _) if tpe.contains(B) =>
          report.errorAndAbort(s"Type parameter ${A.show} occurs in contravariant position in $method"),
      body =
        case (_, tpe, body) if tpe =:= B =>
          body.replace(fa, '{ $f(${ body.asExprOf[A] }) })
        case (method, tpe, _) if tpe.contains(B) =>
          report.errorAndAbort(s"Expected $method to return ${A.show} but found ${tpe.show}")
    )

  private[macros] def deriveTailRecM[F[_]: Type, A: Type, B: Type](
      a: Expr[A],
      f: Expr[A => F[Either[A, B]]]
  )(using q: Quotes): Expr[F[B]] =
    import quotes.reflect.*
    given DeriveMacros[q.type] = new DeriveMacros

    val A = TypeRepr.of[A]
    val B = TypeRepr.of[B]

    '{ $f($a) }.transformTo[F[B]](
      args =
        case (method, tpe, _) if tpe.contains(B) =>
          report.errorAndAbort(s"Type parameter ${A.show} occurs in contravariant position in $method"),
      body =
        case (method, tpe, body) if tpe =:= B =>
          given Quotes = method.asQuotes
          '{
            @tailrec def step(x: A): B =
              ${ body.replace(a, '{ x }).asExprOf[Either[A, B]] } match
                case Left(a) => step(a)
                case Right(b) => b
            step($a)
          }.asTerm
        case (method, tpe, _) if tpe.contains(B) =>
          report.errorAndAbort(s"Expected $method to return ${A.show} but found ${tpe.show}")
    )
