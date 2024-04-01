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
import cats.~>

import scala.compiletime.testing.*
import scala.annotation.experimental

@experimental
class ContravariantKSpec extends munit.FunSuite with Fixtures:

  test("DeriveMacro should derive instance for a simple algebra") {
    def contravariantK = Derive.contravariantK[SimpleServiceC]
    assert(contravariantK.isInstanceOf[ContravariantK[SimpleServiceC]])
  }

  test("DeriveMacro should derive instance for a simple algebra #2") {
    def contravariantK = Derive.contravariantK[SimpleServiceC]
    val fk: Option ~> Id = FunctionK.lift[Option, Id]([X] => (id: Option[X]) => id.get)
    val optionalInstance = contravariantK.contramapK(instancec)(fk)

    val f: (Int, String) => Int = (i, s) => i + s.toInt

    assertEquals(optionalInstance.id(Some(23)), instancec.id(23))
    assertEquals(optionalInstance.ids(Some(0), Some(1)), instancec.ids(0, 1))
    assertEquals(optionalInstance.foldSpecialized("0")(f).run(Some("1")), instancec.foldSpecialized("0")(f).run("1"))
  }

  test("DeriveMacro should not derive instance for not a contravariant algebra") {
    assert(typeCheckErrors("Derive.contravariantK[SimpleService]").nonEmpty)
  }

  test("ContravariantK derives syntax") {
    val fk: Option ~> Id = FunctionK.lift[Option, Id]([X] => (id: Option[X]) => id.get)
    val optionalInstance = instancec.contramapK(fk)

    val f: (Int, String) => Int = (i, s) => i + s.toInt

    assertEquals(optionalInstance.id(Some(23)), instancec.id(23))
    assertEquals(optionalInstance.ids(Some(0), Some(1)), instancec.ids(0, 1))
    assertEquals(optionalInstance.foldSpecialized("0")(f).run(Some("1")), instancec.foldSpecialized("0")(f).run("1"))
  }
