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

package cats.tagless.example

import cats.tagless.*
import cats.~>

import scala.util.Try

object VerticalCompositionExample {
  trait StringCalculatorAlg[F[_]] {
    def calc(i: String): F[Float]
  }

  @finalAlg
  @autoFunctorK
  @autoSemigroupalK
  @autoProductNK
  trait ExpressionAlg[F[_]] {
    def num(i: String): F[Float]

    def divide(dividend: Float, divisor: Float): F[Float]
  }

  import ExpressionAlg.autoDerive.*
  implicit object tryExpression extends ExpressionAlg[Try] {
    def num(i: String) = Try(i.toFloat)
    def divide(dividend: Float, divisor: Float) = Try(dividend / divisor)
  }

  implicit val fk: Try ~> Option = λ[Try ~> Option](_.toOption)

  class StringCalculatorOption(implicit exp: ExpressionAlg[Option]) extends StringCalculatorAlg[Option] {
    def calc(i: String): Option[Float] = {
      val numbers = i.split("/")
      for {
        s1 <- numbers.headOption
        f1 <- exp.num(s1)
        s2 <- numbers.lift(1)
        f2 <- exp.num(s2)
        r <- exp.divide(f1, f2)
      } yield r
    }
  }
  // ExpressionAlg[Option] instance is available here
  new StringCalculatorOption

}
