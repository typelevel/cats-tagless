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

trait IsoAlg1[=>:[_[_[_],_], _[_[_],_]], Alg1[_[_],_], Alg2[_[_],_]] { self =>
  def to: Alg1 =>: Alg2
  def from: Alg2 =>: Alg1
  def flip: IsoAlg1[=>:, Alg2, Alg1] = new IsoAlg1[=>:, Alg2, Alg1] {
    val to = self.from
    val from = self.to
      override def flip: IsoAlg1[=>:, Alg1, Alg2] = self
    }

  def unliftF[F[_]](implicit
    FG: =>:[Alg1, Alg2] <~< (Alg1 ≈~> Alg2),
    GF: =>:[Alg2, Alg1] <~< (Alg2 ≈~> Alg1)
  ): <~>[Alg1[F, ?], Alg2[F, ?]] = {
    type A1[ᵒ] = Alg1[F, ᵒ]
    type A2[ᵒ] = Alg2[F, ᵒ]
    new <~>.Template[A1, A2] {
      def to[A](fa: A1[A]): A2[A] = FG(self.to)(fa)
      def from[A](ga: A2[A]): A1[A] = GF(self.from)(ga)
    }
  }



//  def liftConst[BB](implicit
//    FG: =>:[Alg1, Alg2] <~< (Alg1 ≈~> Alg2),
//    GF: =>:[Alg2, Alg1] <~< (Alg2 ≈~> Alg1)
//  ): <≈~~>[CatsConst[Alg1[?[_],?], ?], AlgB2] = {
//    type AlgB1[F[_], A, B] = CatsConst[Alg1[F, A], B]
//    type AlgB2[F[_], A, B] = CatsConst[Alg2[F, A], B]
//    new <≈~~>.Template[AlgB1, AlgB2] {
//      def _to[F[_], A, B](a1: AlgB1[F, A, B]): AlgB2[F, A, B] = {
//
//        ???
//      }
//
//      def _from[F[_], A, B](a2: AlgB2[F, A, B]): AlgB1[F, A, B] = ???
//    }
//  }

  def unliftA[A](implicit
    FG: =>:[Alg1, Alg2] <~< (Alg1 ≈~> Alg2),
    GF: =>:[Alg2, Alg1] <~< (Alg2 ≈~> Alg1)
  ): Alg1[?[_], A] <≈> Alg2[?[_], A] = {
    type A1[ᵒ[_]] = Alg1[ᵒ, A]
    type A2[ᵒ[_]] = Alg2[ᵒ, A]
    new (A1 <≈> A2) {
      val to = new (A1 ≈> A2) {
        def apply[F[_]](a: A1[F]): A2[F] = FG(self.to)(a)
      }
      val from = new (A2 ≈> A1) {
        def apply[F[_]](a: A2[F]): A1[F] = GF(self.from)(a)
      }
    }
  }

  def %~(f: Alg2 ≈~> Alg2)(implicit
    FG: =>:[Alg1, Alg2] <~< (Alg1 ≈~> Alg2),
    GF: =>:[Alg2, Alg1] <~< (Alg2 ≈~> Alg1)
  ): Alg1 ≈~> Alg1 = new (Alg1 ≈~> Alg1) {
    def apply[F[_], A](alg1: Alg1[F, A]): Alg1[F, A] =
      GF(self.from)(f(FG(self.to)(alg1)))
  }

}

object IsoAlg1 {

}
