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
package tests.simple

import cats.Id
import cats.data.Cokleisli

object Fixtures extends Fixtures
trait Fixtures {

  /** Simple algebra definition */
  trait SimpleService[F[_]] {
    def id(): F[Int]
    def list(id: Int): F[List[Int]]
    def lists(id1: Int, id2: Int): F[List[Int]]
    def paranthesless: F[Int]
    def tuple: F[(Int, Long)]
  }

  object SimpleService {
    implicit val functorK: FunctorK[SimpleService] = Derive.functorK
    implicit val invariantK: InvariantK[SimpleService] = Derive.invariantK
    implicit val semigroupalK: SemigroupalK[SimpleService] = Derive.semigroupalK
    implicit val applyK: ApplyK[SimpleService] = Derive.applyK
  }

  object instance extends SimpleService[Id] {
    def id(): Id[Int] = 42
    def list(id: Int): Id[List[Int]] = List(id)
    def lists(id1: Int, id2: Int): Id[List[Int]] = List(id1, id2)
    def paranthesless: Id[Int] = 23
    def tuple: Id[(Int, Long)] = (42, 23)
  }

  trait NotSimpleService[F[_]] {
    def id(): Int
    def list(id: Int): F[List[Int]]
  }

  object instancens extends NotSimpleService[Id] {
    def id(): Int = 42
    def list(id: Int): Id[List[Int]] = List(1, 2, 3)
  }

  trait SimpleServiceC[F[_]] {
    def id(id: F[Int]): Int
    def ids(id1: F[Int], id2: F[Int]): Int
    def foldSpecialized(init: String)(f: (Int, String) => Int): Cokleisli[F, String, Int]
  }

  object SimpleServiceC {
    implicit val contravariantK: ContravariantK[SimpleServiceC] = Derive.contravariantK
  }

  object instancec extends SimpleServiceC[Id] {
    def id(id: Id[Int]): Int = id
    def ids(id1: Id[Int], id2: Id[Int]): Int = id1 + id2
    def foldSpecialized(init: String)(f: (Int, String) => Int): Cokleisli[Id, String, Int] =
      Cokleisli.apply((str: Id[String]) => f(init.toInt, str))
  }
}
