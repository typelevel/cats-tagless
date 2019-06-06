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

import cats.arrow.Profunctor
import cats.instances.all._
import cats.kernel.Eq
import cats.laws.discipline.eq._
import cats.laws.discipline.{ProfunctorTests, SerializableTests}
import cats.tagless.tests.autoProfunctorTests._
import org.scalacheck.{Arbitrary, Cogen}

class autoProfunctorTests extends CatsTaglessTestSuite {
  checkAll("Profunctor[TestAlgebra]", ProfunctorTests[TestAlgebra].profunctor[Long, String, Int, Long, String, Int])
  checkAll("Serializable Profunctor[TestAlgebra]", SerializableTests.serializable(Profunctor[TestAlgebra]))
}

object autoProfunctorTests {

  // TODO: Macro annotation.
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
  }

  object TestAlgebra {
    implicit val profunctor: Profunctor[TestAlgebra] = Derive.profunctor

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
          algebra.withoutParams
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
      withoutParameters <- Arbitrary.arbitrary[B]
    } yield new TestAlgebra[A, B] {
      override def abstractCovariant(str: String): B = absCovariant(str)
      override def concreteCovariant(str: String): B = conCovariant.getOrElse(super.concreteCovariant(_))(str)
      override def abstractContravariant(a: A): String = absContravariant(a)
      override def concreteContravariant(a: A): String = conContravariant.getOrElse(super.concreteContravariant(_))(a)
      override def abstractMixed(a: A): B = absMixed(a)
      override def concreteMixed(a: A): B = conMixed.getOrElse(super.concreteMixed(_))(a)
      override def abstractOther(str: String): String = absOther(str)
      override def concreteOther(str: String): String = conOther.getOrElse(super.concreteOther(_))(str)
      override def withoutParams: B = withoutParameters
    })
}
