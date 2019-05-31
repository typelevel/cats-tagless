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


import cats.~>

import util.{Success, Try}

class autoProductNKTests extends CatsTaglessTestSuite {
  test("simple product") {
    val optionInterpreter = Interpreters.tryInterpreter.mapK(λ[Try ~> Option](_.toOption))
    val listInterpreter = optionInterpreter.mapK(λ[Option ~> List](_.toList))

    val prodInterpreter: SafeAlg[Tuple3K[Try, List, Option]#λ] =
      SafeAlg.product3K(Interpreters.tryInterpreter, listInterpreter, optionInterpreter)

    prodInterpreter.parseInt("3") should be((Success(3), List(3f), Some(3f)))
    val failure = prodInterpreter.parseInt("3.5")

    failure._1.isFailure should be(true)
    failure._2 should be(Nil)
    failure._3 should be(None)

    prodInterpreter.divide(3f, 3f) should be((Success(1f), List(1f), Some(1f)))

  }

}


object autoProductNKTests {

  @autoProductNK
  trait algWithGenericType[F[_]] {
    def a[T](a: T): F[Unit]
  }

  @autoProductNK
  trait algWithDifferentParameterLists[F[_]] {
    def parameterLess: F[Unit]
    def nullary(): F[Unit]
    def singular(a: String): F[Unit]
    def binary(a: String)(b: Int): F[Unit]
    def complex(a: String, b: Int)()(c: Long)(implicit d: Double, e: Char): F[Unit]
    def varArg(a: String, xs: Int*): F[Unit]
  }
}
