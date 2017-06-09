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

package mainecoon
package tests


import cats.laws.discipline.SerializableTests
import cats.laws.discipline.FunctorTests
import autoFunctorTests._
import cats.Functor
import cats.kernel.Eq
import org.scalacheck.{Arbitrary, Cogen}
import cats.laws.discipline.eq._

class autoFunctorTests extends MainecoonTestSuite {

  checkAll("SimpleAlg[Option]", FunctorTests[SimpleAlg].functor[Float, String, Int])
  checkAll("Functor[SimpleAlg]", SerializableTests.serializable(Functor[SimpleAlg]))

  test("non effect method correctly delegated") {
    val doubleAlg = AlgWithNonEffectMethodFloat.map(_.toDouble)
    doubleAlg.bar("big") should be("big")
    doubleAlg.foo("big") should be(3d)
  }

  test("extra type param correctly handled") {
    val doubleAlg = AlgWithExtraTypeParamFloat.map(_.toDouble)
    doubleAlg.foo("big") should be(3d)
  }

  test("Alg with non effect method with default Impl") {
    val tryInt = new AlgWithDefaultImpl[Int] {
      def plusOne(i: Int): Int = i + 1
    }

    tryInt.map(_.toString).plusOne(3) should be("4")
    tryInt.map(_.toString).minusOne(2) should be(1)
  }
}

object autoFunctorTests {
  @autoFunctor
  trait SimpleAlg[T] {
    def foo(a: String): T
  }

  @autoFunctor
  trait AlgWithNonEffectMethod[T] {
    def foo(a: String): T
    def bar(a: String): String
  }

  @autoFunctor
  trait AlgWithDef[T] {
    def foo: T
  }


  object AlgWithNonEffectMethodFloat extends AlgWithNonEffectMethod[Float] {
    def foo(a: String): Float = a.length.toFloat
    def bar(a: String): String = a
  }

  @autoFunctor
  trait AlgWithExtraTypeParam[T1, T] {
    def foo(a: T1): T
  }

  object AlgWithExtraTypeParamFloat extends AlgWithExtraTypeParam[String, Float] {
    def foo(a: String): Float = a.length.toFloat
  }

  @autoFunctor
  trait AlgWithDefaultImpl[T] {
    def plusOne(i: Int): T
    def minusOne(i: Int): Int = i - 1
  }

  implicit def eqForSimpleAlg[T](implicit eqT: Eq[T]): Eq[SimpleAlg[T]] =
    Eq.by[SimpleAlg[T], String => T] { p =>
      (s: String) => p.foo(s)
    }

  implicit def arbitrarySimpleAlg[T](implicit cS: Cogen[String],
                                      FI: Arbitrary[T]): Arbitrary[SimpleAlg[T]] =
    Arbitrary {
      for {
        f <- Arbitrary.arbitrary[String =>T]
      } yield new SimpleAlg[T] {
        def foo(i: String): T = f(i)
      }
    }
}
