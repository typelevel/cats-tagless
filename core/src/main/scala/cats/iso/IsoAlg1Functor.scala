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

object IsoAlg1Functor {

  def reflexive[A[_[_],_]]: A <≈~> A = new (A <≈~> A) {
    def to:   ≈~>[A, A] = ≈~>.id[A]
    def from: ≈~>[A, A] = ≈~>.id[A]
  }

  trait Template[A1[_[_],_], A2[_[_],_]] extends (A1 <≈~> A2) {
    def _to[F[_], A](a1: A1[F, A]): A2[F, A]
    def _from[F[_], A](a2: A2[F, A]): A1[F, A]

    override final val to: A1 ≈~> A2 = new (A1 ≈~> A2) {
      override def apply[F[_], A](alg1: A1[F, A]): A2[F, A] = _to(alg1)
    }
    override final val from: A2 ≈~> A1 = new (A2 ≈~> A1) {
      override def apply[F[_], A](alg1: A2[F, A]): A1[F, A] = _from(alg1)
    }
  }

}
