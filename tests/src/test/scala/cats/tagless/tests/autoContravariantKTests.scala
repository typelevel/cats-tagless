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

import cats.Eq
import cats.data.Cokleisli
import cats.instances.all._
import cats.laws.discipline.SerializableTests
import cats.laws.discipline.arbitrary._
import cats.laws.discipline.eq._
import cats.tagless.{ContravariantK, autoContravariantK}
import cats.tagless.instances.all._
import cats.tagless.laws.discipline.ContravariantKTests
import org.scalacheck.{Arbitrary, Cogen}

import scala.util.Try

class autoContravariantKTests extends CatsTaglessTestSuite {
  import autoContravariantKTests._

  checkAll("ContravariantK[TestAlgebra]", ContravariantKTests[TestAlgebra].contravariantK[Try, Option, List, Int])
  checkAll("Serializable ContravariantK[TestAlgebra]", SerializableTests.serializable(ContravariantK[TestAlgebra]))
}

object autoContravariantKTests {
  import TestInstances._

  @autoContravariantK
  trait TestAlgebra[F[_]] {
    def sum(xs: F[Int]): Int
    def sumAll(xss: F[Int]*): Int
    def foldSpecialized(init: String)(f: (Int, String) => Int): Cokleisli[F, String, Int]
  }

  object TestAlgebra {
    implicit def eqv[F[_]](implicit arbFi: Arbitrary[F[Int]], arbFs: Arbitrary[F[String]]): Eq[TestAlgebra[F]] =
      Eq.by { algebra => (algebra.sum _, algebra.sumAll _, algebra.foldSpecialized _) }
  }

  implicit def arbitrary[F[_]](
    implicit arbFs: Arbitrary[F[String]], coFi: Cogen[F[Int]], coFs: Cogen[F[String]]
  ): Arbitrary[TestAlgebra[F]] = Arbitrary(for {
    s <- Arbitrary.arbitrary[F[Int] => Int]
    sa <- Arbitrary.arbitrary[Seq[F[Int]] => Int]
    fs <- Arbitrary.arbitrary[(String, (Int, String) => Int) => Cokleisli[F, String, Int]]
  } yield new TestAlgebra[F] {
    def sum(xs: F[Int]) = s(xs)
    def sumAll(xss: F[Int]*) = sa(xss)
    def foldSpecialized(init: String)(f: (Int, String) => Int) = fs(init, f)
  })

  @autoContravariantK
  trait TestAlgebraWithExtraTypeParam[F[_], A] extends TestAlgebra[F] {
    def fold[B](init: B)(f: (B, A) => B): Cokleisli[F, A, B]
  }
}
