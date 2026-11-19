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
import cats.data.Cokleisli
import cats.free.Free
import cats.laws.discipline.SerializableTests
import cats.tagless.{AutoDerive, Derive, FunctorK}
import cats.tagless.laws.discipline.FunctorKTests
import cats.{Monad, Show, ~>}

import scala.util.Try

class autoFunctorKTests extends CatsTaglessTestSuite:
  import autoFunctorKTests.*
  import autoFunctorKTests.given

  checkAll("FunctorK[SafeAlg]", FunctorKTests[SafeAlg].functorK[Try, Option, List, Int])
  checkAll("FunctorK is Serializable", SerializableTests.serializable(FunctorK[SafeAlg]))

  test("simple mapK"):
    val optAlg: SafeAlg[Option] = Interpreters.tryInterpreter.mapK(fk)
    assertEquals(optAlg.parseInt("3"), Some(3))
    assertEquals(optAlg.parseInt("sd"), None)
    assertEquals(optAlg.divide(3f, 3f), Some(1f))

  test("simple instance summon with autoDeriveFromFunctorK on"):
    given SafeAlg[List] = Interpreters.tryInterpreter.mapK(FunctionK.liftFunction[Try, List](_.toList))
    assertEquals(summon[SafeAlg[List]].parseInt("3"), List(3))

  test("auto derive from functor k"):
    import AutoDerive.given
    import Interpreters.tryInterpreter
    summon[SafeAlg[Option]]

  test("Alg with non effect method"):
    object tryAlg extends AlgWithNonEffectMethod[Try]:
      def a(i: Int): Try[Int] = Try(i)
      def b(i: Int): Int = i

    assertEquals(tryAlg.mapK(fk).a(3), Some(3))
    assertEquals(tryAlg.mapK(fk).b(2), 2)

  test("Alg with non effect method with default Impl"):
    object tryAlg extends AlgWithDefaultImpl[Try]:
      def plusOne(i: Int): Try[Int] = Try(i + 1)

    assertEquals(tryAlg.mapK(fk).plusOne(3), Some(4))
    assertEquals(tryAlg.mapK(fk).minusOne(2), 1)

  test("Alg with extra type parameters"):
    object tryAlg extends AlgWithExtraTP[Try, String]:
      def a(i: Int) = Try(i.toString)

    val optAlg = AlgWithExtraTP.functorK.mapK(tryAlg)(fk)
    assertEquals(optAlg.a(5), Some("5"))

  test("Alg with extra type parameters before effect type"):
    object tryAlg extends AlgWithExtraTP2[String, Try]:
      def a(i: Int) = Try(i.toString)

    assertEquals(tryAlg.mapK(fk).a(5), Some("5"))

  test("Alg with type member"):
    object tryAlg extends AlgWithTypeMember[Try]:
      type T = String
      def a(i: Int): Try[String] = Try(i.toString)

    val optAlg = AlgWithTypeMember.functorK.mapK(tryAlg)(fk)
    assertEquals(optAlg.a(5), Some("5"))

  test("Alg with type bound"):
    import AlgWithTypeBound.*
    object tryAlg extends AlgWithTypeBound[Try]:
      type T = B.type
      override def t = Try(B)

    assertEquals[Option[A], Option[A]](tryAlg.mapK(fk).t, Option(B))

  test("Stack safety with Free"):
    object tryAlg extends Increment[Try]:
      def plusOne(i: Int) = Try(i + 1)

    val freeAlg = tryAlg
      .mapK(FunctionK.liftFunction[Try, Free[Try, *]](Free.liftF))

    def a(i: Int): Free[Try, Int] = for
      j <- freeAlg.plusOne(i)
      z <- if j < 10000 then a(j) else Free.pure[Try, Int](j)
    yield z
    assertEquals(a(0).foldMap(FunctionK.id), util.Success(10000))

  test("defs with no params"):
    object tryAlg extends AlgWithDef[Try]:
      def a = Try(1)

    assertEquals(tryAlg.mapK(fk).a, Some(1))

  test("method with type params"):
    object tryAlg extends AlgWithTParamInMethod[Try]:
      def a[T](t: T): Try[String] = Try(t.toString)

    assertEquals(tryAlg.mapK(fk).a(32), Some("32"))

  test("auto deriviation with existing derivation") {
    // should be just AlgWithOwnDerivation[Option]?
    summon[AlgWithOwnDerivation[Option]]
  }

  test("alg with abstract type class"):
    object tryAlg extends AlgWithAbstractTypeClass[Try]:
      type TC[T] = Show[T]
      def a[T: TC](t: T): Try[String] = Try(t.show)

    val optAlg = AlgWithAbstractTypeClass.functorK.mapK(tryAlg)(fk)
    val show: optAlg.TC[Boolean] = _.toString
    assertEquals(optAlg.a(true), Some("true"))
    assertEquals(optAlg.a(true)(using show), Some("true"))

  test("alg with default parameter"):
    object tryAlg extends AlgWithDefaultParameter[Try]:
      def greet(name: String) = Try(s"Hello $name")

    val optAlg = tryAlg.mapK(fk)
    assertEquals(optAlg.greet(), Some("Hello World"))
    assertEquals(optAlg.greet("John Doe"), Some("Hello John Doe"))

  test("alg with final method"):
    object tryAlg extends AlgWithFinalMethod[Try]:
      def log(msg: String) = Try(msg)

    val optAlg = tryAlg.mapK(fk)
    assertEquals(optAlg.info("green"), Some("[info] green"))
    assertEquals(optAlg.warn("yellow"), Some("[warn] yellow"))

  test("alg with by-name parameter"):
    object tryAlg extends AlgWithByNameParameter[Try]:
      def log(msg: => String) = Try(msg)

    val optAlg = tryAlg.mapK(fk)
    assertEquals(optAlg.log("level".reverse), Some("level"))

  test("builder-style algebra"):
    val listBuilder: BuilderAlgebra[List] = BuilderAlgebra.Named("foo")
    val optionBuilder = listBuilder.mapK[Option](FunctionK.liftFunction[List, Option](_.headOption))
    assertEquals(optionBuilder.withFoo("bar").unit, Some(()))

