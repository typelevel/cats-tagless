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

import scala.util.{Success, Try}
import FinalAlgTests._

class FinalAlgTests extends CatsTaglessTestSuite {
  test("companion apply") {
    import Interpreters.tryInterpreter
    SafeAlg[Try].parseInt("10") should be(Success(10))
  }

  test("extra TP") {
    AlgWithExtraTP[Try, String].a(3) should be(Success("3"))
  }

  test("simple effect Type") {
    AlgWithSimpleEffectT[String].a(3) should be("3")
  }

}

object FinalAlgTests {

  @finalAlg
  trait AlgWithExtraTP[F[_], T] {
    def a(i: Int): F[T]
  }

  @finalAlg
  trait AlgWithSimpleEffectT[T] {
    def a(i: Int): T
  }

  implicit val algWithExtraTP: AlgWithExtraTP[Try, String] = new AlgWithExtraTP[Try, String] {
    def a(i: Int) = Try(i.toString)
  }

  implicit val algWithSimpleEffectT: AlgWithSimpleEffectT[String] = new AlgWithSimpleEffectT[String] {
    def a(i: Int) = i.toString
  }
}
