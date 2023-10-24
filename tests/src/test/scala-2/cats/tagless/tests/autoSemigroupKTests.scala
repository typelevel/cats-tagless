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

package cats.tagless.tests

import cats.data.Validated
import cats.laws.discipline.arbitrary.*
import cats.laws.discipline.eq.*
import cats.laws.discipline.{SemigroupKTests, SerializableTests}
import cats.tagless.autoSemigroupK
import cats.{Comparison, Eq, SemigroupK}
import org.scalacheck.{Arbitrary, Cogen, Gen}

class autoSemigroupKTests extends CatsTaglessTestSuite {
  import autoSemigroupKTests.*

  checkAll("SemigroupK[TestAlgebra]", SemigroupKTests[TestAlgebra].semigroupK[Int])
  checkAll("SemigroupK[TestAlgebra]", SerializableTests.serializable(SemigroupK[TestAlgebra]))
}

object autoSemigroupKTests {
  import TestInstances.*

  @autoSemigroupK
  trait TestAlgebra[T] {
    def abstractEffect(a: String): Validated[Int, T]
    def concreteEffect(a: String): Validated[Int, T] = abstractEffect(a + " concreteEffect")
    def abstractOther(t: T): String
    def concreteOther(a: String): String = a + " concreteOther"
    def withoutParams: Comparison
    def headOption(ts: List[T]): Option[T]
  }

  implicit def eqForTestAlgebra[T: Arbitrary: Eq]: Eq[TestAlgebra[T]] =
    Eq.by { algebra =>
      (
        algebra.abstractEffect _,
        algebra.concreteEffect _,
        algebra.abstractOther _,
        algebra.concreteOther _,
        algebra.withoutParams,
        algebra.headOption _
      )
    }

  implicit def arbitraryTestAlgebra[T: Arbitrary: Cogen]: Arbitrary[TestAlgebra[T]] =
    Arbitrary(for {
      absEff <- Arbitrary.arbitrary[String => Validated[Int, T]]
      conEff <- Arbitrary.arbitrary[Option[String => Validated[Int, T]]]
      absOther <- Arbitrary.arbitrary[T => String]
      conOther <- Arbitrary.arbitrary[Option[String => String]]
      withoutParameters <- Gen.oneOf(Comparison.EqualTo, Comparison.GreaterThan, Comparison.LessThan)
      hOpt <- Arbitrary.arbitrary[List[T] => Option[T]]
    } yield new TestAlgebra[T] {
      override def abstractEffect(i: String) = absEff(i)
      override def concreteEffect(a: String) = conEff.getOrElse(super.concreteEffect(_))(a)
      override def abstractOther(t: T) = absOther(t)
      override def concreteOther(a: String) = conOther.getOrElse(super.concreteOther(_))(a)
      override def withoutParams = withoutParameters
      override def headOption(ts: List[T]): Option[T] = hOpt(ts)
    })
}
