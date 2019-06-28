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

object IsoAlgFunctor {

  def reflexive[A[_[_]]]: A <≈> A = new (A <≈> A) {
    def to:   ≈>[A, A] = ≈>.id[A]
    def from: ≈>[A, A] = ≈>.id[A]
  }

  trait Template[A1[_[_]], A2[_[_]]] extends (A1 <≈> A2) {
    def _to[F[_]](a1: A1[F]): A2[F]
    def _from[F[_]](a2: A2[F]): A1[F]


    override final val to: A1 ≈> A2 = new (A1 ≈> A2) {
      def apply[F[_]](a: A1[F]): A2[F] = _to(a)
    }
    override final val from: A2 ≈> A1 = new (A2 ≈> A1) {
      def apply[F[_]](a: A2[F]): A1[F] = _from(a)
    }
  }

}
