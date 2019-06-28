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

trait IsoAlg2[=>:[_[_[_],_,_], _[_[_],_,_]], Alg1[_[_],_,_], Alg2[_[_],_,_]] { self =>
  def to: Alg1 =>: Alg2
  def from: Alg2 =>: Alg1
  def flip: IsoAlg2[=>:, Alg2, Alg1] = new IsoAlg2[=>:, Alg2, Alg1] {
    val to = self.from
    val from = self.to
    override def flip: IsoAlg2[=>:, Alg1, Alg2] = self
  }

  def unliftF[F[_]](implicit
    FG: (Alg1 =>: Alg2) <~< (Alg1 ≈~~> Alg2),
    GF: (Alg2 =>: Alg1) <~< (Alg2 ≈~~> Alg1)
  ): Alg1[F, ?, ?] <~~> Alg2[F, ?, ?] = {
    type A1[α, β] = Alg1[F, α, β]
    type A2[α, β] = Alg2[F, α, β]
    new <~~>.Template[A1, A2] {
      def _to[A, B]  (fa: A1[A, B]): A2[A, B] = FG(self.to)(fa)
      def _from[A, B](ga: A2[A, B]): A1[A, B] = GF(self.from)(ga)
    }
  }

  def unliftFA[F[_], A](implicit
    FG: (Alg1 =>: Alg2) <~< (Alg1 ≈~~> Alg2),
    GF: (Alg2 =>: Alg1) <~< (Alg2 ≈~~> Alg1)
  ): Alg1[F, A, ?] <~> Alg2[F, A, ?] = {
    type A1[ᵒ] = Alg1[F, A, ᵒ]
    type A2[ᵒ] = Alg2[F, A, ᵒ]
    new <~>.Template[A1, A2] {
      def to[B]  (fa: A1[B]): A2[B] = FG(self.to)(fa)
      def from[B](ga: A2[B]): A1[B] = GF(self.from)(ga)
    }
  }

  def unliftFB[F[_], B](implicit
    FG: (Alg1 =>: Alg2) <~< (Alg1 ≈~~> Alg2),
    GF: (Alg2 =>: Alg1) <~< (Alg2 ≈~~> Alg1)
  ): Alg1[F, ?, B] <~> Alg2[F, ?, B] = {
    type A1[ᵒ] = Alg1[F, ᵒ, B]
    type A2[ᵒ] = Alg2[F, ᵒ, B]
    new <~>.Template[A1, A2] {
      def to[A](fa: A1[A]): A2[A] = FG(self.to)(fa)
      def from[A](ga: A2[A]): A1[A] = GF(self.from)(ga)
    }
  }

  def unliftAB[A, B](implicit
    FG: (Alg1 =>: Alg2) <~< (Alg1 ≈~~> Alg2),
    GF: (Alg2 =>: Alg1) <~< (Alg2 ≈~~> Alg1)
  ): Alg1[?[_], A, B] <≈> Alg2[?[_], A, B] = {
    type A1[ᵒ[_]] = Alg1[ᵒ, A, B]
    type A2[ᵒ[_]] = Alg2[ᵒ, A, B]
    new <≈>.Template[A1, A2] {
      def _to  [F[_]](a1: A1[F]): A2[F] = FG(self.to)(a1)
      def _from[F[_]](a2: A2[F]): A1[F] = GF(self.from)(a2)
    }
  }

  def %~(f: Alg2 ≈~~> Alg2)(implicit
    FG: (Alg1 =>: Alg2) <~< (Alg1 ≈~~> Alg2),
    GF: (Alg2 =>: Alg1) <~< (Alg2 ≈~~> Alg1)
  ): Alg1 ≈~~> Alg1 = new (Alg1 ≈~~> Alg1) {
    def apply[F[_], A, B](alg1: Alg1[F, A, B]): Alg1[F, A, B] =
      GF(self.from)(f(FG(self.to)(alg1)))
  }

}

object IsoAlg2 {

}
