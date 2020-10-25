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
import org.scalacheck.Prop._
import cats.{Eq, ~>}
import cats.laws.discipline._
import cats.tagless.aop.{Aspect, Instrumentation}
import cats.tagless.laws.AspectLaws

trait AspectTests[F[_[_]], Dom[_], Cod[_]] extends InstrumentTests[F] {
  def laws: AspectLaws[F, Dom, Cod]

  def aspect[A[_], B[_], C[_], T: Arbitrary](implicit
      ArbFA: Arbitrary[F[A]],
      ArbitraryFK: Arbitrary[A ~> B],
      ArbitraryFK2: Arbitrary[B ~> C],
      ArbitraryFK3: Arbitrary[B ~> A],
      ArbitraryFK4: Arbitrary[C ~> B],
      EqFA: Eq[F[A]],
      EqFC: Eq[F[C]],
      EqFInstrumentation: Eq[F[Instrumentation[A, *]]]
  ): RuleSet = new RuleSet {
    val name = "aspect"
    val parents = List(instrument[A, B, C, T])
    val bases = Nil
    val props = List(
      "weave preserving semantics" -> forAll(laws.weavePreservingSemantics[A] _),
      "weave instrument consistency" -> forAll(laws.weaveInstrumentConsistency[A] _)
    )
  }
}

object AspectTests {
  def apply[F[_[_]], Dom[_], Cod[_]](implicit ev: Aspect[F, Dom, Cod]): AspectTests[F, Dom, Cod] =
    new AspectTests[F, Dom, Cod] { def laws: AspectLaws[F, Dom, Cod] = AspectLaws[F, Dom, Cod] }
}
