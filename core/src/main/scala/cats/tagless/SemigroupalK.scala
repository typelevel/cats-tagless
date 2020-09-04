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

import cats._
import cats.data._
import cats.syntax.all._
import cats.kernel.CommutativeMonoid
import simulacrum.typeclass

@typeclass trait SemigroupalK[A[_[_]]] {
  def productK[F[_], G[_]](af: A[F], ag: A[G]): A[Tuple2K[F, G, *]]
}

object SemigroupalK extends SemigroupalKInstances {
  implicit def catsTaglessSemigroupalKForEitherK[F[_], A]: SemigroupalK[EitherK[F, *[_], A]] =
    InvariantK.catsTaglessApplyKForEitherK[F, A]

  implicit def catsTaglessSemigroupalKForEitherT[A, B]: SemigroupalK[EitherT[*[_], A, B]] =
    InvariantK.catsTaglessApplyKForEitherT[A, B]

  implicit def catsTaglessSemigroupalKForFunc[A, B]: SemigroupalK[Func[*[_], A, B]] =
    InvariantK.catsTaglessApplyKForFunc[A, B]

  implicit def catsTaglessSemigroupalKForIdT[A]: SemigroupalK[IdT[*[_], A]] =
    InvariantK.catsTaglessApplyKForIdT[A]

  implicit def catsTaglessSemigroupalKForIorT[A, B]: SemigroupalK[IorT[*[_], A, B]] =
    InvariantK.catsTaglessApplyKForIorT[A, B]

  implicit def catsTaglessSemigroupalKForKleisli[A, B]: SemigroupalK[Kleisli[*[_], A, B]] =
    InvariantK.catsTaglessApplyKForKleisli[A, B]

  implicit def catsTaglessSemigroupalKForOptionT[A]: SemigroupalK[OptionT[*[_], A]] =
    InvariantK.catsTaglessApplyKForOptionT[A]

  implicit def catsTaglessSemigroupalKForWriterT[A, B]: SemigroupalK[WriterT[*[_], A, B]] =
    InvariantK.catsTaglessApplyKForWriterT[A, B]

  implicit def catsTaglessSemigroupalKForOneAnd[A: Semigroup]: SemigroupalK[OneAnd[*[_], A]] =
    InvariantK.catsTaglessApplyKForOneAnd[A]

  implicit def catsTaglessSemigroupalKForTuple2K1[G[_]: SemigroupK, A]: SemigroupalK[Tuple2K[*[_], G, A]] =
    InvariantK.catsTaglessApplyKForTuple2K1[G, A]

  implicit def catsTaglessSemigroupalKForTuple2K2[F[_]: SemigroupK, A]: SemigroupalK[Tuple2K[F, *[_], A]] =
    InvariantK.catsTaglessApplyKForTuple2K2[F, A]
}

trait SemigroupalKInstances extends SemigroupalKInstances01 {
  implicit val catsTaglessSemigroupalKForMonad: SemigroupalK[Monad] =
    new SemigroupalK[Monad] {
      def productK[F[_], G[_]](af: Monad[F], ag: Monad[G]): Monad[Tuple2K[F, G, *]] =
        new Tup2KMonad[F, G] { val F = af; val G = ag }
    }

  implicit val catsTaglessSemigroupalKForTraverse: SemigroupalK[Traverse] =
    new SemigroupalK[Traverse] {
      def productK[F[_], G[_]](af: Traverse[F], ag: Traverse[G]): Traverse[Tuple2K[F, G, *]] =
        new Tup2KTraverse[F, G] { val F = af; val G = ag }
    }
}

trait SemigroupalKInstances01 extends SemigroupalKInstances02 {
  implicit val catsTaglessSemigroupalKForFlatMap: SemigroupalK[FlatMap] =
    new SemigroupalK[FlatMap] {
      def productK[F[_], G[_]](af: FlatMap[F], ag: FlatMap[G]): FlatMap[Tuple2K[F, G, *]] =
        new Tup2KFlatMap[F, G] { val F = af; val G = ag }
    }

