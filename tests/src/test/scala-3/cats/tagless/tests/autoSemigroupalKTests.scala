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

import cats.laws.discipline.SerializableTests
import cats.laws.discipline.arbitrary.*
import cats.tagless.SemigroupalK
import cats.tagless.laws.discipline.SemigroupalKTests

import scala.util.Try

class autoSemigroupalKTests extends CatsTaglessTestSuite:
  checkAll("SemigroupalK[SafeAlg]", SemigroupalKTests[SafeAlg].semigroupalK[Try, Option, List])
  checkAll("SemigroupalK[SafeInvAlg]", SemigroupalKTests[SafeInvAlg].semigroupalK[Try, Option, List])
  checkAll("SemigroupalK[CalculatorAlg]", SemigroupalKTests[CalculatorAlg].semigroupalK[Try, Option, List])
  checkAll("SemigroupalK is Serializable", SerializableTests.serializable(SemigroupalK[SafeAlg]))

  test("simple product"):
    val prodInterpreter = Interpreters.tryInterpreter.productK(Interpreters.lazyInterpreter)
    assertEquals(prodInterpreter.parseInt("3").first, Try(3))
    assertEquals(prodInterpreter.parseInt("3").second.value, 3)
    assertEquals(prodInterpreter.parseInt("sd").first.isSuccess, false)
    assertEquals(prodInterpreter.divide(3f, 3f).second.value, 1f)

object autoSemigroupalKTests:

  trait algWithGenericType[F[_]] derives SemigroupalK:
    def a[T](a: T): F[Unit]

  trait algWithCurryMethod[F[_]] derives SemigroupalK:
    def a(b: String)(d: Int): F[Unit]

  trait AlgWithVarArgsParameter[F[_]] derives SemigroupalK:
    def sum(xs: Int*): Int
    def effectfulSum(xs: Int*): F[Int]

  trait AlgWithConstantReturnTypes[F[_]] derives SemigroupalK:
    def defer[A](x: => A): F[A]
    def unsafeRun[A](t: F[A]): A
    def unsafeRunAll[A](t: F[A]*): Seq[A]
    def toError(t: F[Int]): Exception
