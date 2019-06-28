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

import cats.Eq
import cats.arrow.FunctionK
import cats.data._
import cats.laws.discipline.ExhaustiveCheck
import cats.tagless.instances.AllInstances
import cats.tagless.laws.discipline.SemigroupalKTests.IsomorphismsK
import cats.tagless.syntax.AllSyntax
import cats.tagless.{InvariantK, Tuple3K}
import cats.tests.CatsSuite
import org.scalacheck.{Arbitrary, Gen}

import scala.util.Try

class CatsTaglessTestSuite extends CatsSuite with TestInstances with AllInstances with AllSyntax

object TestInstances extends TestInstances

trait TestInstances {

  implicit val catsDataArbitraryOptionList: Arbitrary[FunctionK[Option, List]] =
    Arbitrary(Gen.const(λ[FunctionK[Option, List]](_.toList)))

  implicit val catsDataArbitraryListOption: Arbitrary[FunctionK[List, Option]] =
    Arbitrary(Gen.const(λ[FunctionK[List, Option]](_.headOption)))

  implicit val catsDataArbitraryTryOption: Arbitrary[FunctionK[Try, Option]] =
    Arbitrary(Gen.const(λ[FunctionK[Try, Option]](_.toOption)))

  implicit val catsDataArbitraryOptionTry: Arbitrary[FunctionK[Option, Try]] =
    Arbitrary(Gen.const(λ[FunctionK[Option, Try]](o => Try(o.get))))

  implicit val catsDataArbitraryListVector: Arbitrary[FunctionK[List, Vector]] =
    Arbitrary(Gen.const(λ[FunctionK[List, Vector]](_.toVector)))

  implicit val catsDataArbitraryVectorList: Arbitrary[FunctionK[Vector, List]] =
    Arbitrary(Gen.const(λ[FunctionK[Vector, List]](_.toList)))

  implicit val catsTaglessLawsEqForThrowable: Eq[Throwable] = Eq.allEqual

  implicit def catsTaglessLawsExhaustiveCheckForArbitrary[A: Arbitrary]: ExhaustiveCheck[A] =
    ExhaustiveCheck.instance(Gen.resize(30, Arbitrary.arbitrary[Stream[A]]).sample.get)

  implicit def catsTaglessLawsEqForFunc[F[_], A, B](implicit ev: Eq[A => F[B]]): Eq[Func[F, A, B]] =
    Eq.by(_.run)

  implicit def catsTaglessLawsEqForKleisli[F[_], A, B](implicit ev: Eq[A => F[B]]): Eq[Kleisli[F, A, B]] =
    Eq.by(_.run)

  //------------------------------------------------------------------
  // The instances below are needed due to type inference limitations:
  //------------------------------------------------------------------

  implicit def catsTaglessLawsIsomorphismsK110[Alg[_[_], _[_], _], F[_], A](
    implicit ev: InvariantK[Alg[F, ?[_], A]]
  ): IsomorphismsK[Alg[F, ?[_], A]] = IsomorphismsK.invariantK[Alg[F, ?[_], A]]

  implicit def catsTaglessLawsIsomorphismsK100[Alg[_[_], _, _], A, B](
    implicit ev: InvariantK[Alg[?[_], A, B]]
  ): IsomorphismsK[Alg[?[_], A, B]] = IsomorphismsK.invariantK[Alg[?[_], A, B]]

  implicit def catsTaglessLawsIsomorphismsK10[Alg[_[_], _], A](
    implicit ev: InvariantK[Alg[?[_], A]]
  ): IsomorphismsK[Alg[?[_], A]] = IsomorphismsK.invariantK[Alg[?[_], A]]

  implicit def catsTaglessLawsEqForTuple2K[F[_], G[_], A](
    implicit ev: Eq[(F[A], G[A])]
  ): Eq[Tuple2K[F, G, A]] = Eq.by(t2k => (t2k.first, t2k.second))

  implicit def catsTaglessLawsEqForEitherKTuple3K[F[_], G[_], H[_], I[_], A](
    implicit ev: Eq[Either[F[A], (G[A], H[A], I[A])]]
  ): Eq[EitherK[F, Tuple3K[G, H, I]#λ, A]] = Eq.by(_.run)

  implicit def catsTaglessLawsEqForEitherTTuple3K[F[_], G[_], H[_], A, B](
    implicit ev: Eq[(F[Either[A, B]], G[Either[A, B]], H[Either[A, B]])]
  ): Eq[EitherT[Tuple3K[F, G, H]#λ, A, B]] = Eq.by(_.value)

  implicit def catsTaglessLawsEqForFuncTuple3K[F[_], G[_], H[_], A, B](
    implicit ev: Eq[A => (F[B], G[B], H[B])]
  ): Eq[Func[Tuple3K[F, G, H]#λ, A, B]] = Eq.by(_.run)

  implicit def catsTaglessLawsEqForIdTTuple3K[F[_], G[_], H[_], A](
    implicit ev: Eq[(F[A], G[A], H[A])]
  ): Eq[IdT[Tuple3K[F, G, H]#λ, A]] = Eq.by(_.value)

  implicit def catsTaglessLawsEqForIorTTuple3K[F[_], G[_], H[_], A, B](
    implicit ev: Eq[(F[Ior[A, B]], G[Ior[A, B]], H[Ior[A, B]])]
  ): Eq[IorT[Tuple3K[F, G, H]#λ, A, B]] = Eq.by(_.value)

  implicit def catsTaglessLawsEqForKleisliTuple3K[F[_], G[_], H[_], A, B](
    implicit ev: Eq[A => (F[B], G[B], H[B])]
  ): Eq[Kleisli[Tuple3K[F, G, H]#λ, A, B]] = Eq.by(_.run)

  implicit def catsTaglessLawsEqForOneAndTuple3K[F[_], G[_], H[_], A](
    implicit ev: Eq[(A, (F[A], G[A], H[A]))]
  ): Eq[OneAnd[Tuple3K[F, G, H]#λ, A]] = Eq.by(x => (x.head, x.tail))

  implicit def catsTaglessLawsEqForOptionTTuple3K[F[_], G[_], H[_], A](
    implicit ev: Eq[(F[Option[A]], G[Option[A]], H[Option[A]])]
  ): Eq[OptionT[Tuple3K[F, G, H]#λ, A]] = Eq.by(_.value)

  implicit def catsTaglessLawsEqForTuple2KTuple3K[F[_], G[_], H[_], I[_], A](
    implicit ev: Eq[(F[A], (G[A], H[A], I[A]))]
  ): Eq[Tuple2K[F, Tuple3K[G, H, I]#λ, A]] = Eq.by(t2k => (t2k.first, t2k.second))

  implicit def catsTaglessLawsEqForWriterTTuple3K[F[_], G[_], H[_], A, B](
    implicit ev: Eq[(F[(A, B)], G[(A, B)], H[(A, B)])]
  ): Eq[WriterT[Tuple3K[F, G, H]#λ, A, B]] = Eq.by(_.run)
}
