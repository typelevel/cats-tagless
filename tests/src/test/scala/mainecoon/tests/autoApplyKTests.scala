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


import cats.kernel.Eq
import cats.laws.discipline.SerializableTests
import mainecoon.laws.discipline.ApplyKTests
import mainecoon.tests.autoApplyKTests.AutoApplyKTestAlg

import util.Try

class autoApplyKTests extends MainecoonTestSuite {

  implicit def eqT32 = AutoApplyKTestAlg.eqForAutoApplyKTestAlg[Tuple3K[Try, Option, List]#λ] //have to help scalac here

  checkAll("AutoApplyKTestAlg[Option]", ApplyKTests[AutoApplyKTestAlg].applyK[Try, Option, List, Int])
  checkAll("ApplyK[ParseAlg]", SerializableTests.serializable(ApplyK[AutoApplyKTestAlg]))

}

object autoApplyKTests {
  import org.scalacheck.{Arbitrary, Cogen}

  import cats.laws.discipline.eq._
  import cats.implicits._

  @autoApplyK(autoDerivation = false)
  trait AutoApplyKTestAlg[F[_]] {
    def parseInt(i: String): F[Int]
    def divide(dividend: Float, divisor: Float): F[Float]
  }

  object AutoApplyKTestAlg {

    implicit def eqForAutoApplyKTestAlg[F[_]](implicit eqFInt: Eq[F[Int]], eqFFloat: Eq[F[Float]]): Eq[AutoApplyKTestAlg[F]] =
      Eq.by[AutoApplyKTestAlg[F], (String => F[Int], (((Float, Float)) => F[Float]))] { p => (
        (s: String) => p.parseInt(s),
        (pair: (Float, Float)) => p.divide(pair._1, pair._2))
      }

    implicit def arbitraryAutoApplyKTestAlg[F[_]](implicit cS: Cogen[String],
                                        gF: Cogen[Float],
                                        FI: Arbitrary[F[Int]],
                                        FB: Arbitrary[F[Float]]): Arbitrary[AutoApplyKTestAlg[F]] =
      Arbitrary {
        for {
          f1 <- Arbitrary.arbitrary[String => F[Int]]
          f2 <- Arbitrary.arbitrary[(Float, Float) => F[Float]]
        } yield new AutoApplyKTestAlg[F] {
          def parseInt(i: String): F[Int] = f1(i)
          def divide(dividend: Float, divisor: Float): F[Float] = f2(dividend, divisor)
        }
      }
  }

}

