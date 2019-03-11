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

package cats.tagless
package tests


import cats.Functor
import cats.kernel.Eq
import cats.kernel.instances.string._
import cats.kernel.instances.tuple._
import cats.laws.discipline.eq._
import cats.laws.discipline.{FunctorTests, SerializableTests}
import cats.tagless.tests.autoFunctorTests._
import org.scalacheck.{Arbitrary, Cogen}

class autoFunctorTests extends CatsTaglessTestSuite {

  checkAll("Functor[TestAlgebra]", FunctorTests[TestAlgebra].functor[Long, String, Int])
  checkAll("Serializable Functor[TestAlgebra]", SerializableTests.serializable(Functor[TestAlgebra]))

  test("extra type param correctly handled") {
    val doubleAlg = AlgWithExtraTypeParamFloat.map(_.toDouble)
    doubleAlg.foo("big") should be(3d)
  }
}

object autoFunctorTests {

  @autoFunctor
  trait TestAlgebra[T] {
    def abstractEffect(a: String): T

    def concreteEffect(a: String): T = abstractEffect(a + " concreteEffect")

    def abstractOther(a: String): String

    def concreteOther(a: String): String = a + " concreteOther"

    def withoutParams: T
  }

  @autoFunctor
  trait AlgWithCurry[T] {
    def foo(a: String)(b: Int): T
  }

  @autoFunctor
  trait AlgWithExtraTypeParam[T1, T] {
    def foo(a: T1): T
  }

  object AlgWithExtraTypeParamFloat extends AlgWithExtraTypeParam[String, Float] {
    def foo(a: String): Float = a.length.toFloat
  }

  @autoFunctor
  trait AlgWithGenericMethod[T] {
    def plusOne[U](i: U): T
  }

  implicit def eqForTestAlgebra[T](implicit eqT: Eq[T]): Eq[TestAlgebra[T]] =
    Eq.by { p =>
      (p.abstractEffect: String => T) ->
        (p.concreteEffect: String => T) ->
        (p.abstractOther: String => String) ->
        (p.concreteOther: String => String) ->
        p.withoutParams
    }

  implicit def arbitraryTestAlgebra[T: Arbitrary](implicit cS: Cogen[String]): Arbitrary[TestAlgebra[T]] =
    Arbitrary {
      for {
        absEff <- Arbitrary.arbitrary[String => T]
        conEff <- Arbitrary.arbitrary[Option[String => T]]
        absOther <- Arbitrary.arbitrary[String => String]
        conOther <- Arbitrary.arbitrary[Option[String => String]]
        withoutParameters <- Arbitrary.arbitrary[T]
      } yield new TestAlgebra[T] {
        override def abstractEffect(i: String): T = absEff(i)

        override def concreteEffect(a: String): T = conEff.getOrElse(super.concreteEffect(_))(a)

        override def abstractOther(a: String): String = absOther(a)

        override def concreteOther(a: String): String = conOther.getOrElse(super.concreteOther(_))(a)

        override def withoutParams: T = withoutParameters
      }
    }
}
