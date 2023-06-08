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

import cats.{Monad, ~>}
import cats.implicits.*
import cats.tagless.{autoFunctorK, finalAlg}
import cats.free.Free
import cats.arrow.FunctionK

import scala.util.Try

object FreeExample extends App {

  @finalAlg
  @autoFunctorK
  trait Increment[F[_]] {
    def plusOne(i: Int): F[Int]
  }

  implicit object incTry extends Increment[Try] {
    def plusOne(i: Int) = Try(i + 1)
  }

  def program[F[_]: Monad: Increment](i: Int): F[Int] = for {
    j <- Increment[F].plusOne(i)
    z <- if (j < 10000) program[F](j) else Monad[F].pure(j)
  } yield z

  import Increment.autoDerive.*

  implicit def toFree[F[_]]: F ~> Free[F, *] = λ[F ~> Free[F, *]](t => Free.liftF(t))

  println(program[Free[Try, *]](0).foldMap(FunctionK.id))

}
