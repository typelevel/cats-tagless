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
import cats.laws.discipline.SerializableTests
import cats.laws.discipline.arbitrary._
import cats.tagless.laws.discipline.SemigroupalKTests

import scala.util.Try

class autoSemigroupalKTests extends CatsTaglessTestSuite {
  type T3K[A] = Tuple3K[Try, Option, List]#λ[A]
  // Type inference issues on Scala 2.12
  implicit val eqForSafeAlg: Eq[SafeAlg[T3K]] = SafeAlg.eqForSafeAlg[T3K]
  implicit val eqForSafeInvAlg: Eq[SafeInvAlg[T3K]] = SafeInvAlg.eqForSafeInvAlg[T3K]
  implicit val eqForCalculatorAlg: Eq[CalculatorAlg[T3K]] = CalculatorAlg.eqForCalculatorAlg[T3K]

  checkAll("SemigroupalK[SafeAlg]", SemigroupalKTests[SafeAlg].semigroupalK[Try, Option, List])
  checkAll("SemigroupalK[SafeInvAlg]", SemigroupalKTests[SafeInvAlg].semigroupalK[Try, Option, List])
  checkAll("SemigroupalK[CalculatorAlg]", SemigroupalKTests[CalculatorAlg].semigroupalK[Try, Option, List])
  checkAll("SemigroupalK is Serializable", SerializableTests.serializable(SemigroupalK[SafeAlg]))

  test("simple product") {
    val prodInterpreter = Interpreters.tryInterpreter.productK(Interpreters.lazyInterpreter)
    assertEquals(prodInterpreter.parseInt("3").first, Try(3))
    assertEquals(prodInterpreter.parseInt("3").second.value, 3)
    assertEquals(prodInterpreter.parseInt("sd").first.isSuccess, false)
    assertEquals(prodInterpreter.divide(3f, 3f).second.value, 1f)
  }
}

object autoSemigroupalKTests {

  @autoSemigroupalK
  trait algWithGenericType[F[_]] {
    def a[T](a: T): F[Unit]
  }

  @autoSemigroupalK
  trait algWithCurryMethod[F[_]] {
    def a(b: String)(d: Int): F[Unit]
  }

  @autoSemigroupalK
  trait AlgWithVarArgsParameter[F[_]] {
    def sum(xs: Int*): Int
    def effectfulSum(xs: Int*): F[Int]
  }
}