  implicit val catsTaglessSemigroupalKForApplicative: SemigroupalK[Applicative] =
    new SemigroupalK[Applicative] {
      def productK[F[_], G[_]](af: Applicative[F], ag: Applicative[G]): Applicative[Tuple2K[F, G, *]] =
        new Tup2KApplicative[F, G] { val F = af; val G = ag }
    }

  implicit val catsTaglessSemigroupalKForUnorderedTraverse: SemigroupalK[UnorderedTraverse] =
    new SemigroupalK[UnorderedTraverse] {
      def productK[F[_], G[_]](af: UnorderedTraverse[F], ag: UnorderedTraverse[G]): UnorderedTraverse[Tuple2K[F, G, *]] =
        new Tup2KUnorderedTraverse[F, G] { val F = af; val G = ag }
    }

  implicit val catsTaglessSemigroupalKForFoldable: SemigroupalK[Foldable] =
    new SemigroupalK[Foldable] {
      def productK[F[_], G[_]](af: Foldable[F], ag: Foldable[G]): Foldable[Tuple2K[F, G, *]] =
        new Tup2KFoldable[F, G] { val F = af; val G = ag }
    }
}

trait SemigroupalKInstances02 extends SemigroupalKInstances03 {
  implicit val catsTaglessSemigroupalKForUnorderedFoldable: SemigroupalK[UnorderedFoldable] =
    new SemigroupalK[UnorderedFoldable] {
      def productK[F[_], G[_]](af: UnorderedFoldable[F], ag: UnorderedFoldable[G]): UnorderedFoldable[Tuple2K[F, G, *]] =
        new Tup2KUnorderedFoldable[F, G] { val F = af; val G = ag }
    }

  implicit val catsTaglessSemigroupalKForContravariantMonoidal: SemigroupalK[ContravariantMonoidal] =
    new SemigroupalK[ContravariantMonoidal] {
      def productK[F[_], G[_]](af: ContravariantMonoidal[F], ag: ContravariantMonoidal[G]): ContravariantMonoidal[Tuple2K[F, G, *]] =
        new Tup2KContravariantMonoidal[F, G] { val F = af; val G = ag }
    }

  implicit val catsTaglessSemigroupalKForApply: SemigroupalK[Apply] =
    new SemigroupalK[Apply] {
      def productK[F[_], G[_]](af: Apply[F], ag: Apply[G]): Apply[Tuple2K[F, G, *]] =
        new Tup2KApply[F, G] { val F = af; val G = ag }
    }
}

trait SemigroupalKInstances03 extends SemigroupalKInstances04 {
  implicit val catsTaglessSemigroupalKForAlternative: SemigroupalK[Alternative] =
    new SemigroupalK[Alternative] {
      def productK[F[_], G[_]](af: Alternative[F], ag: Alternative[G]): Alternative[Tuple2K[F, G, *]] =
        new Tup2KAlternative[F, G] { val F = af; val G = ag }
    }

  implicit val catsTaglessSemigroupalKForContravariantSemigroupal: SemigroupalK[ContravariantSemigroupal] =
    new SemigroupalK[ContravariantSemigroupal] {
      def productK[F[_], G[_]](af: ContravariantSemigroupal[F], ag: ContravariantSemigroupal[G]): ContravariantSemigroupal[Tuple2K[F, G, *]] =
        new Tup2KContravariantSemigroupal[F, G] { val F = af; val G = ag }
    }
}

trait SemigroupalKInstances04 extends SemigroupalKInstances05 {
  implicit val catsTaglessSemigroupalKForMonoidK: SemigroupalK[MonoidK] =
    new SemigroupalK[MonoidK] {
      def productK[F[_], G[_]](af: MonoidK[F], ag: MonoidK[G]): MonoidK[Tuple2K[F, G, *]] =
        new Tup2KMonoidK[F, G] { val F = af; val G = ag }
    }

  implicit val catsTaglessSemigroupalKForInvariantMonoidal: SemigroupalK[InvariantMonoidal] =
    new SemigroupalK[InvariantMonoidal] {
      def productK[F[_], G[_]](af: InvariantMonoidal[F], ag: InvariantMonoidal[G]): InvariantMonoidal[Tuple2K[F, G, *]] =
        new Tup2KInvariantMonoidal[F, G] { val F = af; val G = ag }
    }

