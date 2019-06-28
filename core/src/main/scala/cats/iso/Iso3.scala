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

package cats.iso

import cats.evidence.<~<

trait Iso3[Arr[_[_, _], _[_, _]], F[_, _], G[_, _]] { self =>
  def to: Arr[F, G]
  def from: Arr[G, F]
  def flip: Iso3[Arr, G, F] = new Iso3[Arr, G, F] {
    val to = self.from
    val from = self.to
    override def flip = self
  }

  def unlift[A, B](implicit
                   FG: Arr[F, G] <~< (F ~~> G),
                   GF: Arr[G, F] <~< (G ~~> F)
                  ): F[A, B] <=> G[A, B] =
    new (F[A, B] <=> G[A, B]){
      def from = GF(self.from).apply _
      def to   = FG(self.to).apply _
    }

  def unliftA[A](implicit
   FG: Arr[F, G] <~< (F ~~> G),
   GF: Arr[G, F] <~< (G ~~> F)
  ): F[A, ?] <~> G[A, ?] = {
    type FA[ᵒ] = F[A, ᵒ]
    type GA[ᵒ] = G[A, ᵒ]
    new <~>.Template[FA, GA]{
      def from[B](ga: GA[B]) = GF(self.from)(ga)
      def to[B](fa: FA[B]) = FG(self.to)(fa)
    }
  }

  def unliftB[B](implicit
    FG: Arr[F, G] <~< (F ~~> G),
    GF: Arr[G, F] <~< (G ~~> F)
  ): F[?, B] <~> G[?, B] = {
    type FB[ᵒ] = F[ᵒ, B]
    type GB[ᵒ] = G[ᵒ, B]
    new <~>.Template[FB, GB]{
      def from[A](ga: GB[A]) = GF(self.from)(ga)
      def to[A](fa: FB[A]) = FG(self.to)(fa)
    }
  }

  def %~(f: G ~~> G)(implicit FG: Arr[F, G] <~< (F ~~> G), GF: Arr[G, F] <~< (G ~~> F)): F ~~> F =
    new (F ~~> F) {
      def apply[A, B](a: F[A, B]): F[A, B] = GF(self.from)(f(FG(self.to)(a)))
    }
}

object Iso3 extends Iso3Instances {
  def apply[F[_,_], G[_,_]](implicit iso: F <~~> G): F <~~> G = iso

  def reflexive[F[_,_]]: F <~~> F = new (F <~~> F) {
    override val to:   F ~~> F = BiNaturalTransformation.id[F]
    override val from: F ~~> F = BiNaturalTransformation.id[F]
  }

  implicit def isoBifunctorOps[F[_,_], G[_,_]](iso: <~~>[F, G]): IsoBifunctorOps[F, G] = IsoBifunctorOps[F, G](iso)

}
