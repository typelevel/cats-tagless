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
import cats.data.Tuple2K
import cats.tagless.syntax.all.*
import cats.tagless.tests.experimental

import scala.util.Try

@experimental
class ApplyKSpec extends munit.FunSuite with Fixtures {
  test("DeriveMacro should derive instance for a simple algebra") {
    val applyK = Derive.applyK[SimpleService]
    assert(applyK.isInstanceOf[ApplyK[SimpleService]])
  }

  test("ApplyK should be a valid instance for a simple algebra") {
    val functorK = Derive.functorK[SimpleService]
    val applyK = Derive.applyK[SimpleService]
    val optionalInstance = functorK.mapK(instance)(FunctionKLift[Id, Option](Option.apply))
    val fk = FunctionKLift[Tuple2K[Id, Option, *], Try](tup => Try(tup.second.map(_ => tup.first).get))
    val tryInstance = applyK.map2K[Id, Option, Try](instance, optionalInstance)(fk)

    assertEquals(tryInstance.id(), Try(instance.id()))
    assertEquals(tryInstance.list(0), Try(instance.list(0)))
    assertEquals(tryInstance.paranthesless, Try(instance.paranthesless))
    assertEquals(tryInstance.tuple, Try(instance.tuple))
  }

  test("DeriveMacro should derive instance for a not simple algebra") {
    assert(compileErrors("Derive.applyK[NotSimpleService]").isEmpty)
  }

  test("ApplyK derives syntax") {
    val optionalInstance = instance.mapK(FunctionKLift[Id, Option](Option.apply))
    val fk = FunctionKLift[Tuple2K[Id, Option, *], Try](tup => Try(tup.second.map(_ => tup.first).get))
    val tryInstance = instance.map2K(optionalInstance)(fk)

    assertEquals(tryInstance.id(), Try(instance.id()))
    assertEquals(tryInstance.list(0), Try(instance.list(0)))
    assertEquals(tryInstance.paranthesless, Try(instance.paranthesless))
    assertEquals(tryInstance.tuple, Try(instance.tuple))
  }
}
