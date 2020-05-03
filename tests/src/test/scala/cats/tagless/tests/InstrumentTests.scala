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

import cats.{Id, Show}
import cats.tagless.{Derive, Trivial, autoInstrument, finalAlg}
import cats.tagless.diagnosis.Instrument
import InstrumentTests._

class InstrumentTests extends CatsTaglessTestSuite {

  test("Instrument should put method and algebra name in result") {
    val dummy = new KVStore[Id] {
      def get(key: String): Id[Option[String]] = Some(s"Test $key")
      def put(key: String, a: String): Id[Unit] = ()
    }

    val instrumented = Derive.instrument[KVStore].instrument(dummy)
    val res = instrumented.get("key1")

    res.algebraName shouldBe "KVStore"
    res.methodName shouldBe "get"
    res.value shouldBe Some("Test key1")
  }

  test("Instrument.With should put method and algebra name in result") {
    val dummy = new KVStore[Id] {
      def get(key: String): Id[Option[String]] = Some(s"Test $key")
      def put(key: String, a: String): Id[Unit] = ()
    }

    implicit val instrumentWithShow: Instrument.WithBoth[KVStore, Show] = Derive.instrumentWith
    val instrumented = dummy.instrumentWithBoth[Show]
    val res = instrumented.get("key1")

    res.instrumentation.algebraName shouldBe "KVStore"
    res.instrumentation.methodName shouldBe "get"
    res.instrumentation.value shouldBe Some("Test key1")
    res.arguments.map(_.map(_.value)) shouldBe List(List("key1"))
    res.arguments.map(_.map(_.name)) shouldBe List(List("key"))
    res.instance.show(res.instrumentation.value) shouldBe "Some(Test key1)"
  }

  test("autoInstrument annotation") {
    val dummy = new Lookup[Id] {
      def ->(id: String): Id[Option[Long]] = Some(1)
    }

    val instrumented = dummy.instrument
    val res = instrumented.->("key1")

    res.algebraName shouldBe "Lookup"
    res.methodName shouldBe "->"
    res.value shouldBe Some(1)
  }
}

object InstrumentTests {
  @autoInstrument
  @finalAlg
  trait Lookup[F[_]] {
    def ->(id: String): F[Option[Long]]
  }

  trait ShowFAlgebra[F[_]] {
    def showF[A: Show](a: A): F[String]
    def showAll[A: Show](as: A*): F[String]
    def showProduct[A: Show](a: A): F[(A, String)]
    def logF[A: Show](message: => A): F[Unit]
  }

  object ShowFAlgebra {
    import cats.instances.all._
    implicit val instrumentWithShow: Instrument.WithBoth[ShowFAlgebra, Show] = Derive.instrumentWith
    implicit val trivialInstrument: Instrument.WithBoth[ShowFAlgebra, Trivial] = Derive.instrumentWith
  }
}