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

import cats.arrow.FunctionK
import cats.data.Cokleisli
import cats.free.Free
import cats.laws.discipline.SerializableTests
import cats.tagless.laws.discipline.FunctorKTests
import cats.tagless.syntax.all.*
import cats.{Monad, Show, ~>}

import scala.annotation.experimental
import scala.util.Try

@experimental
class autoFunctorKTests extends CatsTaglessTestSuite:
  import autoFunctorKTests.*

  checkAll("FunctorK[SafeAlg]", FunctorKTests[SafeAlg].functorK[Try, Option, List, Int])
  checkAll("FunctorK is Serializable", SerializableTests.serializable(FunctorK[SafeAlg]))

  test("simple mapK") {
    val optionParse: SafeAlg[Option] = Interpreters.tryInterpreter.mapK(fk)
    assertEquals(optionParse.parseInt("3"), Some(3))
    assertEquals(optionParse.parseInt("sd"), None)
    assertEquals(optionParse.divide(3f, 3f), Some(1f))
  }

  test("simple instance summon with autoDeriveFromFunctorK on") {
    given SafeAlg[List] = Interpreters.tryInterpreter.mapK(FunctionK.liftFunction[Try, List](_.toList))
    assertEquals(summon[SafeAlg[List]].parseInt("3"), List(3))
  }

  test("auto derive from functor k") {
    import AutoDerive.given
    import Interpreters.tryInterpreter
    summon[SafeAlg[Option]]
  }

  test("Alg with non effect method") {
    val tryInt = new AlgWithNonEffectMethod[Try]:
      def a(i: Int): Try[Int] = Try(i)
      def b(i: Int): Int = i

    assertEquals(tryInt.mapK(fk).a(3), Some(3))
    assertEquals(tryInt.mapK(fk).b(2), 2)
  }

  test("Alg with non effect method with default Impl") {
    val tryInt = new AlgWithDefaultImpl[Try]:
      def plusOne(i: Int): Try[Int] = Try(i + 1)

    assertEquals(tryInt.mapK(fk).plusOne(3), Some(4))
    assertEquals(tryInt.mapK(fk).minusOne(2), 1)
  }

  test("Alg with extra type parameters") {
    val alg = new AlgWithExtraTP[Try, String]:
      def a(i: Int) = Try(i.toString)

    assertEquals(AlgWithExtraTP.functorK.mapK(alg)(fk).a(5), Some("5"))
  }

  test("Alg with extra type parameters before effect type") {
    val algWithExtraTP = new AlgWithExtraTP2[String, Try]:
      def a(i: Int) = Try(i.toString)

    assertEquals(algWithExtraTP.mapK(fk).a(5), Some("5"))
  }

  // TODO: This should work eventually
  test("Alg with type member") {
    // implicit val tryInt: AlgWithTypeMember.Aux[Try, String] = new AlgWithTypeMember[Try]:
    //   type T = String
    //   def a(i: Int): Try[String] = Try(i.toString)

    val errors = compileErrors("FunctorK.derived[[F[_]] =>> AlgWithTypeMember.Aux[F, String]]")
    assert(errors.startsWith("error: Not supported: type T in trait AlgWithTypeMember"))
  }

  //   assertEquals[Option[Any], Option[Any]](tryInt.mapK(fk).a(3), Some("3"))
  //   import AlgWithTypeMember.fullyRefined.autoDerive.*
  //   val op: AlgWithTypeMember.Aux[Option, String] = implicitly
  //   assertEquals(op.a(3), Option("3"))
  // }

  // test("Alg with type bound") {
  //   import AlgWithTypeBound.*
  //   implicit val tryB: AlgWithTypeBound.Aux[Try, B.type] = new AlgWithTypeBound[Try]:
  //     type T = B.type
  //     override def t = Try(B)

  //   assertEquals[Option[A], Option[A]](tryB.mapK(fk).t, Option(B))
  //   // import AlgWithTypeBound.fullyRefined.autoDerive.*
  //   // val op: AlgWithTypeBound.Aux[Option, B.type] = implicitly
  //   // assertEquals(op.t, Option(B))
  // }

  test("Stack safety with Free") {
    val incTry: Increment[Try] = new Increment[Try]:
      def plusOne(i: Int) = Try(i + 1)

    val incFree = incTry.mapK(FunctionK.liftFunction[Try, Free[Try, *]](t => Free.liftF(t)))

    def a(i: Int): Free[Try, Int] = for
      j <- incFree.plusOne(i)
      z <- if j < 10000 then a(j) else Free.pure[Try, Int](j)
    yield z
    assertEquals(a(0).foldMap(FunctionK.id), util.Success(10000))
  }

  // test("turn off auto derivation") {
  //   @nowarn("cat=unused")
  //   implicit object foo extends AlgWithoutAutoDerivation[Try] {
  //     def a(i: Int): Try[Int] = util.Success(i)
  //   }

  //   assertNoDiff(
  //     compileErrors("AlgWithoutAutoDerivation.autoDerive"),
  //     """|error: value autoDerive is not a member of object cats.tagless.tests.autoFunctorKTests.AlgWithoutAutoDerivation
  //        |AlgWithoutAutoDerivation.autoDerive
  //        |                         ^
  //        |""".stripMargin
  //   )
  // }

  test("defs with no params") {
    implicit object foo extends AlgWithDef[Try]:
      def a = Try(1)

    assertEquals(foo.mapK(fk).a, Some(1))
  }

  test("method with type params") {
    implicit object foo extends AlgWithTParamInMethod[Try]:
      def a[T](t: T): Try[String] = Try(t.toString)

    assertEquals(foo.mapK(fk).a(32), Some("32"))
  }

  test("auto deriviation with existing derivation") {
    // should be just AlgWithOwnDerivation[Option]?
    implicitly[AlgWithOwnDerivation[Option]]
  }

  // test("alg with abstract type class fully refined resolve instance") {
  //   implicit object foo extends AlgWithAbstractTypeClass[Try] {
  //     type TC[T] = Show[T]
  //     def a[T: TC](t: T): Try[String] = Try(t.show)
  //   }

  //   // import AlgWithAbstractTypeClass.fullyRefined.*
  //   // Scalac needs help when abstract type is high order.
  //   // implicit val fShow: FunctorK[[F[_]] =>> AlgWithAbstractTypeClass.Aux[F, Show]] =
  //   // functorKForFullyRefinedAlgWithAbstractTypeClass[Show]
  //   // assertEquals(fShow.mapK(foo)(fk).a(true), Some("true"))
  // }

  // test("alg with abstract type class") {
  //   implicit object foo extends AlgWithAbstractTypeClass[Try] {
  //     type TC[T] = Show[T]
  //     def a[T: TC](t: T): Try[String] = Try(t.show)
  //   }

  //   implicit val show: foo.TC[Boolean] = _.toString

  //   assertEquals(foo.mapK(fk).a(true)(using show), Some("true"))
  // }

  test("alg with default parameter") {
    implicit object foo extends AlgWithDefaultParameter[Try]:
      def greet(name: String) = Try(s"Hello $name")

    val bar = implicitly[FunctorK[AlgWithDefaultParameter]].mapK(foo)(fk)
    assertEquals(bar.greet(), Some("Hello World"))
    assertEquals(bar.greet("John Doe"), Some("Hello John Doe"))
  }

  test("alg with final method") {
    implicit object foo extends AlgWithFinalMethod[Try]:
      def log(msg: String) = Try(msg)

    val bar = implicitly[FunctorK[AlgWithFinalMethod]].mapK(foo)(fk)
    assertEquals(bar.info("green"), Some("[info] green"))
    assertEquals(bar.warn("yellow"), Some("[warn] yellow"))
  }

  test("alg with by-name parameter") {
    implicit object foo extends AlgWithByNameParameter[Try]:
      def log(msg: => String) = Try(msg)

    val bar = implicitly[FunctorK[AlgWithByNameParameter]].mapK(foo)(fk)
    assertEquals(bar.log("level".reverse), Some("level"))
  }

  test("builder-style algebra") {
    val listBuilder: BuilderAlgebra[List] = BuilderAlgebra.Named("foo")
    val optionBuilder = listBuilder.mapK[Option](FunctionK.liftFunction[List, Option](_.headOption))
    assertEquals(optionBuilder.withFoo("bar").unit, Some(()))
  }

