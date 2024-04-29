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

import cats.{Contravariant, Eq}
import cats.laws.discipline.{ContravariantTests, ExhaustiveCheck, SerializableTests}
import cats.laws.discipline.eq.*
import cats.tagless.derived.*
import org.scalacheck.{Arbitrary, Cogen}

@experimental
class autoContravariantTests extends CatsTaglessTestSuite:
  import autoContravariantTest.*

  checkAll("Contravariant[SimpleAlg]", ContravariantTests[SimpleAlg].contravariant[Float, String, Int])
  checkAll("Contravariant is Serializable", SerializableTests.serializable(Contravariant[SimpleAlg]))

  test("non effect method correctly delegated"):
    val doubleAlg: AlgWithNonEffectMethod[String] = AlgWithNonEffectMethodFloat.contramap[String](_.toFloat)
    assertEquals(doubleAlg.foo2("big"), "gib")
    assertEquals(doubleAlg.foo3("3"), "3")

  test("extra type param correctly handled"):
    val doubleAlg: AlgWithExtraTypeParam[String, String] = AlgWithExtraTypeParamFloat.contramap[String](_.toFloat)
    assertEquals(doubleAlg.foo("big", "3"), 6)

  test("Alg with non effect method with default Impl"):
    val intAlg = new AlgWithDefaultImpl[Int]:
      def plusOne(i: Int): Int = i + 1

    assertEquals(intAlg.contramap[String](_.toInt).plusOne("3"), 4)
    assertEquals(intAlg.contramap[String](_.toInt).minusOne(2), 1)

@experimental
object autoContravariantTest:

  trait SimpleAlg[T] derives Contravariant:
    def foo(t: T): String
    def bar(opt: Option[T]): String

  trait AlgWithNonEffectMethod[T] derives Contravariant:
    def foo2(a: String): String
    def foo3(a: T): String

  object AlgWithNonEffectMethodFloat extends AlgWithNonEffectMethod[Float]:
    def foo2(a: String): String = a.reverse
    def foo3(a: Float): String = a.toInt.toString

  trait AlgWithExtraTypeParam[T1, T] derives Contravariant:
    def foo(a: T1, b: T): Int

  trait AlgWithDefaultImpl[T] derives Contravariant:
    def plusOne(i: T): Int
    def minusOne(i: Int): Int = i - 1

  trait AlgWithGenericType[T] derives Contravariant:
    def foo[A](i: T, a: A): Int

  trait AlgWithCurry[T] derives Contravariant:
    def foo(a: T)(b: Int): Int

  trait AlgWithCurry2[T] derives Contravariant:
    def foo(a: T)(b: Int): String

  trait AlgWithVarArgsParameter[T] derives Contravariant:
    def sum(xs: Int*): Int
    def showAll(ts: T*): Int

  object AlgWithExtraTypeParamFloat extends AlgWithExtraTypeParam[String, Float]:
    def foo(a: String, b: Float): Int = (a.length.toFloat + b).toInt

  given [T: ExhaustiveCheck]: Eq[SimpleAlg[T]] =
    Eq.by(algebra => (algebra.foo _, algebra.bar _))

  given [T: Cogen]: Arbitrary[SimpleAlg[T]] =
    Arbitrary(
      for
        f <- Arbitrary.arbitrary[T => String]
        g <- Arbitrary.arbitrary[Option[T] => String]
      yield new SimpleAlg[T]:
        def foo(t: T) = f(t)
        def bar(opt: Option[T]) = g(opt)
    )
