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
package discipline

import org.scalacheck.Arbitrary
import org.scalacheck.Prop._
import cats.{Eq, ~>}
import cats.laws.discipline._

trait FunctorKTests[F[_[_]]] extends InvariantKTests[F] {
  def laws: FunctorKLaws[F]

  def functorK[A[_], B[_], C[_], T: Arbitrary](implicit
                                               ArbFA: Arbitrary[F[A]],
                                               ArbitraryFK: Arbitrary[A ~> B],
                                               ArbitraryFK2: Arbitrary[B ~> C],
                                               ArbitraryFK3: Arbitrary[B ~> A],
                                               ArbitraryFK4: Arbitrary[C ~> B],
                                               EqFA: Eq[F[A]],
                                               EqFC: Eq[F[C]]
                                              ): RuleSet = {
    new DefaultRuleSet(
      name = "functorK",
      parent = Some(invariantK[A, B, C]),
      "covariant identity" -> forAll(laws.covariantIdentity[A] _),
      "covariant composition" -> forAll(laws.covariantComposition[A, B, C] _))
  }
}

object FunctorKTests {
  def apply[F[_[_]]: FunctorK]: FunctorKTests[F] =
    new FunctorKTests[F] { def laws: FunctorKLaws[F] = FunctorKLaws[F] }
}
