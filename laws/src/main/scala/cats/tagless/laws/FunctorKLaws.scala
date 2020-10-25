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
import cats.laws._
import syntax.all._
import cats.~>

trait FunctorKLaws[F[_[_]]] extends InvariantKLaws[F] {
  implicit def F: FunctorK[F]

  def covariantIdentity[A[_]](fg: F[A]): IsEq[F[A]] =
    fg.mapK(FunctionK.id[A]) <-> fg

  def covariantComposition[A[_], B[_], C[_]](fa: F[A], f: A ~> B, g: B ~> C): IsEq[F[C]] =
    fa.mapK(f).mapK(g) <-> fa.mapK(f.andThen(g))

}

object FunctorKLaws {
  def apply[F[_[_]]](implicit ev: FunctorK[F]): FunctorKLaws[F] =
    new FunctorKLaws[F] { def F: FunctorK[F] = ev }
}
