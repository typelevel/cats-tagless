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

import cats.{Eq, Functor}
import cats.laws.discipline.{FunctorTests, SerializableTests}
import cats.laws.discipline.eq.*
import cats.tagless.derived.*
import org.scalacheck.{Arbitrary, Cogen}

@experimental
class autoFunctorTests extends CatsTaglessTestSuite:
  import autoFunctorTests.*

  checkAll("Functor[TestAlgebra]", FunctorTests[TestAlgebra].functor[Long, String, Int])
  checkAll("Serializable Functor[TestAlgebra]", SerializableTests.serializable(Functor[TestAlgebra]))

  test("extra type param correctly handled"):
    val doubleAlg = AlgWithExtraTypeParamFloat.map(_.toDouble)
    assertEquals(doubleAlg.foo("big"), 3d)

object autoFunctorTests:
  import TestInstances.*

  trait TestAlgebra[T] derives Functor:
    def abstractEffect(a: String): T
    def concreteEffect(a: String): T = abstractEffect(a + " concreteEffect")
    def abstractOther(a: String): String
    def concreteOther(a: String): String = a + " concreteOther"
    def withoutParams: T
    def toList(xs: List[Int]): List[T]
    def fromFunction(f: T => String): T

  trait AlgWithCurry[T] derives Functor:
    def foo(a: String)(b: Int): T

  trait AlgWithExtraTypeParam[T1, T] derives Functor:
    def foo(a: T1): T

  object AlgWithExtraTypeParamFloat extends AlgWithExtraTypeParam[String, Float]:
    def foo(a: String): Float = a.length.toFloat

  trait AlgWithGenericMethod[T] derives Functor:
    def plusOne[A](i: A): T

  trait AlgWithVarArgsParameter[T] derives Functor:
    def sum(xs: Int*): Int
    def product(xs: Int*): T

  given [T: Eq: Cogen]: Eq[TestAlgebra[T]] =
    Eq.by: algebra =>
      (
        algebra.abstractEffect,
        algebra.concreteEffect,
        algebra.abstractOther,
        algebra.concreteOther,
        algebra.withoutParams,
        algebra.toList,
        algebra.fromFunction
      )

  given [T: Arbitrary]: Arbitrary[TestAlgebra[T]] =
    Arbitrary(for
      absEff <- Arbitrary.arbitrary[String => T]
      conEff <- Arbitrary.arbitrary[Option[String => T]]
      absOther <- Arbitrary.arbitrary[String => String]
      conOther <- Arbitrary.arbitrary[Option[String => String]]
      withoutParameters <- Arbitrary.arbitrary[T]
      list <- Arbitrary.arbitrary[List[Int] => List[T]]
      fromFn <- Arbitrary.arbitrary[(T => String) => T]
    yield new TestAlgebra[T]:
      override def abstractEffect(i: String) = absEff(i)
      override def concreteEffect(a: String) = conEff.getOrElse(super.concreteEffect)(a)
      override def abstractOther(a: String) = absOther(a)
      override def concreteOther(a: String) = conOther.getOrElse(super.concreteOther)(a)
      override def withoutParams = withoutParameters
      override def toList(xs: List[Int]) = list(xs)
      override def fromFunction(f: T => String) = fromFn(f))
