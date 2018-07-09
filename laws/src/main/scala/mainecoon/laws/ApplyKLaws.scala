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

package mainecoon
package laws

import cats.~>
import cats.laws._
import cats.data.Tuple2K

trait ApplyKLaws[F[_[_]]] extends SemigroupalKLaws[F] with FunctorKLaws[F] {
  implicit def F: ApplyK[F]


  def applyKAssociativity[A[_], B[_], C[_]](af: F[A], ag: F[B], ah: F[C]): IsEq[F[Tuple2K[A, Tuple2K[B, C, ?], ?]]] = {
    F.productK(af, F.productK(ag, ah)) <->
      F.mapK(F.productK(F.productK(af, ag), ah))(new (Tuple2K[Tuple2K[A, B, ?], C, ?] ~> Tuple2K[A, Tuple2K[B, C, ?], ?]) {
        def apply[X](fa: Tuple2K[Tuple2K[A, B, ?], C, X]): Tuple2K[A, Tuple2K[B, C, ?], X] =
          Tuple2K(fa.first.first, Tuple2K(fa.first.second, fa.second))
      })
  }

}

object ApplyKLaws {
  def apply[F[_[_]]](implicit ev: ApplyK[F]): ApplyKLaws[F] =
    new ApplyKLaws[F] { val F = ev }
}
