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

import cats.laws.discipline.SerializableTests
import cats.tagless.laws.discipline.SemigroupalKTests

import scala.util.Try

class autoSemigroupalKTests extends CatsTaglessTestSuite {
  // Type inference issue.
  implicit val eqForSafeAlg = SafeAlg.eqForSafeAlg[Tuple3K[Try, Option, List]#λ]

  checkAll("ParseAlg[Option]", SemigroupalKTests[SafeAlg].semigroupalK[Try, Option, List])
  checkAll("SemigroupalK[ParseAlg]", SerializableTests.serializable(SemigroupalK[SafeAlg]))

  test("simple product") {
    val prodInterpreter = Interpreters.tryInterpreter.productK(Interpreters.lazyInterpreter)
    prodInterpreter.parseInt("3").first shouldBe Try(3)
    prodInterpreter.parseInt("3").second.value shouldBe 3
    prodInterpreter.parseInt("sd").first.isSuccess shouldBe false
    prodInterpreter.divide(3f, 3f).second.value shouldBe 1f
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

