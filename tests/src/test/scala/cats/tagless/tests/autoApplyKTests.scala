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
import cats.data.EitherT
import cats.instances.all._
import cats.laws.discipline.SerializableTests
import cats.laws.discipline.arbitrary._
import cats.laws.discipline.eq._
import cats.tagless.laws.discipline.ApplyKTests
import cats.tagless.tests.autoApplyKTests.AutoApplyKTestAlg
import org.scalacheck.Arbitrary

import scala.util.Try

class autoApplyKTests extends CatsTaglessTestSuite {
  // Type inference limitation.
  implicit val eqTuple3K = AutoApplyKTestAlg.eqForAutoApplyKTestAlg[Tuple3K[Try, Option, List]#λ]
  checkAll("ApplyK[AutoApplyKTestAlg]", ApplyKTests[AutoApplyKTestAlg].applyK[Try, Option, List, Int])
  checkAll("ApplyK is Serializable", SerializableTests.serializable(ApplyK[AutoApplyKTestAlg]))
}

object autoApplyKTests {

  @autoApplyK(autoDerivation = false)
  trait AutoApplyKTestAlg[F[_]] {
    def parseInt(str: String): F[Int]
    def parseDouble(str: String): EitherT[F, String, Double]
    def divide(dividend: Float, divisor: Float): F[Float]
  }

  object AutoApplyKTestAlg {
    import TestInstances._

    implicit def eqForAutoApplyKTestAlg[F[_]](
      implicit eqFi: Eq[F[Int]], eqFf: Eq[F[Float]], eqEfd: Eq[EitherT[F, String, Double]]
    ): Eq[AutoApplyKTestAlg[F]] = Eq.by {
      algebra => (algebra.parseInt _, algebra.parseDouble _, algebra.divide _)
    }

    implicit def arbitraryAutoApplyKTestAlg[F[_]](
      implicit arbFi: Arbitrary[F[Int]], arbFf: Arbitrary[F[Float]], arbEfd: Arbitrary[EitherT[F, String, Double]]
    ): Arbitrary[AutoApplyKTestAlg[F]] = Arbitrary {
      for {
        pInt <- Arbitrary.arbitrary[String => F[Int]]
        pDouble <- Arbitrary.arbitrary[String => EitherT[F, String, Double]]
        div <- Arbitrary.arbitrary[(Float, Float) => F[Float]]
      } yield new AutoApplyKTestAlg[F] {
        def parseInt(str: String) = pInt(str)
        def parseDouble(str: String) = pDouble(str)
        def divide(dividend: Float, divisor: Float) = div(dividend, divisor)
      }
    }
  }

  @autoApplyK
  trait AlgWithVarArgsParameter[F[_]] {
    def sum(xs: Int*): Int
    def fSum(xs: Int*): F[Int]
  }
}
