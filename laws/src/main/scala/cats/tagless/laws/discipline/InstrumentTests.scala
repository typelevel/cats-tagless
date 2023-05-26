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

package cats.tagless.laws.discipline

import org.scalacheck.Arbitrary
import org.scalacheck.Prop.*
import cats.{Eq, ~>}
import cats.laws.discipline.*
import cats.tagless.aop.Instrument
import cats.tagless.laws.InstrumentLaws

trait InstrumentTests[F[_[_]]] extends FunctorKTests[F] {
  def laws: InstrumentLaws[F]

  def instrument[A[_], B[_], C[_], T](implicit
      ArbFA: Arbitrary[F[A]],
      ArbitraryFK: Arbitrary[A ~> B],
      ArbitraryFK2: Arbitrary[B ~> C],
      ArbitraryFK3: Arbitrary[B ~> A],
      ArbitraryFK4: Arbitrary[C ~> B],
      EqFA: Eq[F[A]],
      EqFC: Eq[F[C]]
  ): RuleSet = new RuleSet {
    val name = "instrument"
    val parents = List(functorK[A, B, C, T])
    val bases = Nil
    val props = List("instrument preserving semantics" -> forAll(laws.instrumentPreservingSemantics[A](_)))
  }
}

object InstrumentTests {
  def apply[F[_[_]]: Instrument]: InstrumentTests[F] =
    new InstrumentTests[F] { def laws: InstrumentLaws[F] = InstrumentLaws[F] }
}