  implicit val catsTaglessSemigroupalKForDistributive: SemigroupalK[Distributive] =
    new SemigroupalK[Distributive] {
      def productK[F[_], G[_]](af: Distributive[F], ag: Distributive[G]): Distributive[Tuple2K[F, G, *]] =
        new Tup2KDistributive[F, G] { val F = af; val G = ag }
    }
}

trait SemigroupalKInstances05 extends SemigroupalKInstances06 {
  implicit val catsTaglessSemigroupalKForInvariantSemigroupal: SemigroupalK[InvariantSemigroupal] =
    new SemigroupalK[InvariantSemigroupal] {
      def productK[F[_], G[_]](af: InvariantSemigroupal[F], ag: InvariantSemigroupal[G]): InvariantSemigroupal[Tuple2K[F, G, *]] =
        new Tup2KInvariantSemigroupal[F, G] { val F = af; val G = ag }
    }

  implicit val catsTaglessSemigroupalKForSemigroupK: SemigroupalK[SemigroupK] =
    new SemigroupalK[SemigroupK] {
      def productK[F[_], G[_]](af: SemigroupK[F], ag: SemigroupK[G]): SemigroupK[Tuple2K[F, G, *]] =
        new Tup2KSemigroupK[F, G] { val F = af; val G = ag }
    }
}

trait SemigroupalKInstances06 extends SemigroupalKInstances07 {
  implicit val catsTaglessSemigroupalKForFunctor: SemigroupalK[Functor] =
    new SemigroupalK[Functor] {
      def productK[F[_], G[_]](af: Functor[F], ag: Functor[G]): Functor[Tuple2K[F, G, *]] =
        new Tup2KFunctor[F, G] { val F = af; val G = ag }
    }

  implicit val catsTaglessSemigroupalKForContravariant: SemigroupalK[Contravariant] =
    new SemigroupalK[Contravariant] {
      def productK[F[_], G[_]](af: Contravariant[F], ag: Contravariant[G]): Contravariant[Tuple2K[F, G, *]] =
        new Tup2KContravariant[F, G] { val F = af; val G = ag }
    }
  
  implicit val catsTaglessSemigroupalKForSemigroupal: SemigroupalK[Semigroupal] =
    new SemigroupalK[Semigroupal] {
      def productK[F[_], G[_]](af: Semigroupal[F], ag: Semigroupal[G]): Semigroupal[Tuple2K[F, G, *]] =
        new Tup2KSemigroupal[F, G] { val F = af; val G = ag }
    }
}

trait SemigroupalKInstances07 {
  implicit val catsTaglessSemigroupalKForInvariant: SemigroupalK[Invariant] =
    new SemigroupalK[Invariant] {
      def productK[F[_], G[_]](af: Invariant[F], ag: Invariant[G]): Invariant[Tuple2K[F, G, *]] =
        new Tup2KInvariant[F, G] { val F = af; val G = ag }
    }
}

private trait Tup2KSemigroupal[F[_], G[_]] extends Semigroupal[Tuple2K[F, G, *]] {
  def F: Semigroupal[F]
  def G: Semigroupal[G]
  final override def product[A, B](fa: Tuple2K[F, G, A], fb: Tuple2K[F, G, B]): Tuple2K[F, G, (A, B)] =
    Tuple2K(F.product(fa.first, fb.first), G.product(fa.second, fb.second))
}

private trait Tup2KInvariant[F[_], G[_]] extends Invariant[Tuple2K[F, G, *]] {
  def F: Invariant[F]
  def G: Invariant[G]
  final override def imap[A, B](fa: Tuple2K[F, G, A])(f: A => B)(g: B => A): Tuple2K[F, G, B] =
    Tuple2K(F.imap(fa.first)(f)(g), G.imap(fa.second)(f)(g))
}

private trait Tup2KFunctor[F[_], G[_]] extends Functor[Tuple2K[F, G, *]] {
  def F: Functor[F]
  def G: Functor[G]
  final override def map[A, B](fa: Tuple2K[F, G, A])(f: A => B): Tuple2K[F, G, B] =
    Tuple2K(F.map(fa.first)(f), G.map(fa.second)(f))
}