object autoFunctorKTests:
  given fk: (Try ~> Option) = FunctionK.liftFunction[Try, Option](_.toOption)

  trait AlgWithNonEffectMethod[F[_]] derives FunctorK:
    def a(i: Int): F[Int]
    def b(i: Int): Int

  trait AlgWithTypeMember[F[_]]:
    type T
    def a(i: Int): F[T]

  object AlgWithTypeMember:
    type Aux[F[_], T0] = AlgWithTypeMember[F] { type T = T0 }
    given functorK[T]: FunctorK[[F[_]] =>> Aux[F, T]] = Derive.functorK

  trait AlgWithTypeBound[F[_]] derives FunctorK:
    type T <: AlgWithTypeBound.A
    def t: F[T]

  object AlgWithTypeBound:
    sealed abstract class A
    case object B extends A
    case object C extends A
    type Aux[F[_], T0 <: A] = AlgWithTypeBound[F] { type T = T0 }

  trait AlgWithExtraTP[F[_], T]:
    def a(i: Int): F[T]

  object AlgWithExtraTP:
    given functorK[T]: FunctorK[[F[_]] =>> AlgWithExtraTP[F, T]] = FunctorK.derived

  trait AlgWithExtraTP2[T, F[_]] derives FunctorK:
    def a(i: Int): F[T]

  trait Increment[F[_]] derives FunctorK:
    def plusOne(i: Int): F[Int]

  trait AlgWithoutAutoDerivation[F[_]] derives FunctorK:
    def a(i: Int): F[Int]

  trait AlgWithDefaultImpl[F[_]] derives FunctorK:
    def plusOne(i: Int): F[Int]
    def minusOne(i: Int): Int = i - 1

  trait AlgWithDef[F[_]] derives FunctorK:
    def a: F[Int]

  trait AlgWithTParamInMethod[F[_]] derives FunctorK:
    def a[T](t: T): F[String]

  trait AlgWithContextBounds[F[_]] derives FunctorK:
    def a[T: Show](t: Int): F[String]

  trait AlgWithAbstractTypeClass[F[_]]:
    type TC[T]
    def a[T: TC](t: T): F[String]

  object AlgWithAbstractTypeClass:
    type Aux[F[_], TC0[_]] = AlgWithAbstractTypeClass[F] { type TC[T] = TC0[T] }
    given functorK[TC[_]]: FunctorK[[F[_]] =>> Aux[F, TC]] = Derive.functorK

  trait AlgWithCurryMethod[F[_]] derives FunctorK:
    def a(t: Int = 42)(b: String): F[String]

  trait AlgWithOwnDerivation[F[_]] derives FunctorK:
    def a(b: Int): F[String]

  object AlgWithOwnDerivation:
    given [F[_]: Monad]: AlgWithOwnDerivation[F] with
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
