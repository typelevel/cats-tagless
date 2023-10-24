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
import cats.laws.discipline.{SemigroupalTests, SerializableTests}
import cats.tagless.{autoInvariant, autoSemigroupal}
import cats.{Eq, Semigroupal}
import org.scalacheck.{Arbitrary, Cogen}

class autoSemigroupalTests extends CatsTaglessTestSuite {
  import autoSemigroupalTests.*

  checkAll("Semigroupal[TestAlgebra]", SemigroupalTests[TestAlgebra].semigroupal[Long, String, Int])
  checkAll("Serializable Semigroupal[TestAlgebra]", SerializableTests.serializable(Semigroupal[TestAlgebra]))
}

object autoSemigroupalTests {
  import TestInstances.*

  @autoSemigroupal
  @autoInvariant // Needed for Isomorphisms
  trait TestAlgebra[T] {
    def abstractEffect(a: String): T
    def concreteEffect(a: String): T = abstractEffect(a + " concreteEffect")
    def abstractOther(a: String): String
    def concreteOther(a: String): String = a + " concreteOther"
    def withoutParams: T
    def curried(a: String)(b: Int): T
    def headOption(ts: List[T]): Option[T]
  }

  @autoSemigroupal
  trait AlgWithExtraTypeParam[T1, T] {
    def foo(a: T1): T
  }

  @autoSemigroupal
  trait AlgWithGenericMethod[T] {
    def plusOne[A](i: A): T
  }

  @autoSemigroupal
  trait AlgWithVarArgsParameter[T] {
    def sum(xs: Int*): Int
    def product(xs: Int*): T
  }

  @autoSemigroupal
  trait AlgWithConstantReturnTypes[T] {
    def fromInt(i: Int): T
    def toString(t: T): String
    def toError(t: T): Exception
  }

  implicit def eqForTestAlgebra[T: Arbitrary: Eq]: Eq[TestAlgebra[T]] =
    Eq.by { algebra =>
      (
        algebra.abstractEffect _,
        algebra.concreteEffect _,
        algebra.abstractOther _,
        algebra.concreteOther _,
        algebra.withoutParams,
        Function.uncurried(algebra.curried _).tupled,
        algebra.headOption _
      )
    }

  implicit def arbitraryTestAlgebra[T: Arbitrary: Cogen]: Arbitrary[TestAlgebra[T]] =
    Arbitrary(for {
      absEff <- Arbitrary.arbitrary[String => T]
      conEff <- Arbitrary.arbitrary[Option[String => T]]
      absOther <- Arbitrary.arbitrary[String => String]
      conOther <- Arbitrary.arbitrary[Option[String => String]]
      withoutParameters <- Arbitrary.arbitrary[T]
      curry <- Arbitrary.arbitrary[String => Int => T]
      hOpt <- Arbitrary.arbitrary[List[T] => Option[T]]
    } yield new TestAlgebra[T] {
      override def abstractEffect(i: String) = absEff(i)
      override def concreteEffect(a: String) = conEff.getOrElse(super.concreteEffect(_))(a)
      override def abstractOther(a: String) = absOther(a)
      override def concreteOther(a: String) = conOther.getOrElse(super.concreteOther(_))(a)
      override def withoutParams = withoutParameters
      override def curried(a: String)(b: Int) = curry(a)(b)
      override def headOption(ts: List[T]): Option[T] = hOpt(ts)
    })
}
