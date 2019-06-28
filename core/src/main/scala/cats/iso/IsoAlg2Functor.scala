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

object IsoAlg2Functor {

  def reflexive[A[_[_],_,_]]: A <≈~~> A = new (A <≈~~> A) {
    override final val to:   ≈~~>[A, A] = ≈~~>.id[A]
    override final val from: ≈~~>[A, A] = ≈~~>.id[A]
  }

  def isoAlg2ConstSConst[Alg[_[_],_], Rep[_[_],_]](i: Alg <≈~> Rep)
  : λ[(F[_], A, B) => ShapeConst[Alg[F, A], B]] <≈~~> λ[(F[_], A, B) => CatsConst[Rep[F, A], B]] = {
    type SC[T[_], a, b] = ShapeConst[Alg[T, a], b]
    type CC[T[_], a, b] = CatsConst[Rep[T, a], b]
    new Template[SC, CC] {
      def _to  [F[_], A, B](a1: SC[F, A, B]): CC[F, A, B] = CatsConst.of[B](i.to(a1))
      def _from[F[_], A, B](a2: CC[F, A, B]): SC[F, A, B] = i.from(a2.getConst)
    }
  }

  trait Template[A1[_[_],_,_], A2[_[_],_,_]] extends (A1 <≈~~> A2) {
    def _to[F[_], A, B](a1: A1[F, A, B]): A2[F, A, B]
    def _from[F[_], A, B](a2: A2[F, A, B]): A1[F, A, B]

    override final val to: A1 ≈~~> A2 = new (A1 ≈~~> A2) {
      override def apply[F[_], A, B](alg1: A1[F, A, B]): A2[F, A, B] = _to(alg1)
    }
    override final val from: A2 ≈~~> A1 = new (A2 ≈~~> A1) {
      override def apply[F[_], A, B](alg1: A2[F, A, B]): A1[F, A, B] = _from(alg1)
    }
  }

}
