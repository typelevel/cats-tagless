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


import cats.{Eq, ~>}
import org.scalacheck.Prop._
import org.scalacheck.Arbitrary
import cats.laws.discipline._

trait TraverseKTests[F[_[_]]] extends FunctorKTests[F] {
  def laws: TraverseKLaws[F]

  def traverseK[A[_], B[_], C[_], T: Arbitrary](implicit
                                               ArbFA: Arbitrary[F[A]],
                                               ArbitraryFK: Arbitrary[A ~> B],
                                               ArbitraryFK2: Arbitrary[B ~> C],
                                               ArbitraryFK3: Arbitrary[B ~> A],
                                               ArbitraryFK4: Arbitrary[C ~> B],
                                               EqFA: Eq[F[A]],
                                               EqFB: Eq[F[B]],
                                               EqFC: Eq[F[C]]
                                              ): RuleSet = {
    new DefaultRuleSet(
      name = "TraverseKK",
      parent = Some(functorK[A, B, C, T]),
      "traverseK identity" -> forAll((fa: F[A], f: A ~> B) => laws.traverseKIdentity[A, B](fa)(f))
    )
  }
}


object TraverseKTests {
  def apply[F[_[_]]: TraverseK]: TraverseKTests[F] =
    new TraverseKTests[F] { def laws: TraverseKLaws[F] = TraverseKLaws[F] }
}
