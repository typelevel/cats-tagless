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
import cats.tagless.syntax.all.*
import cats.tagless.tests.experimental

@experimental
class InvariantKSpec extends munit.FunSuite with Fixtures {
  test("DeriveMacro should derive instance for a simple algebra") {
    val invariantK = Derive.invariantK[SimpleService]
    assert(invariantK.isInstanceOf[InvariantK[SimpleService]])
  }

  test("InvariantK should be a valid instance for a simple algebra") {
    val invariantK = Derive.invariantK[SimpleService]
    val functorK = Derive.functorK[SimpleService]
    val fk = FunctionKLift[Id, Option](Option.apply)
    val gk = FunctionKLift[Option, Id](_.get)
    val invariantInstance = invariantK.imapK(instance)(fk)(gk)
    val optionalInstance = functorK.mapK(instance)(fk)

    assertEquals(invariantInstance.id(), optionalInstance.id())
    assertEquals(invariantInstance.list(0), optionalInstance.list(0))
    assertEquals(invariantInstance.lists(0, 1), optionalInstance.lists(0, 1))
    assertEquals(invariantInstance.paranthesless, optionalInstance.paranthesless)
    assertEquals(invariantInstance.tuple, optionalInstance.tuple)
  }

  test("DeriveMacro should derive instance for a not simple algebra") {
    assert(compileErrors("Derive.invariantK[NotSimpleService]").isEmpty)
  }

  test("InvariantK derives syntax") {
    val fk = FunctionKLift[Id, Option](Option.apply)
    val gk = FunctionKLift[Option, Id](_.get)
    val invariantInstance = instance.imapK(fk)(gk)
    val optionalInstance = instance.mapK(fk)

    assertEquals(invariantInstance.id(), optionalInstance.id())
    assertEquals(invariantInstance.list(0), optionalInstance.list(0))
    assertEquals(invariantInstance.lists(0, 1), optionalInstance.lists(0, 1))
    assertEquals(invariantInstance.paranthesless, optionalInstance.paranthesless)
    assertEquals(invariantInstance.tuple, optionalInstance.tuple)
  }
}
