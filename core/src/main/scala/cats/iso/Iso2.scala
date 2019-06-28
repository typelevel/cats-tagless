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

import cats.arrow.FunctionK
import cats.data.Tuple2K
import cats.evidence.<~<
import cats.~>

/**Isomorphism for arrows of kind (* -> *) -> (* -> *) -> * */
trait Iso2[=>:[_[_], _[_]], F[_], G[_]] { self =>
  def to: F =>: G
  def from: G =>: F
  def flip: Iso2[=>:, G, F] = new Iso2[=>:, G, F] {
    val to = self.from
    val from = self.to
    override def flip = self
  }

  def unlift[A](implicit FG: =>:[F, G] <~< (F ~> G), GF: =>:[G, F] <~< (G ~> F)): F[A] <=> G[A] =
    new (F[A] <=> G[A]) {
      def from = GF(self.from).apply
      def to   = FG(self.to).apply
    }

  def %~(f: G ~> G)(implicit FG: =>:[F, G] <~< (F ~> G), GF: =>:[G, F] <~< (G ~> F)): F ~> F =
    λ[F ~> F](a => GF(self.from)(f(FG(self.to)(a))))
}

object Iso2 extends Iso2Instances {

  def apply[F[_], G[_]](implicit iso: F <~> G): F <~> G = iso

  def reflexive[F[_]]: F <~> F = new (F <~> F) {
    final override val to:   F ~> F = FunctionK.id[F]
    final override val from: F ~> F = FunctionK.id[F]
    final override val flip: Iso2[~>, F, F] = this
  }

  implicit def isoFunctorOps[F[_], G[_]](iso: <~>[F, G]): IsoFunctorOps[F, G] = IsoFunctorOps[F, G](iso)
  implicit def toNatTrans[F[_], G[_]](iso: F <~> G): F ~> G = iso.to
  implicit def toOpNatTrans[F[_], G[_]](iso: F <~> G): G ~> F = iso.from

  def productIso2[F[_], G[_]]: Tuple2K[F, G, ?] <~> λ[A => (F[A], G[A])] = {
    type Tup2K[ᵒ] = Tuple2K[F, G, ᵒ]
    type Tup[ᵒ] = (F[ᵒ], G[ᵒ])
    new <~>.Template[Tup2K, Tup] {
      def from[A](ga: (F[A], G[A])): Tuple2K[F, G, A] = Tuple2K[F, G, A](ga._1, ga._2)
      def to[A](fa: Tuple2K[F, G, A]): (F[A], G[A]) = (fa.first, fa.second)
    }
  }

}


