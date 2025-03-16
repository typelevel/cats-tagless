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

import cats.{Eq, Invariant}
import cats.laws.discipline.{InvariantTests, MiniInt, SerializableTests}
import cats.laws.discipline.arbitrary.*
import cats.laws.discipline.eq.*
import cats.tagless.derived.*
import org.scalacheck.{Arbitrary, Cogen}

@experimental
class autoInvariantTests extends CatsTaglessTestSuite:
  import autoInvariantTests.*

  checkAll("SimpleAlg[Option]", InvariantTests[SimpleAlg].invariant[Float, MiniInt, Int])
  checkAll("Invariant[SimpleAlg]", SerializableTests.serializable(Invariant[SimpleAlg]))

  test("non effect method correctly delegated"):
    val doubleAlg = AlgWithNonEffectMethodFloat.imap(_.toDouble)(_.toFloat)
    assertEquals(doubleAlg.foo("big"), 3d)
    assertEquals(doubleAlg.foo2("big"), "gib")
    assertEquals(doubleAlg.foo3(3d), "3")

  test("extra type param correctly handled"):
    val doubleAlg = AlgWithExtraTypeParamFloat.imap(_.toDouble)(_.toFloat)
    assertEquals(doubleAlg.foo("big", 3d), 6d)

  test("Alg with non effect method with default Impl"):
    val intAlg = new AlgWithDefaultImpl[Int]:
      def plusOne(i: Int): Int = i + 1

    assertEquals(intAlg.imap(_.toString)(_.toInt).plusOne("3"), "4")
    assertEquals(intAlg.imap(_.toString)(_.toInt).minusOne(2), 1)

object autoInvariantTests:
  import TestInstances.*

  trait SimpleAlg[T] derives Invariant:
    def foo(a: T): T
    def headOption(ts: List[T]): Option[T]
    def map[U](ts: List[T])(f: T => U): List[U] = ts.map(f)

  trait AlgWithNonEffectMethod[T] derives Invariant:
    def foo(a: String): T
    def foo2(a: String): String
    def foo3(a: T): String

  object AlgWithNonEffectMethodFloat extends AlgWithNonEffectMethod[Float]:
    def foo(a: String): Float = a.length.toFloat
    def foo2(a: String): String = a.reverse
    def foo3(a: Float): String = a.toInt.toString

  trait AlgWithDef[T] derives Invariant:
    def foo: T
    def bar(t: T): T

  trait AlgWithExtraTypeParam[T1, T] derives Invariant:
    def foo(a: T1, b: T): T

  trait AlgWithDefaultImpl[T] derives Invariant:
    def plusOne(i: T): T
    def minusOne(i: Int): Int = i - 1

  trait AlgWithGenericType[T] derives Invariant:
    def foo[A](i: T, a: A): T

  trait AlgWithCurry[T] derives Invariant:
    def foo(a: T)(b: Int): T

  trait AlgWithCurry2[T] derives Invariant:
    def foo(a: T)(b: Int): String

  object AlgWithExtraTypeParamFloat extends AlgWithExtraTypeParam[String, Float]:
    def foo(a: String, b: Float): Float = a.length.toFloat + b

  given [T: Arbitrary: Cogen: Eq]: Eq[SimpleAlg[T]] =
    Eq.by(algebra => (algebra.foo, algebra.headOption, algebra.map[Int]))

  given [T: Arbitrary: Cogen]: Arbitrary[SimpleAlg[T]] =
    Arbitrary(for
      f <- Arbitrary.arbitrary[T => T]
      hOpt <- Arbitrary.arbitrary[List[T] => Option[T]]
    yield new SimpleAlg[T]:
      def foo(i: T): T = f(i)
      def headOption(list: List[T]) = hOpt(list))

  trait AlgWithVarArgsParameter[T] derives Invariant:
    def sum(xs: Int*): Int
    def covariantSum(xs: Int*): T
    def contravariantSum(xs: T*): Int
    def invariantSum(xs: T*): T
