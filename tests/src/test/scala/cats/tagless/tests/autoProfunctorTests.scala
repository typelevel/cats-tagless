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
package tests

import cats.Eq
import cats.arrow.Profunctor
import cats.laws.discipline.{ProfunctorTests, SerializableTests}
import cats.laws.discipline.eq._
import org.scalacheck.{Arbitrary, Cogen}

class autoProfunctorTests extends CatsTaglessTestSuite {
  import autoProfunctorTests._

  checkAll("Profunctor[TestAlgebra]", ProfunctorTests[TestAlgebra].profunctor[Long, String, Int, Long, String, Int])
  checkAll("Serializable Profunctor[TestAlgebra]", SerializableTests.serializable(Profunctor[TestAlgebra]))

  test("extra type param correctly handled") {
    val asStringAlg = AlgWithExtraTypeParamString.dimap((s: String) => s.length)(_ + 1)
    asStringAlg.foo("base", "x2") should be(9d)
  }
}

object autoProfunctorTests {
   import TestInstances._

  @autoProfunctor
  trait TestAlgebra[A, B] {
    def abstractCovariant(str: String): B
    def concreteCovariant(str: String): B = abstractCovariant(str + " concreteCovariant")
    def abstractContravariant(a: A): String
    def concreteContravariant(a: A): String = abstractContravariant(a) + " concreteContravariant"
    def abstractMixed(a: A): B
    def concreteMixed(a: A): B = abstractMixed(a)
    def abstractOther(str: String): String
    def concreteOther(str: String): String = str + " concreteOther"
    def withoutParams: B
    def fromList(as: List[A]): List[B]
  }

  object TestAlgebra {
    implicit def eqv[A: Arbitrary, B: Eq]: Eq[TestAlgebra[A, B]] =
      Eq.by { algebra =>
        (
          algebra.abstractCovariant _,
          algebra.concreteCovariant _,
          algebra.abstractContravariant _,
          algebra.concreteContravariant _,
          algebra.abstractMixed _,
          algebra.concreteMixed _,
          algebra.abstractOther _,
          algebra.concreteOther _,
          algebra.withoutParams,
          algebra.fromList _
        )
      }
  }

  implicit def arbitrary[A: Cogen, B: Arbitrary]: Arbitrary[TestAlgebra[A, B]] =
    Arbitrary(for {
      absCovariant <- Arbitrary.arbitrary[String => B]
      conCovariant <- Arbitrary.arbitrary[Option[String => B]]
      absContravariant <- Arbitrary.arbitrary[A => String]
      conContravariant <- Arbitrary.arbitrary[Option[A => String]]
      absMixed <- Arbitrary.arbitrary[A => B]
      conMixed <- Arbitrary.arbitrary[Option[A => B]]
      absOther <- Arbitrary.arbitrary[String => String]
      conOther <- Arbitrary.arbitrary[Option[String => String]]
      noParams <- Arbitrary.arbitrary[B]
      list <- Arbitrary.arbitrary[List[A] => List[B]]
    } yield new TestAlgebra[A, B] {
      override def abstractCovariant(str: String) = absCovariant(str)
      override def concreteCovariant(str: String) = conCovariant.getOrElse(super.concreteCovariant(_))(str)
      override def abstractContravariant(a: A) = absContravariant(a)
      override def concreteContravariant(a: A) = conContravariant.getOrElse(super.concreteContravariant(_))(a)
      override def abstractMixed(a: A) = absMixed(a)
      override def concreteMixed(a: A) = conMixed.getOrElse(super.concreteMixed(_))(a)
      override def abstractOther(str: String) = absOther(str)
      override def concreteOther(str: String) = conOther.getOrElse(super.concreteOther(_))(str)
      override def withoutParams = noParams
      override def fromList(as: List[A]) = list(as)
    })

  @autoProfunctor
  trait AlgWithExtraTypeParam[T, A, B] {
    def foo(t: T, a: A): B
  }

  object AlgWithExtraTypeParamString extends AlgWithExtraTypeParam[String, Int, Double] {
    override def foo(t: String, a: Int) = t.length * a.toDouble
  }
}
