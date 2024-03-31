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
import cats.tagless.syntax.all.*
import cats.tagless.macros.*

import cats.Id
import cats.arrow.FunctionK
import cats.data.Tuple2K
import scala.util.Try
import cats.~>

import scala.compiletime.testing.*
import scala.annotation.experimental

@experimental
class ApplyKSpec extends munit.FunSuite with Fixtures:
  test("DeriveMacro should derive instance for a simple algebra") {
    val applyK = Derive.applyK[SimpleService]
    assert(applyK.isInstanceOf[ApplyK[SimpleService]])
  }

  test("ApplyK should be a valid instance for a simple algebra") {
    val functorK = Derive.functorK[SimpleService]
    val applyK = Derive.applyK[SimpleService]
    val optionalInstance = functorK.mapK(instance)(FunctionK.lift([X] => (id: Id[X]) => Option(id)))

    val fk: Tuple2K[Id, Option, *] ~> Try =
      FunctionK.lift([X] => (tup: Tuple2K[Id, Option, X]) => Try(tup.second.map(_ => tup.first).get))
    val tryInstance = applyK.map2K[Id, Option, Try](instance, optionalInstance)(fk)

    assertEquals(tryInstance.id(), Try(instance.id()))
    assertEquals(tryInstance.list(0), Try(instance.list(0)))
    assertEquals(tryInstance.paranthesless, Try(instance.paranthesless))
    assertEquals(tryInstance.tuple, Try(instance.tuple))
  }

  test("DeriveMacro should not derive instance for a not simple algebra") {
    assert(typeCheckErrors("Derive.applyK[NotSimpleService]").isEmpty)
  }

  test("ApplyK derives syntax") {
    val optionalInstance = instance.mapK(FunctionK.lift([X] => (id: Id[X]) => Option(id)))

    val fk: Tuple2K[Id, Option, *] ~> Try =
      FunctionK.lift([X] => (tup: Tuple2K[Id, Option, X]) => Try(tup.second.map(_ => tup.first).get))
    val tryInstance = instance.map2K(optionalInstance)(fk)

    assertEquals(tryInstance.id(), Try(instance.id()))
    assertEquals(tryInstance.list(0), Try(instance.list(0)))
    assertEquals(tryInstance.paranthesless, Try(instance.paranthesless))
    assertEquals(tryInstance.tuple, Try(instance.tuple))
  }
