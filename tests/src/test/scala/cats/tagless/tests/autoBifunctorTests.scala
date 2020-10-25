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

import cats.{Bifunctor, Eq}
import cats.laws.discipline.{BifunctorTests, SerializableTests}
import cats.laws.discipline.eq._
import cats.tagless.autoBifunctor
import org.scalacheck.{Arbitrary, Cogen}

class autoBifunctorTests extends CatsTaglessTestSuite {
  import autoBifunctorTests._

  checkAll(
    "Bifunctor[TestAlgebra]",
    BifunctorTests[TestAlgebra].bifunctor[String, Boolean, Option[String], Int, String, List[Int]]
  )
  checkAll("Serializable Bifunctor[TestAlgebra]", SerializableTests.serializable(Bifunctor[TestAlgebra]))

  test("extra type param correctly handled") {
    val transformedAlg = AlgWithExtraTypeParamString.bimap(i => if (i > 0) Some(i) else None, new String(_))
    transformedAlg.foo("") should be(None)
    transformedAlg.foo("1") should be(Some(1))
    transformedAlg.boo("adsfdsd") should be("adsfdsd")
  }
}

object autoBifunctorTests extends TestInstances {

  @autoBifunctor
  trait TestAlgebra[A, B] {
    def left: A
    def right: B
    def toTuple: (A, B) = (left, right)
    def mapLeft[C](f: A => C): C = f(left)
    def mapRight[C](f: B => C): C = f(right)
    def concreteMethod: Int = 0
    def fromInt(i: Int): A
    def fromString(s: String): B
  }

  object TestAlgebra {
    implicit def eqv[A: Eq: Cogen, B: Eq: Cogen]: Eq[TestAlgebra[A, B]] =
      Eq.by { algebra =>
        (
          algebra.left,
          algebra.right,
          algebra.fromInt _,
          algebra.fromString _,
          algebra.concreteMethod,
          algebra.toTuple,
          algebra.mapLeft[Int] _,
          algebra.mapRight[Int] _
        )
      }
  }

  implicit def arbitrary[A: Arbitrary, B: Arbitrary]: Arbitrary[TestAlgebra[A, B]] =
    Arbitrary(for {
      a1 <- Arbitrary.arbitrary[A]
      a2 <- Arbitrary.arbitrary[A]
      b <- Arbitrary.arbitrary[B]
      int <- Arbitrary.arbitrary[Int]
      tuple <- Arbitrary.arbitrary[Option[(A, B)]]
    } yield new TestAlgebra[A, B] {
      override def left = a1
      override def right = b
      override def concreteMethod = int
      override def fromInt(i: Int) = if (i > 0) left else a2
      override def fromString(s: String) = b
      override def toTuple = tuple.getOrElse(super.toTuple)
    })

  @autoBifunctor
  trait AlgWithExtraTypeParam[T, A, B] {
    def foo(t: T): A
    def boo(t: T): B
  }

  object AlgWithExtraTypeParamString extends AlgWithExtraTypeParam[String, Int, Array[Char]] {
    override def foo(t: String) = t.length
    override def boo(t: String) = t.toCharArray
  }
}
