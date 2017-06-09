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
import mainecoon.laws.discipline.FunctorKTests
import autoFunctorKTests._
import cats.arrow.FunctionK
import cats.free.Free
import shapeless.test.illTyped

class autoFunctorKTests extends MainecoonTestSuite {
  test("simple mapK") {

    val optionParse: SafeAlg[Option] = Interpreters.tryInterpreter.mapK(fk)

    optionParse.parseInt("3") should be(Some(3))
    optionParse.parseInt("sd") should be(None)
    optionParse.divide(3f, 3f) should be(Some(1f))

  }

  checkAll("ParseAlg[Option]", FunctorKTests[SafeAlg].functorK[Try, Option, List, Int])
  checkAll("FunctorK[ParseAlg]", SerializableTests.serializable(FunctorK[SafeAlg]))

  test("Alg with non effect method") {
    val tryInt = new AlgWithNonEffectMethod[Try] {
      def a(i: Int): Try[Int] = Try(i)
      def b(i: Int): Int = i
    }

    tryInt.mapK(fk).a(3) should be(Some(3))
    tryInt.mapK(fk).b(2) should be(2)
  }

  test("Alg with non effect method with default Impl") {
    val tryInt = new AlgWithDefaultImpl[Try] {
      def plusOne(i: Int): Try[Int] = Try(i + 1)
    }

    tryInt.mapK(fk).plusOne(3) should be(Some(4))
    tryInt.mapK(fk).minusOne(2) should be(1)
  }


  test("Alg with extra type parameters") {

    implicit val algWithExtraTP: AlgWithExtraTP[Try, String] = new AlgWithExtraTP[Try, String] {
      def a(i: Int) = Try(i.toString)
    }
    AlgWithExtraTP[Option, String].a(5) should be(Some("5"))
  }

  test("Alg with extra type parameters before effect type") {
    implicit val algWithExtraTP: AlgWithExtraTP2[String, Try] = new AlgWithExtraTP2[String, Try] {
      def a(i: Int) = Try(i.toString)
    }
    AlgWithExtraTP2[String, Option].a(5) should be(Some("5"))
  }

  test("Alg with type member") {
    implicit val tryInt = new AlgWithTypeMember[Try] {
      type T = String
      def a(i: Int): Try[String] = Try(i.toString)
    }

    AlgWithTypeMember[Option].a(3) should be(Some("3"))
    val algAux: AlgWithTypeMember.Aux[Option, String] = AlgWithTypeMember.mapK(tryInt)(fk)
    algAux.a(4) should be(Some("4"))
  }

  test("Stack safety with Free") {
    val incTry: Increment[Try] = new Increment[Try] {
      def plusOne(i: Int) = Try(i + 1)
    }

    val incFree = incTry.mapK(λ[Try ~> Free[Try, ?]](t => Free.liftF(t)))

    def a(i: Int): Free[Try, Int] = for {
      j <- incFree.plusOne(i)
      z <- if (j < 10000) a(j) else Free.pure[Try, Int](j)
    } yield z
    a(0).foldMap(FunctionK.id) should be(util.Success(10000))
  }

  test("turn off auto derivation") {
    implicit object foo extends AlgWithoutAutoDerivation[Try] {
      def a(i: Int): Try[Int] = util.Success(i)
    }

    illTyped { """ implicitly[AlgWithoutAutoDerivation[Option]] """}
  }

  test("defs with no params") {
    implicit object foo extends AlgWithDef[Try] {
      def a = Try(1)
    }

    AlgWithDef[Option].a should be(Some(1))
  }
}


object autoFunctorKTests {
  implicit val fk : Try ~> Option = λ[Try ~> Option](_.toOption)

  @autoFunctorK
  trait AlgWithNonEffectMethod[F[_]] {
    def a(i: Int): F[Int]
    def b(i: Int): Int
  }

  @autoFunctorK @finalAlg
  trait AlgWithTypeMember[F[_]] {
    type T
    def a(i: Int): F[T]
  }

  object AlgWithTypeMember {
    type Aux[F[_], T0] = AlgWithTypeMember[F] { type T = T0 }
  }


  @autoFunctorK @finalAlg
  trait AlgWithExtraTP[F[_], T] {
    def a(i: Int): F[T]
  }

  @autoFunctorK @finalAlg
  trait AlgWithExtraTP2[T, F[_]] {
    def a(i: Int): F[T]
  }

  @autoFunctorK
  trait Increment[F[_]] {
    def plusOne(i: Int): F[Int]
  }

  @autoFunctorK(autoDerivation = false)
  trait AlgWithoutAutoDerivation[F[_]] {
    def a(i: Int): F[Int]
  }

  @autoFunctorK
  trait AlgWithDefaultImpl[F[_]] {
    def plusOne(i: Int): F[Int]
    def minusOne(i: Int): Int = i - 1
  }


  @autoFunctorK @finalAlg
  trait AlgWithDef[F[_]] {
    def a: F[Int]
  }


}
