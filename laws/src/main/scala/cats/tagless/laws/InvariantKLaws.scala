/*
 * Copyright 2019 cats-tagless maintainers
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

package cats.tagless
package laws

import cats.arrow.FunctionK
import cats.laws.*
import cats.~>
import syntax.all.*

trait InvariantKLaws[F[_[_]]] {
  implicit def F: InvariantK[F]

  def invariantIdentity[A[_]](fa: F[A]): IsEq[F[A]] =
    fa.imapK(FunctionK.id[A])(FunctionK.id[A]) <-> fa

  def invariantComposition[A[_], B[_], C[_]](fa: F[A], f1: A ~> B, f2: B ~> A, g1: B ~> C, g2: C ~> B): IsEq[F[C]] =
    fa.imapK(f1)(f2).imapK(g1)(g2) <-> fa.imapK(g1.compose(f1))(f2.compose(g2))

}

object InvariantKLaws {
  def apply[F[_[_]]](implicit ev: InvariantK[F]): InvariantKLaws[F] =
    new InvariantKLaws[F] { def F: InvariantK[F] = ev }
}
