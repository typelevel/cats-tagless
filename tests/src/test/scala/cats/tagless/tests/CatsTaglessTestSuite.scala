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

package cats.tagless.tests
import cats.tests.CatsSuite
import cats.arrow.FunctionK
import cats.instances.AllInstances
import cats.kernel.Eq
import cats.laws.discipline.ExhaustiveCheck
import cats.laws.discipline.ExhaustiveCheck.instance
import cats.tagless.syntax.AllSyntax
import org.scalacheck.{Arbitrary, Gen}

import scala.util.{Failure, Success, Try}

class CatsTaglessTestSuite extends CatsSuite with TestInstances with AllInstances with AllSyntax


trait TestInstances {
  implicit val catsDataArbitraryOptionList: Arbitrary[FunctionK[Option, List]] = Arbitrary(Gen.const(λ[FunctionK[Option, List]](_.toList)))
  implicit val catsDataArbitraryListOption: Arbitrary[FunctionK[List, Option]] = Arbitrary(Gen.const(λ[FunctionK[List, Option]](_.headOption)))
  implicit val catsDataArbitraryTryOption: Arbitrary[FunctionK[Try, Option]] = Arbitrary(Gen.const(λ[FunctionK[Try, Option]](_.toOption)))
  implicit val catsDataArbitraryOptionTry: Arbitrary[FunctionK[Option, Try]] = Arbitrary(Gen.const(λ[FunctionK[Option, Try]](o => Try(o.get))))
  implicit val catsDataArbitraryListVector: Arbitrary[FunctionK[List, Vector]] = Arbitrary(Gen.const(λ[FunctionK[List, Vector]](_.toVector)))
  implicit val catsDataArbitraryVectorList: Arbitrary[FunctionK[Vector, List]] = Arbitrary(Gen.const(λ[FunctionK[Vector, List]](_.toList)))

  implicit val eqThrow: Eq[Throwable] = Eq.allEqual

  implicit def exhaustiveCheckForTry[A](implicit A: ExhaustiveCheck[A]): ExhaustiveCheck[Try[A]] =
    instance(Stream.cons(Failure(new RuntimeException("some exception")), A.allValues.map(Success(_))))

  implicit def exhaustiveCheckForList[A](implicit A: ExhaustiveCheck[A]): ExhaustiveCheck[List[A]] =
    instance(Stream.cons(Nil, A.allValues.map(List(_))))

  implicit def exhaustiveCheckForString: ExhaustiveCheck[String] = new ExhaustiveCheck[String] {
    override def allValues: Stream[String] = List("", "fasdf23", "dv23[09;ksf", "dfa, ", "dcv2,asdk234;riosd;fkj;").toStream
  }

  implicit def exhaustiveCheckForFloat: ExhaustiveCheck[Float] = new ExhaustiveCheck[Float] {
    override def allValues: Stream[Float] = List(-1f, 32f, 0f, 322342342523567.23423f, 100f).toStream
  }

  implicit def exhaustiveCheckForInt: ExhaustiveCheck[Int] = new ExhaustiveCheck[Int] {
    override def allValues: Stream[Int] = List(-1, 32, 0, 322232342, 100).toStream
  }

  implicit def exhaustiveCheckForLong: ExhaustiveCheck[Long] = new ExhaustiveCheck[Long] {
    override def allValues: Stream[Long] = List(-1L, 32L, 0L, 23423322232342L, 100L, -234234324235L).toStream
  }

}

object TestInstances extends TestInstances