private trait Tup2KContravariant[F[_], G[_]] extends Contravariant[Tuple2K[F, G, *]] {
  def F: Contravariant[F]
  def G: Contravariant[G]
  final override def contramap[A, B](fa: Tuple2K[F, G, A])(f: B => A): Tuple2K[F, G, B] =
    Tuple2K(F.contramap(fa.first)(f), G.contramap(fa.second)(f))
}

private trait Tup2KDistributive[F[_], G[_]] extends Distributive[Tuple2K[F, G, *]] with Tup2KFunctor[F, G] {
  def F: Distributive[F]
  def G: Distributive[G]
  final override def distribute[H[_]: Functor, A, B](ha: H[A])(f: A => Tuple2K[F, G, B]): Tuple2K[F, G, H[B]] =
    Tuple2K(F.distribute(ha)(f(_).first), G.distribute(ha)(f(_).second))
}

private trait Tup2KContravariantMonoidal[F[_], G[_]]
    extends ContravariantMonoidal[Tuple2K[F, G, *]] with Tup2KSemigroupal[F, G] with Tup2KContravariant[F, G] {
  def F: ContravariantMonoidal[F]
  def G: ContravariantMonoidal[G]
  final override def unit: Tuple2K[F, G, Unit] = Tuple2K(F.unit, G.unit)
}

private trait Tup2KApply[F[_], G[_]]
    extends Apply[Tuple2K[F, G, *]] with Tup2KFunctor[F, G] with Tup2KSemigroupal[F, G] {
  def F: Apply[F]
  def G: Apply[G]
  final override def ap[A, B](f: Tuple2K[F, G, A => B])(fa: Tuple2K[F, G, A]): Tuple2K[F, G, B] =
    Tuple2K(F.ap(f.first)(fa.first), G.ap(f.second)(fa.second))
  final override def map2Eval[A, B, Z](fa: Tuple2K[F, G, A], fb: Eval[Tuple2K[F, G, B]])(
    f: (A, B) => Z
  ): Eval[Tuple2K[F, G, Z]] = {
    val fbMemo = fb.memoize // don't recompute this twice internally
    for {
      fz <- F.map2Eval(fa.first, fbMemo.map(_.first))(f)
      gz <- G.map2Eval(fa.second, fbMemo.map(_.second))(f)
    } yield Tuple2K(fz, gz)
  }
}

private trait Tup2KContravariantSemigroupal[F[_], G[_]]
    extends ContravariantSemigroupal[Tuple2K[F, G, *]] with Tup2KSemigroupal[F, G] with Tup2KContravariant[F, G] {
  def F: ContravariantSemigroupal[F]
  def G: ContravariantSemigroupal[G]
}

private trait Tup2KInvariantSemigroupal[F[_], G[_]]
    extends InvariantSemigroupal[Tuple2K[F, G, *]] with Tup2KSemigroupal[F, G] with Tup2KInvariant[F, G] {
  def F: InvariantSemigroupal[F]
  def G: InvariantSemigroupal[G]
}

private trait Tup2KInvariantMonoidal[F[_], G[_]]
    extends InvariantMonoidal[Tuple2K[F, G, *]] with Tup2KSemigroupal[F, G] with Tup2KInvariant[F, G] {
  def F: InvariantMonoidal[F]
  def G: InvariantMonoidal[G]
  final override def unit: Tuple2K[F, G, Unit] = Tuple2K[F, G, Unit](F.unit, G.unit)
}

private trait Tup2KApplicative[F[_], G[_]] extends Applicative[Tuple2K[F, G, *]] with Tup2KApply[F, G] {
  def F: Applicative[F]
  def G: Applicative[G]
  final override def pure[A](a: A): Tuple2K[F, G, A] = Tuple2K(F.pure(a), G.pure(a))
}

