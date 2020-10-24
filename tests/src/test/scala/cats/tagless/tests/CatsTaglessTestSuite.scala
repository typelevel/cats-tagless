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
import cats.data._
import cats.laws.discipline.ExhaustiveCheck
import cats.tagless.aop.Instrumentation
import cats.tagless.laws.discipline.SemigroupalKTests.IsomorphismsK
import cats.tagless.syntax.AllSyntax
import cats.tagless.{InvariantK, Tuple3K}
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.typelevel.discipline.scalatest.FunSuiteDiscipline

import scala.util.Try

class CatsTaglessTestSuite
    extends AnyFunSuiteLike
    with Matchers
    with ScalaCheckDrivenPropertyChecks
    with FunSuiteDiscipline
    with cats.syntax.AllSyntax
    with StrictCatsEquality
    with TestInstances
    with AllSyntax

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
    ExhaustiveCheck.instance(Gen.resize(30, Arbitrary.arbitrary[List[A]]).sample.get)

  implicit def catsTaglessLawsEqForFunc[F[_], A, B](implicit ev: Eq[A => F[B]]): Eq[Func[F, A, B]] =
    Eq.by(_.run)

  implicit def catsTaglessLawsEqForKleisli[F[_], A, B](implicit ev: Eq[A => F[B]]): Eq[Kleisli[F, A, B]] =
    Eq.by(_.run)

  implicit def catsTaglessLawsEqForCokleisli[F[_], A, B](implicit ev: Eq[F[A] => B]): Eq[Cokleisli[F, A, B]] =
    Eq.by(_.run)

  implicit def catsTaglessLawsEqForInstrumentation[F[_], A](implicit ev: Eq[F[A]]): Eq[Instrumentation[F, A]] =
    Eq.by(i => (i.algebraName, i.methodName, i.value))

  //------------------------------------------------------------------
  // The instances below are needed due to type inference limitations:
  //------------------------------------------------------------------

  implicit def catsTaglessLawsIsomorphismsK110[Alg[_[_], _[_], _], F[_], A](implicit
      ev: InvariantK[Alg[F, *[_], A]]
  ): IsomorphismsK[Alg[F, *[_], A]] = IsomorphismsK.invariantK[Alg[F, *[_], A]]

  implicit def catsTaglessLawsIsomorphismsK100[Alg[_[_], _, _], A, B](implicit
      ev: InvariantK[Alg[*[_], A, B]]
  ): IsomorphismsK[Alg[*[_], A, B]] = IsomorphismsK.invariantK[Alg[*[_], A, B]]

  implicit def catsTaglessLawsIsomorphismsK10[Alg[_[_], _], A](implicit
      ev: InvariantK[Alg[*[_], A]]
  ): IsomorphismsK[Alg[*[_], A]] = IsomorphismsK.invariantK[Alg[*[_], A]]

  implicit def catsTaglessLawsIsomorphismsKForTuple2K1[G[_], A](implicit
      ev: InvariantK[Tuple2K[*[_], G, A]]
  ): IsomorphismsK[Tuple2K[*[_], G, A]] = IsomorphismsK.invariantK[Tuple2K[*[_], G, A]]

  implicit def catsTaglessLawsEqForTuple2K[F[_], G[_], A](implicit
      ev: Eq[(F[A], G[A])]
  ): Eq[Tuple2K[F, G, A]] = Eq.by(t2k => (t2k.first, t2k.second))

  implicit def catsTaglessLawsEqForEitherKTuple3K[F[_], G[_], H[_], I[_], A](implicit
      ev: Eq[Either[F[A], (G[A], H[A], I[A])]]
  ): Eq[EitherK[F, Tuple3K[G, H, I]#λ, A]] = Eq.by(_.run)

  implicit def catsTaglessLawsEqForEitherTTuple3K[F[_], G[_], H[_], A, B](implicit
      ev: Eq[(F[Either[A, B]], G[Either[A, B]], H[Either[A, B]])]
  ): Eq[EitherT[Tuple3K[F, G, H]#λ, A, B]] = Eq.by(_.value)

  implicit def catsTaglessLawsEqForFuncTuple3K[F[_], G[_], H[_], A, B](implicit
      ev: Eq[A => (F[B], G[B], H[B])]
  ): Eq[Func[Tuple3K[F, G, H]#λ, A, B]] = Eq.by(_.run)

  implicit def catsTaglessLawsEqForIdTTuple3K[F[_], G[_], H[_], A](implicit
      ev: Eq[(F[A], G[A], H[A])]
  ): Eq[IdT[Tuple3K[F, G, H]#λ, A]] = Eq.by(_.value)

  implicit def catsTaglessLawsEqForIorTTuple3K[F[_], G[_], H[_], A, B](implicit
      ev: Eq[(F[Ior[A, B]], G[Ior[A, B]], H[Ior[A, B]])]
  ): Eq[IorT[Tuple3K[F, G, H]#λ, A, B]] = Eq.by(_.value)

  implicit def catsTaglessLawsEqForKleisliTuple3K[F[_], G[_], H[_], A, B](implicit
      ev: Eq[A => (F[B], G[B], H[B])]
  ): Eq[Kleisli[Tuple3K[F, G, H]#λ, A, B]] = Eq.by(_.run)

  implicit def catsTaglessLawsEqForCokleisliTuple3K[F[_], G[_], H[_], A, B](implicit
      ev: Eq[((F[A], G[A], H[A])) => B]
  ): Eq[Cokleisli[Tuple3K[F, G, H]#λ, A, B]] = Eq.by(_.run)

  implicit def catsTaglessLawsEqForOneAndTuple3K[F[_], G[_], H[_], A](implicit
      ev: Eq[(A, (F[A], G[A], H[A]))]
  ): Eq[OneAnd[Tuple3K[F, G, H]#λ, A]] = Eq.by(x => (x.head, x.tail))

  implicit def catsTaglessLawsEqForOptionTTuple3K[F[_], G[_], H[_], A](implicit
      ev: Eq[(F[Option[A]], G[Option[A]], H[Option[A]])]
  ): Eq[OptionT[Tuple3K[F, G, H]#λ, A]] = Eq.by(_.value)

  implicit def catsTaglessLawsEqForTuple2KTuple3K1[F[_], G[_], H[_], I[_], A](implicit
      ev: Eq[((F[A], G[A], H[A]), I[A])]
  ): Eq[Tuple2K[λ[a => (F[a], G[a], H[a])], I, A]] = Eq.by(t2k => (t2k.first, t2k.second))

  implicit def catsTaglessLawsEqForTuple2KTuple3K2[F[_], G[_], H[_], I[_], A](implicit
      ev: Eq[(F[A], (G[A], H[A], I[A]))]
  ): Eq[Tuple2K[F, Tuple3K[G, H, I]#λ, A]] = Eq.by(t2k => (t2k.first, t2k.second))

  implicit def catsTaglessLawsEqForWriterTTuple3K[F[_], G[_], H[_], A, B](implicit
      ev: Eq[(F[(A, B)], G[(A, B)], H[(A, B)])]
  ): Eq[WriterT[Tuple3K[F, G, H]#λ, A, B]] = Eq.by(_.run)
}

import org.scalactic.TripleEqualsSupport.{AToBEquivalenceConstraint, BToAEquivalenceConstraint}
import org.scalactic._

// The code in this file was taken and only slightly modified from
// https://github.com/bvenners/equality-integration-demo
// Thanks for the great examples, Bill!

final class CatsEquivalence[T](T: Eq[T]) extends Equivalence[T] {
  def areEquivalent(a: T, b: T): Boolean = T.eqv(a, b)
}

trait LowPriorityStrictCatsConstraints extends TripleEquals {
  implicit def lowPriorityCatsCanEqual[A, B](implicit B: Eq[B], ev: A <:< B): CanEqual[A, B] =
    new AToBEquivalenceConstraint[A, B](new CatsEquivalence(B), ev)
}

trait StrictCatsEquality extends LowPriorityStrictCatsConstraints {
  override def convertToEqualizer[T](left: T): Equalizer[T] = super.convertToEqualizer[T](left)
  implicit override def convertToCheckingEqualizer[T](left: T): CheckingEqualizer[T] = new CheckingEqualizer(left)
  override def unconstrainedEquality[A, B](implicit equalityOfA: Equality[A]): CanEqual[A, B] =
    super.unconstrainedEquality[A, B]
  implicit def catsCanEqual[A, B](implicit A: Eq[A], ev: B <:< A): CanEqual[A, B] =
    new BToAEquivalenceConstraint[A, B](new CatsEquivalence(A), ev)
}
