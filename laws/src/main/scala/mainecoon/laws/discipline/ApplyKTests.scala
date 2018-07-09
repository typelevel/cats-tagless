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

import org.scalacheck.Arbitrary
import org.scalacheck.Prop._
import cats.{Eq, ~>}
import cats.data.Tuple2K
import cats.laws.discipline._
import mainecoon.laws.discipline.SemigroupalKTests.IsomorphismsK

trait ApplyKTests[F[_[_]]] extends FunctorKTests[F] with SemigroupalKTests[F] {
  def laws: ApplyKLaws[F]

  def applyK[A[_], B[_], C[_], T: Arbitrary](implicit
                                               ArbFA: Arbitrary[F[A]],
                                               ArbCG: Arbitrary[F[B]],
                                               ArbCH: Arbitrary[F[C]],
                                               iso: IsomorphismsK[F],
                                               ArbitraryG: Arbitrary[A[T]],
                                               ArbitraryH: Arbitrary[B[T]],
                                               ArbitraryI: Arbitrary[C[T]],
                                               ArbitraryFK: Arbitrary[A ~> B],
                                               ArbitraryFK2: Arbitrary[B ~> C],
                                               ArbitraryFK3: Arbitrary[B ~> A],
                                               ArbitraryFK4: Arbitrary[C ~> B],
                                               EqFA: Eq[F[A]],
                                               EqFB: Eq[F[B]],
                                               EqFC: Eq[F[C]],
                                               EqFG: Eq[F[Tuple2K[A, Tuple2K[B, C, ?], ?]]],
                                               EqFGH: Eq[F[Tuple3K[A, B, C]#λ]]
                                              ): RuleSet =
    new RuleSet {
      val name = "applyK"
      val parents = List(functorK[A, B, C, T], semigroupalK[A, B, C])
      val bases = List.empty
      val props = List(
        "applyK associativity" -> forAll(laws.applyKAssociativity[A, B, C] _))
    }
}

object ApplyKTests {
  def apply[F[_[_]]: ApplyK]: ApplyKTests[F] =
    new ApplyKTests[F] { def laws: ApplyKLaws[F] = ApplyKLaws[F] }
}
