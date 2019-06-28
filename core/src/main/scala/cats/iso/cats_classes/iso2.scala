/*
 * Copyright 2017 Kailuo Wang
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

package cats.iso.cats_classes

import cats._
import cats.implicits._

object iso2 {

  trait Iso2Invariant[F[_], G[_]] extends Invariant[F] {
    def G: Invariant[G]
    def fg: F ~> G
    def gf: G ~> F
    override def imap[A, B](fa: F[A])(f: A => B)(g: B => A): F[B] =
      gf(G.imap(fg(fa))(f)(g))
  }

  trait Iso2Semigroupal[F[_], G[_]] extends Semigroupal[F] {
    def G: Semigroupal[G]
    def fg: F ~> G
    def gf: G ~> F

    override def product[A, B](fa: F[A], fb: F[B]): F[(A, B)] =
      gf(G.product(fg(fa), fg(fb)))
  }

  trait Iso2Contravariant[F[_], G[_]] extends Contravariant[F] with Iso2Invariant[F, G] {
    def G: Contravariant[G]

    override def contramap[A, B](fa: F[A])(f: B => A): F[B] =
      gf(G.contramap(fg(fa))(f))
  }

  trait Iso2Functor[F[_], G[_]] extends Functor[F] with Iso2Invariant[F, G] {
    def G: Functor[G]

    override def map[A, B](fa: F[A])(f: A => B): F[B] =
      gf(G.map(fg(fa))(f))
  }

  trait Iso2InvariantSemigroupal[F[_], G[_]] extends InvariantSemigroupal[F]
    with Iso2Invariant[F, G] with Iso2Semigroupal[F, G]
  { def G: InvariantSemigroupal[G] }

  trait Iso2ContravariantSemigroupal[F[_], G[_]] extends ContravariantSemigroupal[F]
    with Iso2Contravariant[F, G] with Iso2InvariantSemigroupal[F, G]
  { def G: ContravariantSemigroupal[G] }

  trait Iso2Apply[F[_], G[_]] extends Apply[F]
    with Iso2Functor[F, G] with Iso2InvariantSemigroupal[F, G] {
    def G: Apply[G]

    override def ap[A, B](ff: F[A => B])(fa: F[A]): F[B] =
      gf(G.ap(fg(ff))(fg(fa)))
  }

  trait Iso2InvariantMonoidal[F[_], G[_]] extends InvariantMonoidal[F] with Iso2InvariantSemigroupal[F, G] {
    def G: InvariantMonoidal[G]

    override def unit: F[Unit] = gf(G.unit)
  }

  trait Iso2ContravariantMonoidal[F[_], G[_]] extends ContravariantMonoidal[F]
    with Iso2ContravariantSemigroupal[F, G] with Iso2InvariantMonoidal[F, G]
  { def G: ContravariantMonoidal[G] }

  trait Iso2Applicative[F[_], G[_]] extends Applicative[F]
    with Iso2Apply[F, G] with Iso2InvariantMonoidal[F, G] {
    def G: Applicative[G]

    override def pure[A](x: A): F[A] = gf(G.pure(x))
  }

  trait Iso2FlatMap[F[_], G[_]] extends FlatMap[F] with Iso2Apply[F, G] {
    def G: FlatMap[G]
    override def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] =
      gf(G.flatMap(fg(fa))(f >>> fg.apply))
    override def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B] =
      gf(G.tailRecM(a)(f >>> fg.apply))
  }

  trait Iso2Monad[F[_], G[_]] extends Monad[F] with Iso2Applicative[F, G] with Iso2FlatMap[F, G]
  { def G: Monad[G] }

}
