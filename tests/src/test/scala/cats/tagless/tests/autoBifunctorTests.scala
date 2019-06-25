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

package cats.tagless.tests
import cats.Bifunctor
import cats.instances.AllInstances
import cats.kernel.Eq
import cats.laws.discipline.{BifunctorTests, SerializableTests}
import cats.tagless.autoBifunctor
import org.scalacheck.Arbitrary
import cats.laws.discipline.eq._

class autoBifunctorTests extends CatsTaglessTestSuite {
  import autoBifunctorTests._

  checkAll("Bifunctor[TestAlgebra]", BifunctorTests[TestAlgebra].bifunctor[String, Boolean, Option[String], Int, String, List[Int]])
  checkAll("Serializable Bifunctor[TestAlgebra]", SerializableTests.serializable(Bifunctor[TestAlgebra]))

  test("extra type param correctly handled") {
    val transformedAlg = AlgWithExtraTypeParamString.bimap(i => if (i > 0) Some(i) else None, new String(_))
    transformedAlg.foo("") should be(None)
    transformedAlg.foo("1") should be(Some(1))
    transformedAlg.boo("adsfdsd") should be("adsfdsd")
  }
}

object autoBifunctorTests extends TestInstances with AllInstances {

  @autoBifunctor
  trait TestAlgebra[A, B] {
    def left: A
    def right: B
    final def toTuple: (A, B) = (left, right)
    final def mapLeft[C](f: A => C): C = f(left)
    final def mapRight[C](f: B => C): C = f(right)

    def concreteMethod: Int = 0
    def fromInt(i: Int): A
    def fromString(s: String): B
  }

  object TestAlgebra {
    implicit def eqv[A: Eq, B: Eq]: Eq[TestAlgebra[A, B]] =
      Eq.by { algebra =>
        (
          algebra.left,
          algebra.right,
          algebra.fromInt _,
          algebra.fromString _,
          algebra.concreteMethod
        )
      }
  }

  implicit def arbitrary[A: Arbitrary, B: Arbitrary]: Arbitrary[TestAlgebra[A, B]] =
    Arbitrary(for {
      a1 <- Arbitrary.arbitrary[A]
      a2 <- Arbitrary.arbitrary[A]
      b <- Arbitrary.arbitrary[B]
      int <- Arbitrary.arbitrary[Int]
    } yield new TestAlgebra[A, B] {
      override def left: A = a1
      override def right: B = b
      override def concreteMethod = int
      override def fromInt(i: Int) = if (i > 0) left else a2
      override def fromString(s: String) = b
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
