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
package tests

import cats.~>
import cats.laws.discipline.SerializableTests
import cats.laws.discipline.arbitrary._
import cats.tagless.laws.discipline.InvariantKTests
import shapeless.test.illTyped

import scala.util.Try

class autoInvariantKTests extends CatsTaglessTestSuite {
  import autoInvariantKTests._

  checkAll("SafeInvAlg[Option]", InvariantKTests[SafeInvAlg].invariantK[Try, Option, List])
  checkAll("InvariantK[SafeInvAlg]", SerializableTests.serializable(InvariantK[SafeInvAlg]))

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
    import WithExtraTpeParam.autoDerive._
    WithExtraTpeParam[Option, String].a(Some("5")) should be(Some("5"))
    WithExtraTpeParam[Option, String].b(Some(5)) should be(Some("5"))
  }

  test("Alg with extra type parameters before effect type") {
    implicit val algWithExtraTP: AlgWithExtraTP2[String, Try] = new AlgWithExtraTP2[String, Try] {
      def a(i: Try[Int]) = i.map(_.toString)
    }
    import AlgWithExtraTP2.autoDerive._
    AlgWithExtraTP2[String, Option].a(Some(5)) should be(Some("5"))
  }

  test("Alg with type member") {
    implicit val tryInt = new AlgWithTypeMember[Try] {
      type T = String
      def a(i: Try[String]): Try[String] = i.map(_ + "a")
    }

    AlgWithTypeMember.imapK(tryInt)(toFk)(otFk).a(Some("4")) should be(Some("4a"))
  }

  test("Alg with type member fully refined") {
    implicit val tryInt = new AlgWithTypeMember[Try] {
      type T = String
      def a(i: Try[String]): Try[String] = i.map(_ + "a")
    }

    import AlgWithTypeMember.fullyRefined._
    import AlgWithTypeMember.fullyRefined.autoDerive._

    val algAux: AlgWithTypeMember.Aux[Option, String] = implicitly
    algAux.a(Some("4")) should be(Some("4a"))
  }

  test("turn off auto derivation") {
    implicit object foo extends AlgWithoutAutoDerivation[Try] {
      def a(i: Try[Int]): Try[Int] = i
    }

    illTyped(""" AlgWithoutAutoDerivation.autoDerive """)
  }

  test("with default impl") {
    implicit object foo extends AlgWithDefaultImpl[Try]
    foo.imapK(toFk)(otFk).const(Option(1)) should be(3)
  }

  test("with methods with type param") {
    implicit object foo extends AlgWithTypeParam[Try] {
      def a[T](i: Try[T]): Try[String] = i.map(_.toString)
    }
    foo.imapK(toFk)(otFk).a(Option(1)) should be(Some("1"))
  }
}

object autoInvariantKTests {
  implicit val toFk: Try ~> Option = λ[Try ~> Option](_.toOption)
  implicit val otFk: Option ~> Try = λ[Option ~> Try](o => Try(o.get))

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

  @autoInvariantK(autoDerivation = false)
  trait AlgWithoutAutoDerivation[F[_]] {
    def a(i: F[Int]): F[Int]
  }

  @autoInvariantK @finalAlg
  trait AlgWithDef[F[_]] {
    def a: F[Int]
    def b(c: F[Int]): F[String]
  }

  @autoInvariantK @finalAlg
  trait AlgWithDefaultImpl[F[_]] {
    def const(i: F[Int]): Int = 3
  }

  @autoInvariantK @finalAlg
  trait AlgWithTypeParam[F[_]] {
    def a[T](i: F[T]): F[String]
  }

  @autoInvariantK @finalAlg
  trait AlgWithCurryMethod[F[_]] {
    def a(t: F[Int])(b: String): F[String]
  }

  @autoInvariantK
  trait AlgWithVarArgsParameter[F[_]] {
    def sum(xs: Int*): Int
    def covariantSum(xs: Int*): F[Int]
    def contravariantSum(xs: F[Int]*): Int
    def invariantSum(xs: F[Int]*): F[Int]
  }

  @autoInvariantK
  trait AlgWithByNameParameter[F[_]] {
    def whenM(cond: F[Boolean])(action: => F[Unit]): F[Unit]
  }
}
