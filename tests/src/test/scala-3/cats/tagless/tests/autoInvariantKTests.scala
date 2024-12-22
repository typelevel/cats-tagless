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

import cats.arrow.FunctionK
import cats.laws.discipline.SerializableTests
import cats.laws.discipline.arbitrary.*
import cats.tagless.{AutoDerive, Derive, InvariantK}
import cats.tagless.laws.discipline.InvariantKTests
import cats.~>

import scala.annotation.experimental
import scala.util.Try

@experimental
class autoInvariantKTests extends CatsTaglessTestSuite:
  import autoInvariantKTests.*
  import autoInvariantKTests.given

  checkAll("InvariantK[SafeInvAlg]", InvariantKTests[SafeInvAlg].invariantK[Try, Option, List])
  checkAll("InvariantK[CalculatorAlg]", InvariantKTests[CalculatorAlg].invariantK[Try, Option, List])
  checkAll("InvariantK is Serializable", SerializableTests.serializable(InvariantK[SafeInvAlg]))

  test("Alg with non effect method"):
    object tryAlg extends NoEffectMethod[Try]:
      def a(i: Int): Int = i

    assertEquals(tryAlg.imapK(toFk)(otFk).a(2), 2)

  test("Alg with contravariant Eff method"):
    object tryAlg extends ContravariantEff[Try]:
      def a(i: Try[Int], j: String): Int = i.get
      def b(i: Try[Int]): Int = i.get

    val optAlg = tryAlg.imapK(toFk)(otFk)
    assertEquals(optAlg.a(Option(2), "ignored"), 2)
    assertEquals(optAlg.b(Option(3)), 3)

  test("Alg with Invariant effect method"):
    object tryAlg extends InvariantEff[Try]:
      def a(i: Try[Int], j: String): Try[Int] = i

    assertEquals(tryAlg.imapK(toFk)(otFk).a(Option(2), "ignored"), Some(2))

  test("Alg with extra type parameters auto derivation"):
    object tryAlg extends WithExtraTpeParam[Try, String]:
      def a(i: Try[String]) = i
      def b(i: Try[Int]) = i.map(_.toString)

    val optAlg = WithExtraTpeParam.invariantK.imapK(tryAlg)(toFk)(otFk)
    assertEquals(optAlg.a(Some("5")), Some("5"))
    assertEquals(optAlg.b(Some(5)), Some("5"))

  test("Alg with extra type parameters before effect type"):
    given AlgWithExtraTP2[String, Try] with
      def a(i: Try[Int]) = i.map(_.toString)

    import AutoDerive.given
    assertEquals(summon[AlgWithExtraTP2[String, Option]].a(Some(5)), Some("5"))

  test("Alg with type member"):
    object tryAlg extends AlgWithTypeMember[Try]:
      type T = String
      def a(i: Try[String]): Try[String] = i.map(_ + "a")

    val optAlg = AlgWithTypeMember.invariantK.imapK(tryAlg)(toFk)(otFk)
    assertEquals(optAlg.a(Some("4")), Some("4a"))

  test("with default impl"):
    object tryAlg extends AlgWithDefaultImpl[Try]
    assertEquals(tryAlg.imapK(toFk)(otFk).const(Option(1)), Some(1))

  test("with methods with type param"):
    object tryAlg extends AlgWithTypeParam[Try]:
      def a[T](i: Try[T]): Try[String] = i.map(_.toString)

    assertEquals(tryAlg.imapK(toFk)(otFk).a(Option(1)), Some("1"))

@experimental
object autoInvariantKTests:
  given toFk: (Try ~> Option) = FunctionK.liftFunction[Try, Option](_.toOption)
  given otFk: (Option ~> Try) = FunctionK.liftFunction[Option, Try](o => Try(o.get))

  trait NoEffectMethod[F[_]] derives InvariantK:
    def a(i: Int): Int

  trait ContravariantEff[F[_]] derives InvariantK:
    def a(i: F[Int], j: String): Int
    def b(i: F[Int]): Int

  trait InvariantEff[F[_]] derives InvariantK:
    def a(i: F[Int], j: String): F[Int]

  trait AlgWithTypeMember[F[_]]:
    type T
    def a(i: F[T]): F[T]

  object AlgWithTypeMember:
    type Aux[F[_], T0] = AlgWithTypeMember[F] { type T = T0 }
    given invariantK[A]: InvariantK[[F[_]] =>> Aux[F, A]] = Derive.invariantK

  trait WithExtraTpeParam[F[_], T]:
    def a(i: F[T]): F[T]
    def b(i: F[Int]): F[T]

  object WithExtraTpeParam:
    given invariantK[T]: InvariantK[[F[_]] =>> WithExtraTpeParam[F, T]] = InvariantK.derived

  trait AlgWithExtraTP2[T, F[_]] derives InvariantK:
    def a(i: F[Int]): F[T]

  trait AlgWithoutAutoDerivation[F[_]] derives InvariantK:
    def a(i: F[Int]): F[Int]

  trait AlgWithDef[F[_]] derives InvariantK:
    def a: F[Int]
    def b(c: F[Int]): F[String]

  trait AlgWithDefaultImpl[F[_]] derives InvariantK:
    def const(i: F[Int]): F[Int] = i

  trait AlgWithTypeParam[F[_]] derives InvariantK:
    def a[T](i: F[T]): F[String]

  trait AlgWithCurryMethod[F[_]] derives InvariantK:
    def a(t: F[Int])(b: String): F[String]

  trait AlgWithVarArgsParameter[F[_]] derives InvariantK:
    def sum(xs: Int*): Int
    def covariantSum(xs: Int*): F[Int]
    def contravariantSum(xs: F[Int]*): Int
    def invariantSum(xs: F[Int]*): F[Int]

  trait AlgWithByNameParameter[F[_]] derives InvariantK:
    def whenM(cond: F[Boolean])(action: => F[Unit]): F[Unit]
