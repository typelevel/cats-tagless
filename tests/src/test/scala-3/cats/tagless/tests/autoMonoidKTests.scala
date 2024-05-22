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

import cats.laws.discipline.eq.*
import cats.laws.discipline.{MonoidKTests, SerializableTests}
import cats.tagless.derived.*
import cats.{Eq, MonoidK}
import org.scalacheck.{Arbitrary, Cogen}

@experimental
class autoMonoidKTests extends CatsTaglessTestSuite:
  import autoMonoidKTests.*

  checkAll("MonoidK[TestAlgebra]", MonoidKTests[TestAlgebra].monoidK[Int])
  checkAll("MonoidK[TestAlgebra]", SerializableTests.serializable(MonoidK[TestAlgebra]))

object autoMonoidKTests:
  import TestInstances.*

  trait TestAlgebra[T] derives MonoidK:
    def abstractEffect(a: String): Map[String, T]
    def concreteEffect(a: String): Map[String, T] = abstractEffect(a + " concreteEffect")
    def abstractOther(t: T): String
    def concreteOther(a: String): String = a + " concreteOther"
    def withoutParams: Int
    def headOption(ts: List[T]): Option[T]

  given [T: Arbitrary: Eq]: Eq[TestAlgebra[T]] =
    Eq.by: algebra =>
      (
        algebra.abstractEffect,
        algebra.concreteEffect,
        algebra.abstractOther,
        algebra.concreteOther,
        algebra.withoutParams,
        algebra.headOption
      )

  given [T: Arbitrary: Cogen]: Arbitrary[TestAlgebra[T]] =
    Arbitrary(for
      absEff <- Arbitrary.arbitrary[String => Map[String, T]]
      conEff <- Arbitrary.arbitrary[Option[String => Map[String, T]]]
      absOther <- Arbitrary.arbitrary[T => String]
      conOther <- Arbitrary.arbitrary[Option[String => String]]
      withoutParameters <- Arbitrary.arbitrary[Int]
      hOpt <- Arbitrary.arbitrary[List[T] => Option[T]]
    yield new TestAlgebra[T]:
      override def abstractEffect(i: String) = absEff(i)
      override def concreteEffect(a: String) = conEff.getOrElse(super.concreteEffect)(a)
      override def abstractOther(t: T) = absOther(t)
      override def concreteOther(a: String) = conOther.getOrElse(super.concreteOther)(a)
      override def withoutParams = withoutParameters
      override def headOption(ts: List[T]): Option[T] = hOpt(ts)
    )
