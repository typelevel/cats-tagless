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

package cats.tagless.simple

import cats.tagless.*
import cats.Id
import scala.annotation.experimental

@experimental
trait Fixtures:
  /** Simple algebra definition */
  trait SimpleService[F[_]] derives FunctorK:
    def id(): F[Int]
    def list(id: Int): F[List[Int]]
    def lists(id1: Int, id2: Int): F[List[Int]]
    def paranthesless: F[Int]
    def tuple: F[(Int, Long)]

  def instance = new SimpleService[Id]:
    def id(): Id[Int] = 42
    def list(id: Int): Id[List[Int]] = List(id)
    def lists(id1: Int, id2: Int): Id[List[Int]] = List(id1, id2)
    def paranthesless: Id[Int] = 23
    def tuple: Id[(Int, Long)] = (42, 23)

  trait NotSimpleService[F[_]]:
    def id(): Int
    def list(id: Int): F[List[Int]]

object Fixtures extends Fixtures