object autoFunctorKTests:
  implicit val fk: Try ~> Option = FunctionK.liftFunction[Try, Option](_.toOption)

  trait AlgWithNonEffectMethod[F[_]] derives FunctorK:
    def a(i: Int): F[Int]
    def b(i: Int): Int

  // TODO: @finalAlg
  trait AlgWithTypeMember[F[_]]:
    type T
    def a(i: Int): F[T]

  object AlgWithTypeMember:
    type Aux[F[_], T0] = AlgWithTypeMember[F] { type T = T0 }

  // trait AlgWithTypeBound[F[_]] derives FunctorK:
  //   type T <: AlgWithTypeBound.A
  //   def t: F[T]

  // object AlgWithTypeBound:
  //   sealed abstract class A
  //   case object B extends A
  //   case object C extends A
  //   type Aux[F[_], T0 <: A] = AlgWithTypeBound[F] { type T = T0 }

  // TODO: @finalAlg
  trait AlgWithExtraTP[F[_], T]:
    def a(i: Int): F[T]

  object AlgWithExtraTP:
    given functorK[T]: FunctorK[[F[_]] =>> AlgWithExtraTP[F, T]] = FunctorK.derived

  // TODO: @finalAlg
  trait AlgWithExtraTP2[T, F[_]] derives FunctorK:
    def a(i: Int): F[T]

  trait Increment[F[_]] derives FunctorK:
    def plusOne(i: Int): F[Int]

  trait AlgWithoutAutoDerivation[F[_]] derives FunctorK:
    def a(i: Int): F[Int]

  trait AlgWithDefaultImpl[F[_]] derives FunctorK:
    def plusOne(i: Int): F[Int]
    def minusOne(i: Int): Int = i - 1

  // TODO: @finalAlg
  trait AlgWithDef[F[_]] derives FunctorK:
    def a: F[Int]

  // TODO: @finalAlg
  trait AlgWithTParamInMethod[F[_]] derives FunctorK:
    def a[T](t: T): F[String]

  // TODO: @finalAlg
  trait AlgWithContextBounds[F[_]] derives FunctorK:
    def a[T: Show](t: Int): F[String]

  // TODO: @finalAlg
  // trait AlgWithAbstractTypeClass[F[_]] derives FunctorK:
  //   type TC[T]
  //   def a[T: TC](t: T): F[String]

  // object AlgWithAbstractTypeClass {
  //   type Aux[F[_], TC0[_]] = AlgWithAbstractTypeClass[F] { type TC[T] = TC0[T] }
  //   val functorK = Derive.functorK[[F[_]] =>> AlgWithAbstractTypeClass.Aux[F, List]]
  // }

  // TODO: @finalAlg
  trait AlgWithCurryMethod[F[_]] derives FunctorK:
    def a(t: Int = 42)(b: String): F[String]

  // TODO: @finalAlg
  trait AlgWithOwnDerivation[F[_]] derives FunctorK:
    def a(b: Int): F[String]

  object AlgWithOwnDerivation:
    implicit def fromMonad[F[_]: Monad]: AlgWithOwnDerivation[F] = new AlgWithOwnDerivation[F]:
      def a(b: Int): F[String] = Monad[F].pure(b.toString)

  trait AlgWithDefaultParameter[F[_]] derives FunctorK:
    def greet(name: String = "World"): F[String]

  trait AlgWithFinalMethod[F[_]] derives FunctorK:
    def log(msg: String): F[String]
    final def info(msg: String): F[String] = log(s"[info] $msg")
    final def warn(msg: String): F[String] = log(s"[warn] $msg")

  trait AlgWithByNameParameter[F[_]] derives FunctorK:
    def log(msg: => String): F[String]

  trait AlgWithVarArgsParameter[F[_]] derives FunctorK:
    def sum(xs: Int*): Int
    def fSum(xs: Int*): F[Int]

  trait AlgWithValues[F[_]] derives FunctorK:
    val int: F[Int]
    lazy val str: F[String]

  trait BuilderAlgebra[F[_]] derives FunctorK:
    def unit: F[Unit]
    def withFoo(foo: String): BuilderAlgebra[F]

  object BuilderAlgebra:
    final case class Named(name: String) extends BuilderAlgebra[List]:
      val unit: List[Unit] = List.fill(5)(())
      def withFoo(foo: String): BuilderAlgebra[List] = copy(name = foo)

  trait AlgWithContravariantK[F[_]] derives FunctorK:
    def app(f: Cokleisli[F, String, Int])(x: String): F[Int]
