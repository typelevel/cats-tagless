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

@experimental
class SemigroupalKSpec extends munit.FunSuite with Fixtures {
  test("DeriveMacro should derive instance for a simple algebra") {
    val semigroupalK = Derive.semigroupalK[SimpleService]
    assert(semigroupalK.isInstanceOf[SemigroupalK[SimpleService]])
  }

  test("SemigroupalK should be a valid instance for a simple algebra") {
    val functorK = Derive.functorK[SimpleService]
    val semigroupalK = Derive.semigroupalK[SimpleService]
    val optionalInstance = functorK.mapK(instance)(FunctionKLift[Id, Option](Option.apply))
    val combinedInstance = semigroupalK.productK(instance, optionalInstance)

    assertEquals(combinedInstance.id(), Tuple2K(instance.id(), optionalInstance.id()))
    assertEquals(combinedInstance.list(0), Tuple2K(instance.list(0), optionalInstance.list(0)))
    assertEquals(combinedInstance.lists(0, 1), Tuple2K(instance.lists(0, 1), optionalInstance.lists(0, 1)))
    assertEquals(combinedInstance.paranthesless, Tuple2K(instance.paranthesless, optionalInstance.paranthesless))
    assertEquals(combinedInstance.tuple, Tuple2K(instance.tuple, optionalInstance.tuple))
  }

  test("DeriveMacro should derive instance for a not simple algebra") {
    val semigroupalK = Derive.semigroupalK[NotSimpleService]
    val combinedInstance = semigroupalK.productK(instancens, instancens)

    assertEquals(combinedInstance.id(), 2 * instancens.id())
  }

  test("SemigroupalK derives syntax") {
    val optionalInstance = instance.mapK(FunctionKLift[Id, Option](Option.apply))
    val combinedInstance = instance.productK(optionalInstance)

    assertEquals(combinedInstance.id(), Tuple2K(instance.id(), optionalInstance.id()))
    assertEquals(combinedInstance.list(0), Tuple2K(instance.list(0), optionalInstance.list(0)))
    assertEquals(combinedInstance.lists(0, 1), Tuple2K(instance.lists(0, 1), optionalInstance.lists(0, 1)))
    assertEquals(combinedInstance.paranthesless, Tuple2K(instance.paranthesless, optionalInstance.paranthesless))
    assertEquals(combinedInstance.tuple, Tuple2K(instance.tuple, optionalInstance.tuple))
  }
}
