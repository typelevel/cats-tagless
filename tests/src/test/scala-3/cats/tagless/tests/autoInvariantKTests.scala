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

import cats.tagless.syntax.all.*
import cats.~>
import cats.laws.discipline.SerializableTests
import cats.laws.discipline.arbitrary.*
import cats.tagless.laws.discipline.InvariantKTests

import scala.annotation.nowarn
import scala.util.Try

class autoInvariantKTests extends CatsTaglessTestSuite:
  import autoInvariantKTests.*

  checkAll("InvariantK[SafeInvAlg]", InvariantKTests[SafeInvAlg].invariantK[Try, Option, List])
  checkAll("InvariantK[CalculatorAlg]", InvariantKTests[CalculatorAlg].invariantK[Try, Option, List])
  checkAll("InvariantK is Serializable", SerializableTests.serializable(InvariantK[SafeInvAlg]))

  test("Alg with non effect method") {
    val tryInt = new NoEffectMethod[Try]:
      def a(i: Int): Int = i

    assertEquals(tryInt.imapK(toFk)(otFk).a(2), 2)
  }

  // test("Alg with contravariant Eff method") {
  //   val tryInt = new ContravariantEff[Try] {
  //     def a(i: Try[Int], j: String): Int = i.get
  //     def b(i: Try[Int]): Int = i.get
  //   }

  //   val oInt = tryInt.imapK(toFk)(otFk)
  //   assertEquals(oInt.a(Option(2), "ignored"), 2)
  //   assertEquals(oInt.b(Option(3)), 3)
  // }

  // test("Alg with Invariant effect method") {
  //   val tryInt = new InvariantEff[Try] {
  //     def a(i: Try[Int], j: String): Try[Int] = i
  //   }

  //   val oInt = tryInt.imapK(toFk)(otFk)
  //   assertEquals(oInt.a(Option(2), "ignored"), Some(2))
  // }

  // test("Alg with extra type parameters auto derivation") {
  //   implicit object algWithExtraTP extends WithExtraTpeParam[Try, String] {
  //     def a(i: Try[String]) = i
  //     def b(i: Try[Int]) = i.map(_.toString)
  //   }
  //   import WithExtraTpeParam.autoDerive.*
  //   assertEquals(WithExtraTpeParam[Option, String].a(Some("5")), Some("5"))
  //   assertEquals(WithExtraTpeParam[Option, String].b(Some(5)), Some("5"))
  // }

  // test("Alg with extra type parameters before effect type") {
  //   implicit val algWithExtraTP: AlgWithExtraTP2[String, Try] = new AlgWithExtraTP2[String, Try] {
  //     def a(i: Try[Int]) = i.map(_.toString)
  //   }
  //   import AlgWithExtraTP2.autoDerive.*
  //   assertEquals(AlgWithExtraTP2[String, Option].a(Some(5)), Some("5"))
  // }

  // test("Alg with type member") {
  //   implicit val tryInt = new AlgWithTypeMember[Try] {
  //     type T = String
  //     def a(i: Try[String]): Try[String] = i.map(_ + "a")
  //   }

  //   assertEquals(AlgWithTypeMember.imapK(tryInt)(toFk)(otFk).a(Some("4")), Some("4a"))
  // }

  // test("Alg with type member fully refined") {
  //   implicit val tryInt = new AlgWithTypeMember[Try] {
  //     type T = String
  //     def a(i: Try[String]): Try[String] = i.map(_ + "a")
  //   }

  //   import AlgWithTypeMember.fullyRefined.*
  //   import AlgWithTypeMember.fullyRefined.autoDerive.*

  //   val algAux: AlgWithTypeMember.Aux[Option, String] = implicitly
  //   assertEquals(algAux.a(Some("4")), Some("4a"))
  // }

  // test("turn off auto derivation") {
  //   @nowarn("cat=unused")
  //   implicit object foo extends AlgWithoutAutoDerivation[Try] {
  //     def a(i: Try[Int]): Try[Int] = i
  //   }

  //   assertNoDiff(
  //     compileErrors("AlgWithoutAutoDerivation.autoDerive"),
  //     """|error: value autoDerive is not a member of object cats.tagless.tests.autoInvariantKTests.AlgWithoutAutoDerivation
  //        |AlgWithoutAutoDerivation.autoDerive
  //        |                         ^
  //        |""".stripMargin
  //   )
  // }

  // test("with default impl") {
  //   implicit object foo extends AlgWithDefaultImpl[Try]
  //   assertEquals(foo.imapK(toFk)(otFk).const(Option(1)), Some(1))
  // }

  // test("with methods with type param") {
  //   implicit object foo extends AlgWithTypeParam[Try] {
  //     def a[T](i: Try[T]): Try[String] = i.map(_.toString)
  //   }
  //   assertEquals(foo.imapK(toFk)(otFk).a(Option(1)), Some("1"))
  // }

object autoInvariantKTests:
  implicit val toFk: Try ~> Option = FunctionKLift[Try, Option](_.toOption)
  implicit val otFk: Option ~> Try = FunctionKLift[Option, Try](o => Try(o.get))

  trait NoEffectMethod[F[_]] derives InvariantK:
    def a(i: Int): Int

  // trait ContravariantEff[F[_]] derives InvariantK {
  //   def a(i: F[Int], j: String): Int
  //   def b(i: F[Int]): Int
  // }

  // trait InvariantEff[F[_]] derives InvariantK {
  //   def a(i: F[Int], j: String): F[Int]
  // }

  // TODO: @finalAlg
  // trait AlgWithTypeMember[F[_]] derives InvariantK {
  //   type T
  //   def a(i: F[T]): F[T]
  // }

  // object AlgWithTypeMember {
  //   type Aux[F[_], T0] = AlgWithTypeMember[F] { type T = T0 }
  // }

  // TODO: @finalAlg
  // trait WithExtraTpeParam[F[_], T] derives InvariantK {
  //   def a(i: F[T]): F[T]
  //   def b(i: F[Int]): F[T]
  // }

  // TODO: @finalAlg
  // trait AlgWithExtraTP2[T, F[_]] derives InvariantK {
  //   def a(i: F[Int]): F[T]
  // }

  // trait AlgWithoutAutoDerivation[F[_]] derives InvariantK {
  //   def a(i: F[Int]): F[Int]
  // }

  // // @finalAlg
  // trait AlgWithDef[F[_]] derives InvariantK {
  //   def a: F[Int]
  //   def b(c: F[Int]): F[String]
  // }

  // @finalAlg
  trait AlgWithDefaultImpl[F[_]] derives InvariantK:
    def const(i: F[Int]): F[Int] = i

  // @finalAlg
  // trait AlgWithTypeParam[F[_]] derives InvariantK {
  //   def a[T](i: F[T]): F[String]
  // }

  // @finalAlg
  // trait AlgWithCurryMethod[F[_]] derives InvariantK {
  //   def a(t: F[Int])(b: String): F[String]
  // }

  trait AlgWithVarArgsParameter[F[_]] derives InvariantK:
    def sum(xs: Int*): Int
    def covariantSum(xs: Int*): F[Int]
    // def contravariantSum(xs: F[Int]*): Int
    // def invariantSum(xs: F[Int]*): F[Int]

  // trait AlgWithByNameParameter[F[_]] derives InvariantK {
  //   def whenM(cond: F[Boolean])(action: => F[Unit]): F[Unit]
  // }
