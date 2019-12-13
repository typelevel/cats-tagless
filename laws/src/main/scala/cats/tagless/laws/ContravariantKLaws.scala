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

package cats.tagless.laws

import cats.arrow.FunctionK
import cats.laws._
import cats.tagless.ContravariantK
import cats.tagless.syntax.contravariantK._
import cats.~>

trait ContravariantKLaws[F[_[_]]] extends InvariantKLaws[F]{
  implicit def F: ContravariantK[F]

  def contravariantIdentity[A[_]](fg: F[A]): IsEq[F[A]] =
    fg.contramapK(FunctionK.id[A]) <-> fg

  def contravariantComposition[A[_], B[_], C[_]](fa: F[A], f: B ~> A, g: C ~> B): IsEq[F[C]] =
    fa.contramapK(f).contramapK(g) <-> fa.contramapK(f compose g)
}

object ContravariantKLaws {
  def apply[F[_[_]]](implicit ev: ContravariantK[F]): ContravariantKLaws[F] =
    new ContravariantKLaws[F] { def F = ev }
}
