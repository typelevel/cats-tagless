/*
 * Copyright 2017 Kailuo Wang
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

package mainecoon
package tests


import scala.util.Try
import cats.~>
import cats.laws.discipline.SerializableTests
import mainecoon.laws.discipline.InvariantKTests
import autoInvariantKTests._

class autoInvariantKTests extends MainecoonTestSuite {
  //work with covariant algs
  checkAll("ParseAlg[Option]", InvariantKTests[SafeAlg].invariantK[Try, Option, List])
  checkAll("InvariantK[ParseAlg]", SerializableTests.serializable(InvariantK[SafeAlg]))

  test("Alg with non effect method") {
    val tryInt = new NoEffectMethod[Try] {
      def a(i: Int): Int = i
    }

    tryInt.imapK(toFk)(otFk).a(2) should be(2)
  }

  test("Alg with contravariant Eff method") {
    val tryInt = new ContravariantEff[Try] {
      def a(i: Try[Int], j: String): Int = i.get
      def b(i: Try[Int]): Int = i.get
    }

    val oInt = tryInt.imapK(toFk)(otFk)
    oInt.a(Option(2), "ignored") should be(2)
    oInt.b(Option(3)) should be(3)
  }

  test("Alg with Invariant effect method") {
    val tryInt = new InvariantEff[Try] {
      def a(i: Try[Int], j: String): Try[Int] = i
    }

    val oInt = tryInt.imapK(toFk)(otFk)
    oInt.a(Option(2), "ignored") should be(Some(2))
  }

  test("Alg with extra type parameters auto derivation") {
    implicit object algWithExtraTP extends WithExtraTpeParam[Try, String] {
      def a(i: Try[String]) = i
      def b(i: Try[Int]) = i.map(_.toString)
    }

    WithExtraTpeParam[Option, String].a(Some("5")) should be(Some("5"))
    WithExtraTpeParam[Option, String].b(Some(5)) should be(Some("5"))
  }

  test("Alg with extra type parameters before effect type") {
    implicit val algWithExtraTP: AlgWithExtraTP2[String, Try] = new AlgWithExtraTP2[String, Try] {
      def a(i: Try[Int]) = i.map(_.toString)
    }

    AlgWithExtraTP2[String, Option].a(Some(5)) should be(Some("5"))
  }

  test("Alg with type member") {
    implicit val tryInt = new AlgWithTypeMember[Try] {
      type T = String
      def a(i: Try[String]): Try[String] = i.map(_ + "a")
    }

    val algAux: AlgWithTypeMember.Aux[Option, String] = AlgWithTypeMember.imapK(tryInt)(toFk)(otFk)
    algAux.a(Some("4")) should be(Some("4a"))
  }

}


object autoInvariantKTests {
  implicit val toFk : Try ~> Option = λ[Try ~> Option](_.toOption)
  implicit val otFk : Option ~> Try = λ[Option ~> Try](o => Try(o.get))

  @autoInvariantK
  trait NoEffectMethod[F[_]] {
    def a(i: Int): Int
  }

  @autoInvariantK
  trait ContravariantEff[F[_]] {
    def a(i: F[Int], j: String): Int
    def b(i: F[Int]): Int
  }

  @autoInvariantK
  trait InvariantEff[F[_]] {
    def a(i: F[Int], j: String): F[Int]
  }

  @autoInvariantK @finalAlg
  trait AlgWithTypeMember[F[_]] {
    type T
    def a(i: F[T]): F[T]
  }

  object AlgWithTypeMember {
    type Aux[F[_], T0] = AlgWithTypeMember[F] { type T = T0 }
  }


  @autoInvariantK @finalAlg
  trait WithExtraTpeParam[F[_], T] {
    def a(i: F[T]): F[T]
    def b(i: F[Int]): F[T]
  }

  @autoInvariantK @finalAlg
  trait AlgWithExtraTP2[T, F[_]] {
    def a(i: F[Int]): F[T]
  }

}
