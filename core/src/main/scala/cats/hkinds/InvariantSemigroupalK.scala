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

package cats.hkinds

import cats.data.Tuple2K
import cats.tagless._
import cats.~>

trait InvariantSemigroupalK[A[_[_]]] extends InvariantK[A] with SemigroupalK[A] {
  def imap2K[F[_], G[_], H[_]](af: A[F], ag: A[G])(f: Tuple2K[F, G, ?] ~> H)(g: H ~> Tuple2K[F, G, ?]): A[H] =
    imapK(productK(af, ag))(f)(g)
}
object InvariantSemigroupalK {
  def apply[A[_[_]]](implicit invK: InvariantK[A], semK: SemigroupalK[A]): InvariantSemigroupalK[A] =
    new InvariantSemigroupalK[A] {
      def productK[F[_], G[_]](af: A[F], ag: A[G]): A[Tuple2K[F, G, ?]] = semK.productK(af, ag)
      def imapK[F[_], G[_]](af: A[F])(fg: ~>[F, G])(gf: ~>[G, F]): A[G] = invK.imapK(af)(fg)(gf)
    }
}