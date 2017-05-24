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

import cats.Eval
import cats.kernel.Eq
import org.scalacheck.{Arbitrary, Cogen}

import scala.util.Try
import cats.laws.discipline.eq._

@finalAlg @autoFunctorK @autoCartesianK @autoProductNK
trait SafeAlg[F[_]] {
  def parseInt(i: String): F[Int]
  def divide(dividend: Float, divisor: Float): F[Float]
}

object SafeAlg {

  implicit def eqForParseAlg[F[_]](implicit eqF: Eq[F[Int]]): Eq[SafeAlg[F]] =
    Eq.by[SafeAlg[F], String => F[Int]](p => (s: String) => p.parseInt(s))

  implicit def arbitraryParseAlg[F[_]](implicit cS: Cogen[String],
                                       gF: Cogen[Float],
                                       FI: Arbitrary[F[Int]],
                                       FB: Arbitrary[F[Float]]): Arbitrary[SafeAlg[F]] =
    Arbitrary {
      for {
        f1 <- Arbitrary.arbitrary[String => F[Int]]
        f2 <- Arbitrary.arbitrary[(Float, Float) => F[Float]]
      } yield new SafeAlg[F] {
        def parseInt(i: String): F[Int] = f1(i)
        def divide(dividend: Float, divisor: Float): F[Float] = f2(dividend, divisor)
      }
    }
}

object Interpreters {

  implicit object tryInterpreter extends SafeAlg[Try] {
    def parseInt(s: String) = Try(s.toInt)
    def divide(dividend: Float, divisor: Float): Try[Float] = Try(dividend / divisor)
  }

  implicit object lazyInterpreter extends SafeAlg[Eval] {
    def parseInt(s: String): Eval[Int] = Eval.later(s.toInt)
    def divide(dividend: Float, divisor: Float): Eval[Float] = Eval.later(dividend / divisor)
  }

}
