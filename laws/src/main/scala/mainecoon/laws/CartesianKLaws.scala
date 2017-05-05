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

import cats.data.Prod

trait CartesianKLaws[A[_[_]]] {
  implicit def A: CartesianK[A]

  def cartesianAssociativity[F[_], G[_], H[_]](af: A[F], ag: A[G], ah: A[H]):
  (A[Prod[F, Prod[G, H, ?], ?]], A[Prod[Prod[F, G, ?], H, ?]]) =
    (A.product(af, A.product(ag, ah)), A.product(A.product(af, ag), ah))

}

object CartesianKLaws {
  def apply[A[_[_]]](implicit ev: CartesianK[A]): CartesianKLaws[A] =
    new CartesianKLaws[A] { val A = ev }
}
