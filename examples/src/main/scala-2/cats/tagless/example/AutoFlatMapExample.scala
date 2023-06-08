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

import cats.tagless.autoFlatMap
import cats.tagless.*
import cats.implicits.*

object AutoFlatMapExample extends App {

  @autoFlatMap
  trait StringAlg[T] {
    def foo(a: String): T
  }

  object LengthAlg extends StringAlg[Int] {
    def foo(a: String): Int = a.length
  }

  object HeadAlg extends StringAlg[Char] {
    def foo(a: String): Char = a.headOption.getOrElse(' ')
  }

  // flatMap is generated automatically for all StringAlg instance:
  val hintAlg: StringAlg[String] = for {
    length <- LengthAlg
    head <- HeadAlg
  } yield head.toString ++ "*" * (length - 1)

  println(hintAlg.foo("Password"))

}
