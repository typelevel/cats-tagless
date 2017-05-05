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
package discipline


import cats.Eq
import cats.data.Prod
import mainecoon.laws.discipline.CartesianKTests.IsomorphismsK
import org.scalacheck.Prop._
import org.scalacheck.{Arbitrary, Prop}
import org.typelevel.discipline.Laws

trait CartesianKTests[F[_[_]]] extends Laws {
  def laws: CartesianKLaws[F]

  def CartesianK[A[_], B[_], C[_]](implicit
                                               ArbCF: Arbitrary[F[A]],
                                               ArbCG: Arbitrary[F[B]],
                                               ArbCH: Arbitrary[F[C]],
                                               iso: IsomorphismsK[F],
                                               EqFGH: Eq[F[λ[T => (A[T], B[T], C[T])]]]
                                              ): RuleSet = {
    new DefaultRuleSet(
      name = "CartesianK",
      parent = None,
      "cartesian associativity" -> forAll((af: F[A], ag: F[B], ah: F[C]) => iso.associativity(laws.cartesianAssociativity[A, B, C](af, ag, ah))))
  }
}


object CartesianKTests {
  def apply[F[_[_]]: CartesianK]: CartesianKTests[F] =
    new CartesianKTests[F] { def laws: CartesianKLaws[F] = CartesianKLaws[F] }

  trait IsomorphismsK[F[_[_]]] {
    def associativity[A[_], B[_], C[_]](fs: (F[Prod[A, Prod[B, C, ?], ?]], F[Prod[Prod[A, B, ?], C, ?]]))
                                       (implicit EqFGH: Eq[F[λ[T => (A[T], B[T], C[T])]]]): Prop
  }
}
