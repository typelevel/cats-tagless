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
import cats.arrow._
import cats.iso._

object iso3 {
  trait Iso3Profunctor[F[_,_], G[_,_]] extends Profunctor[F] {
    def G: Profunctor[G]
    def iso: F <~~> G

    override def dimap[A, B, C, D](fab: F[A, B])(f: C => A)(g: B => D): F[C, D] =
      iso.from(G.dimap(iso.to(fab))(f)(g))
  }

  trait Iso3Strong[F[_,_], G[_,_]] extends Strong[F] with Iso3Profunctor[F, G] {
    def G: Strong[G]

    override def first[A, B, C](fa: F[A, B]): F[(A, C), (B, C)] =
      iso.from(G.first(iso.to(fa)))

    override def second[A, B, C](fa: F[A, B]): F[(C, A), (C, B)] =
      iso.from(G.second(iso.to(fa)))
  }

  trait Iso3Compose[F[_,_], G[_,_]] extends Compose[F] {
    def G: Compose[G]
    def iso: F <~~> G

    override def compose[A, B, C](f: F[B, C], g: F[A, B]): F[A, C] =
      iso.from(G.compose(iso.to(f), iso.to(g)))
  }

  trait Iso3Category[F[_,_], G[_,_]] extends Category[F] with Iso3Compose[F, G] {
    def G: Category[G]

    override def id[A]: F[A, A] = iso.from(G.id[A])
  }

  trait Iso3Choice[F[_,_], G[_,_]] extends Choice[F] with Iso3Category[F, G] {
    def G: Choice[G]

    override def choice[A, B, C](f: F[A, C], g: F[B, C]): F[Either[A, B], C] =
      iso.from(G.choice(iso.to(f), iso.to(g)))
  }

  trait Iso3Arrow[F[_,_], G[_,_]] extends Arrow[F] with Iso3Category[F, G] with Iso3Strong[F, G] {
    def G: Arrow[G]

    override def lift[A, B](f: A => B): F[A, B] = iso.from(G.lift(f))
  }

  trait Iso3CommutativeArrow[F[_,_], G[_,_]] extends CommutativeArrow[F] with Iso3Arrow[F, G]

  trait Iso3ArrowChoice[F[_,_], G[_,_]] extends ArrowChoice[F] with Iso3Arrow[F, G] with Iso3Choice[F, G] {
    def G: ArrowChoice[G]

    override def choose[A, B, C, D](f: F[A, C])(g: F[B, D]): F[Either[A, B], Either[C, D]] =
      iso.from(G.choose(iso.to(f))(iso.to(g)))
  }

  trait Iso3Bifunctor[F[_,_], G[_,_]] extends Bifunctor[F] {
    def G: Bifunctor[G]
    def iso: F <~~> G

    override def bimap[A, B, C, D](fab: F[A, B])(f: A => C, g: B => D): F[C, D] =
      iso.from(G.bimap(iso.to(fab))(f, g))
  }

  trait Iso3Bifoldable[F[_,_], G[_,_]] extends Bifoldable[F] {
    def G: Bifoldable[G]
    def natTrans: F ~~> G

    override def bifoldLeft[A, B, C](fab: F[A, B], c: C)(f: (C, A) => C, g: (C, B) => C): C =
      G.bifoldLeft(natTrans(fab), c)(f, g)

    override def bifoldRight[A, B, C](fab: F[A, B], c: Eval[C])(f: (A, Eval[C]) => Eval[C], g: (B, Eval[C]) => Eval[C]): Eval[C] =
      G.bifoldRight(natTrans(fab), c)(f, g)
  }

  trait Iso3Bitraverse[F[_,_], G[_,_]] extends Bitraverse[F] with Iso3Bifunctor[F, G] with Iso3Bifoldable[F, G] {
    def G: Bitraverse[G]
    def natTrans: ~~>[F, G] = iso.to

    override def bitraverse[H[_]: Applicative, A, B, C, D](fab: F[A, B])(f: A => H[C], g: B => H[D]): H[F[C, D]] =
      Applicative[H].map(G.bitraverse(iso.to(fab))(f, g))(iso.from.apply)
  }

}