private trait Tup2KSemigroupK[F[_], G[_]] extends SemigroupK[Tuple2K[F, G, *]] {
  def F: SemigroupK[F]
  def G: SemigroupK[G]
  final override def combineK[A](x: Tuple2K[F, G, A], y: Tuple2K[F, G, A]): Tuple2K[F, G, A] =
    Tuple2K(F.combineK(x.first, y.first), G.combineK(x.second, y.second))
}

private trait Tup2KMonoidK[F[_], G[_]] extends MonoidK[Tuple2K[F, G, *]] with Tup2KSemigroupK[F, G] {
  def F: MonoidK[F]
  def G: MonoidK[G]
  final override def empty[A]: Tuple2K[F, G, A] = Tuple2K(F.empty[A], G.empty[A])
}

private trait Tup2KAlternative[F[_], G[_]]
    extends Alternative[Tuple2K[F, G, *]] with Tup2KApplicative[F, G] with Tup2KMonoidK[F, G] {
  def F: Alternative[F]
  def G: Alternative[G]
}

private trait Tup2KFlatMap[F[_], G[_]] extends FlatMap[Tuple2K[F, G, *]] with Tup2KApply[F, G] {
  def F: FlatMap[F]
  def G: FlatMap[G]
  final override def flatMap[A, B](fa: Tuple2K[F, G, A])(f: A => Tuple2K[F, G, B]): Tuple2K[F, G, B] =
    Tuple2K(F.flatMap(fa.first)(f(_).first), G.flatMap(fa.second)(f(_).second))
  final override def tailRecM[A, B](a: A)(f: A => Tuple2K[F, G, Either[A, B]]): Tuple2K[F, G, B] =
    Tuple2K(F.tailRecM(a)(f(_).first), G.tailRecM(a)(f(_).second))
}

private trait Tup2KMonad[F[_], G[_]]
    extends Monad[Tuple2K[F, G, *]] with Tup2KApplicative[F, G] with Tup2KFlatMap[F, G] {
  def F: Monad[F]
  def G: Monad[G]
}

private trait Tup2KFoldable[F[_], G[_]] extends Foldable[Tuple2K[F, G, *]] {
  def F: Foldable[F]
  def G: Foldable[G]
  final override def foldLeft[A, B](fa: Tuple2K[F, G, A], b: B)(f: (B, A) => B): B =
    G.foldLeft(fa.second, F.foldLeft(fa.first, b)(f))(f)
  final override def foldRight[A, B](fa: Tuple2K[F, G, A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] =
    F.foldRight(fa.first, G.foldRight(fa.second, lb)(f))(f)
}

private trait Tup2KUnorderedFoldable[F[_], G[_]] extends UnorderedFoldable[Tuple2K[F, G, *]] {
  def F: UnorderedFoldable[F]
  def G: UnorderedFoldable[G]
  final override def unorderedFoldMap[A, B](fa: Tuple2K[F, G, A])(f: A => B)(implicit M: CommutativeMonoid[B]): B =
    F.unorderedFoldMap(fa.first)(f) |+| G.unorderedFoldMap(fa.second)(f)
}

private trait Tup2KTraverse[F[_], G[_]]
  extends Traverse[Tuple2K[F, G, *]] with Tup2KFoldable[F, G] {
  def F: Traverse[F]
  def G: Traverse[G]
  final override def traverse[H[_], A, B](fa: Tuple2K[F, G, A])(f: A => H[B])(
    implicit H: Applicative[H]
  ): H[Tuple2K[F, G, B]] =
    H.map2(F.traverse(fa.first)(f), G.traverse(fa.second)(f))(Tuple2K.apply)
}

private trait Tup2KUnorderedTraverse[F[_], G[_]]
  extends UnorderedTraverse[Tuple2K[F, G, *]] with Tup2KUnorderedFoldable[F, G] {
  def F: UnorderedTraverse[F]
  def G: UnorderedTraverse[G]
  final override def unorderedTraverse[H[_], A, B](fa: Tuple2K[F, G, A])(f: A => H[B])(
    implicit H: CommutativeApplicative[H]
  ): H[Tuple2K[F, G, B]] =
    H.map2(F.unorderedTraverse(fa.first)(f), G.unorderedTraverse(fa.second)(f))(Tuple2K.apply)
}
