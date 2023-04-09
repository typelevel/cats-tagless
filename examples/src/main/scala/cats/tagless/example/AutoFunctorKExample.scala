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
import scala.util.Try
import cats.tagless.implicits.*
import cats.implicits.*
import cats.*

object AutoFunctorKExample extends App {
  @autoFunctorK
  trait ExpressionAlg[F[_]] {
    def num(i: String): F[Float]

    def divide(dividend: Float, divisor: Float): F[Float]
  }

  implicit object tryExpression extends ExpressionAlg[Try] {
    def num(i: String) = Try(i.toFloat)

    def divide(dividend: Float, divisor: Float) = Try(dividend / divisor)
  }

  implicit val fk: Try ~> Option = λ[Try ~> Option](_.toOption)

  // mapK is called on auto generated functor instance:
  tryExpression.mapK(fk)

  // given implicit instances tryExpression and Try ~> Option we obtain ExpressionAlg[Option] instance for free:
  import ExpressionAlg.autoDerive.*
  implicitly[ExpressionAlg[Option]]

}
