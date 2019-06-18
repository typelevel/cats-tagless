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


import cats.laws.discipline.{ContravariantTests, ExhaustiveCheck, SerializableTests}
import autoContravariantTest._
import cats.kernel.Eq
import org.scalacheck.{Arbitrary, Cogen}
import cats.laws.discipline.eq._
import cats.Contravariant

class autoContravariantTests extends CatsTaglessTestSuite {

  checkAll("SimpleAlg[Option]", ContravariantTests[SimpleAlg].contravariant[Float, String, Int])
  checkAll("Invariant[SimpleAlg]", SerializableTests.serializable(Contravariant[SimpleAlg]))

  test("non effect method correctly delegated") {
    val doubleAlg = AlgWithNonEffectMethodFloat.contramap[String](_.toFloat)
    doubleAlg.foo2("big") should be("gib")
    doubleAlg.foo3("3") should be("3")
  }

  test("extra type param correctly handled") {
    val doubleAlg = AlgWithExtraTypeParamFloat.contramap[String](_.toFloat)
    doubleAlg.foo("big", "3") should be(6)
  }

  test("Alg with non effect method with default Impl") {
    val intAlg = new AlgWithDefaultImpl[Int] {
      def plusOne(i: Int): Int = i + 1
    }

    intAlg.contramap[String](_.toInt).plusOne("3") should be(4)
    intAlg.contramap[String](_.toInt).minusOne(2) should be(1)
  }

}

object autoContravariantTest {

  @autoContravariant
  trait SimpleAlg[T] {
    def foo(a: T): String
  }

  @autoContravariant
  trait AlgWithNonEffectMethod[T] {
    def foo2(a: String): String
    def foo3(a: T): String
  }

  object AlgWithNonEffectMethodFloat extends AlgWithNonEffectMethod[Float] {
    def foo2(a: String): String = a.reverse
    def foo3(a: Float): String = a.toInt.toString
  }

  @autoContravariant
  trait AlgWithExtraTypeParam[T1, T] {
    def foo(a: T1, b: T): Int
  }

  @autoContravariant
  trait AlgWithDefaultImpl[T] {
    def plusOne(i: T): Int
    def minusOne(i: Int): Int = i - 1
  }

  @autoContravariant
  trait AlgWithGenericType[T] {
    def foo[A](i: T, a: A): Int
  }

  @autoContravariant
  trait AlgWithCurry[T] {
    def foo(a: T)(b: Int): Int
  }

  @autoContravariant
  trait AlgWithCurry2[T] {
    def foo(a: T)(b: Int): String
  }

  @autoContravariant
  trait AlgWithVarArgsParameter[T] {
    def sum(xs: Int*): Int
    def showAll(ts: T*): Int
  }

  object AlgWithExtraTypeParamFloat extends AlgWithExtraTypeParam[String, Float] {
    def foo(a: String, b: Float): Int = (a.length.toFloat + b).toInt
  }


  import cats.instances.string._
  implicit def eqForSimpleAlg[T: ExhaustiveCheck: Eq]: Eq[SimpleAlg[T]] =
    Eq.by[SimpleAlg[T], T => String] { p =>
      (s: T) => p.foo(s)
    }

  implicit def arbitrarySimpleAlg[T: Cogen]: Arbitrary[SimpleAlg[T]] =
    Arbitrary {
      for {
        f <- Arbitrary.arbitrary[T => String]
      } yield new SimpleAlg[T] {
        def foo(i: T): String = f(i)
      }
    }
}
