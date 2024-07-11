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
import cats.arrow.FunctionK
import cats.data.*
import cats.laws.discipline.ExhaustiveCheck
import cats.tagless.aop.Instrumentation
import cats.tagless.syntax.AllSyntax
import munit.DisciplineSuite
import org.scalacheck.{Arbitrary, Gen}

import scala.util.Try

class CatsTaglessTestSuite extends DisciplineSuite with cats.syntax.AllSyntax with TestInstances with AllSyntax

object TestInstances extends TestInstances
trait TestInstances {

  implicit val catsDataArbitraryOptionList: Arbitrary[FunctionK[Option, List]] =
    Arbitrary(Gen.const(FunctionK.liftFunction[Option, List](_.toList)))

  implicit val catsDataArbitraryListOption: Arbitrary[FunctionK[List, Option]] =
    Arbitrary(Gen.const(FunctionK.liftFunction[List, Option](_.headOption)))

  implicit val catsDataArbitraryTryOption: Arbitrary[FunctionK[Try, Option]] =
    Arbitrary(Gen.const(FunctionK.liftFunction[Try, Option](_.toOption)))

  implicit val catsDataArbitraryOptionTry: Arbitrary[FunctionK[Option, Try]] =
    Arbitrary(Gen.const(FunctionK.liftFunction[Option, Try](o => Try(o.get))))

  implicit val catsDataArbitraryListVector: Arbitrary[FunctionK[List, Vector]] =
    Arbitrary(Gen.const(FunctionK.liftFunction[List, Vector](_.toVector)))

  implicit val catsDataArbitraryVectorList: Arbitrary[FunctionK[Vector, List]] =
    Arbitrary(Gen.const(FunctionK.liftFunction[Vector, List](_.toList)))

  implicit val catsTaglessLawsEqForThrowable: Eq[Throwable] = Eq.allEqual

  implicit def catsTaglessLawsExhaustiveCheckForArbitrary[A: Arbitrary]: ExhaustiveCheck[A] =
    ExhaustiveCheck.instance(Gen.resize(30, Arbitrary.arbitrary[List[A]]).sample.get)

  implicit def catsTaglessLawsEqForFunc[F[_], A, B](implicit ev: Eq[A => F[B]]): Eq[Func[F, A, B]] =
    Eq.by(_.run)

  implicit def catsTaglessLawsEqForKleisli[F[_], A, B](implicit ev: Eq[A => F[B]]): Eq[Kleisli[F, A, B]] =
    Eq.by(_.run)

  implicit def catsTaglessLawsEqForCokleisli[F[_], A, B](implicit ev: Eq[F[A] => B]): Eq[Cokleisli[F, A, B]] =
    Eq.by(_.run)

  implicit def catsTaglessLawsEqForInstrumentation[F[_], A](implicit ev: Eq[F[A]]): Eq[Instrumentation[F, A]] =
    Eq.by(i => (i.algebraName, i.methodName, i.value))

  implicit def catsTaglessLawsEqForTuple2K[F[_], G[_], A](implicit fa: Eq[F[A]], ga: Eq[G[A]]): Eq[Tuple2K[F, G, A]] =
    Eq.by(t2k => (t2k.first, t2k.second))
}
