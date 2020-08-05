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

package cats.tagless.tests

import cats.Id
import cats.tagless.tests.InstrumentTests._
import cats.tagless.{Derive, autoInstrument, finalAlg}

class InstrumentTests extends CatsTaglessTestSuite {

  test("Instrument.algebraName and Instrument.instrument") {
    val store = new KVStore[Id] {
      def get(key: String): Id[Option[String]] = Some(s"Test $key")
      def put(key: String, a: String): Id[Unit] = ()
    }

    val instrument = Derive.instrument[KVStore]
    val instrumented = instrument.instrument(store)
    val result = instrumented.get("key1")

    result.algebraName shouldBe "KVStore"
    result.methodName shouldBe "get"
    result.value shouldBe Some("Test key1")
  }

  test("autoInstrument annotation") {
    val lookup = new Lookup[Id] {
      def ->(id: String): Id[Option[Long]] = Some(1)
    }

    val instrumented = lookup.instrument
    val result = instrumented -> "key1"

    result.algebraName shouldBe "Lookup"
    result.methodName shouldBe "->"
    result.value shouldBe Some(1)
  }
}

object InstrumentTests {
  @autoInstrument
  @finalAlg
  trait Lookup[F[_]] {
    def ->(id: String): F[Option[Long]]
  }
}