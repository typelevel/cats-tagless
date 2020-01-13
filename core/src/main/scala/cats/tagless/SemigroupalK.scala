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

import simulacrum.typeclass
import cats.data._
import cats.implicits._
import cats.kernel.CommutativeMonoid
import cats.{Alternative, Applicative, Apply, CommutativeApplicative, Contravariant, ContravariantMonoidal, ContravariantSemigroupal, Distributive, Eval, FlatMap, Foldable, Functor, Invariant, InvariantMonoidal, InvariantSemigroupal, Monad, MonoidK, Semigroup, SemigroupK, Semigroupal, Traverse, UnorderedFoldable, UnorderedTraverse}

@typeclass trait SemigroupalK[A[_[_]]] {
  def productK[F[_], G[_]](af: A[F], ag: A[G]): A[Tuple2K[F, G, *]]
}

import cats.tagless.SemigroupalKTraits._

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

  implicit def catsTaglessSemigroupalKForTuple2K[H[_]: SemigroupK, A]: SemigroupalK[Tuple2K[H, *[_], A]] =
    InvariantK.catsTaglessApplyKForTuple2K[H, A]

}

trait SemigroupalKInstances extends SemigroupalKInstances01 {
  implicit def semKMonad:         SemigroupalK[Monad]         = new SemKMonad {}
  implicit def semKTraverse:      SemigroupalK[Traverse]      = new SemKTraverse {}
}
trait SemigroupalKInstances01 extends SemigroupalKInstances02 {
  implicit def semKFlatMap:       SemigroupalK[FlatMap]       = new SemKFlatMap {}
  implicit def semKApplicative:   SemigroupalK[Applicative]   = new SemKApplicative {}
  implicit def semKUnorderedTraverse: SemigroupalK[UnorderedTraverse] = new SemKUnorderedTraverse {}
}
trait SemigroupalKInstances02 extends SemigroupalKInstances03 {
  implicit def semKUnorderedFoldable: SemigroupalK[UnorderedFoldable] = new SemKUnorderedFoldable {}
  implicit def semKContravariantMonoidal: SemigroupalK[ContravariantMonoidal] = new SemKContravariantMonoidal {}
  implicit def semKApply:         SemigroupalK[Apply]         = new SemKApply {}
}
trait SemigroupalKInstances03 extends SemigroupalKInstances04 {
  implicit def semKAlternative:   SemigroupalK[Alternative]   = new SemKAlternative {}
  implicit def semKContravariantSemigroupal: SemigroupalK[ContravariantSemigroupal] = new SemKContravariantSemigroupal {}
}
trait SemigroupalKInstances04 extends SemigroupalKInstances05 {
  implicit def semKMonoidK:       SemigroupalK[MonoidK]       = new SemKMonoidK {}
  implicit def semKInvariantMonoidal: SemigroupalK[InvariantMonoidal] = new SemKInvariantMonoidal {}
  implicit def semKDistributive:  SemigroupalK[Distributive]  = new SemKDistributive {}
}
trait SemigroupalKInstances05 extends SemigroupalKInstances06 {
  implicit def semKInvariantSemigroupal: SemigroupalK[InvariantSemigroupal] = new SemKInvariantSemigroupal {}
  implicit def semKSemigroupK:    SemigroupalK[SemigroupK]    = new SemKSemigroupK {}
}
trait SemigroupalKInstances06 extends SemigroupalKInstances07 {
  implicit def semKFunctor:       SemigroupalK[Functor]       = new SemKFunctor {}
  implicit def semKContravariant: SemigroupalK[Contravariant] = new SemKContravariant {}
  implicit def semKSemigroupal:   SemigroupalK[Semigroupal]   = new SemKSemigroupal {}
}
trait SemigroupalKInstances07 extends SemigroupalKInstances08 {
  implicit def semKInvariant:     SemigroupalK[Invariant]     = new SemKInvariant {}
}
trait SemigroupalKInstances08 extends SemigroupalKInstances09
trait SemigroupalKInstances09

object SemigroupalKTraits extends SemigroupalKTraits

trait SemigroupalKTraits extends SemigroupalKTraitsPrivate {

  trait SemKSemigroupal extends SemigroupalK[Semigroupal] {
    def productK[F[_], G[_]](af: Semigroupal[F], ag: Semigroupal[G]): Semigroupal[Tuple2K[F, G, ?]] =
      new Tup2KSemigroupal[F, G] { val F = af; val G = ag }
  }
  trait SemKInvariant extends SemigroupalK[Invariant] {
    def productK[F[_], G[_]](af: Invariant[F], ag: Invariant[G]): Invariant[Tuple2K[F, G, ?]] =
      new Tup2KInvariant[F, G] { val F = af; val G = ag }
  }
  trait SemKFunctor extends SemigroupalK[Functor] {
    def productK[F[_], G[_]](af: Functor[F], ag: Functor[G]): Functor[Tuple2K[F, G, ?]] =
      new Tup2KFunctor[F, G] { val F = af; val G = ag }
  }
  trait SemKDistributive extends SemigroupalK[Distributive] {
    def productK[F[_], G[_]](af: Distributive[F], ag: Distributive[G]): Distributive[Tuple2K[F, G, ?]] =
      new Tup2KDistributive[F, G] { val F = af; val G = ag }
  }
  trait SemKContravariant extends SemigroupalK[Contravariant] {
    def productK[F[_], G[_]](af: Contravariant[F], ag: Contravariant[G]): Contravariant[Tuple2K[F, G, ?]] =
      new Tup2KContravariant[F, G] { val F = af; val G = ag }
  }
  trait SemKContravariantSemigroupal extends SemigroupalK[ContravariantSemigroupal] {
    def productK[F[_], G[_]](af: ContravariantSemigroupal[F], ag: ContravariantSemigroupal[G]): ContravariantSemigroupal[Tuple2K[F, G, ?]] =
      new Tup2KContravariantSemigroupal[F, G] { val F = af; val G = ag }
  }
  trait SemKInvariantSemigroupal extends SemigroupalK[InvariantSemigroupal] {
    def productK[F[_], G[_]](af: InvariantSemigroupal[F], ag: InvariantSemigroupal[G]): InvariantSemigroupal[Tuple2K[F, G, ?]] =
      new Tup2KInvariantSemigroupal[F, G] { val F = af; val G = ag }
  }
  trait SemKInvariantMonoidal extends SemigroupalK[InvariantMonoidal] {
    def productK[F[_], G[_]](af: InvariantMonoidal[F], ag: InvariantMonoidal[G]): InvariantMonoidal[Tuple2K[F, G, ?]] =
      new Tup2KInvariantMonoidal[F, G] { val F = af; val G = ag }
  }
  trait SemKContravariantMonoidal extends SemigroupalK[ContravariantMonoidal] {
    def productK[F[_], G[_]](af: ContravariantMonoidal[F], ag: ContravariantMonoidal[G]): ContravariantMonoidal[Tuple2K[F, G, ?]] =
      new Tup2KContravariantMonoidal[F, G] { val F = af; val G = ag }
  }
  trait SemKApply extends SemigroupalK[Apply] {
    def productK[F[_], G[_]](af: Apply[F], ag: Apply[G]): Apply[Tuple2K[F, G, ?]] =
      new Tup2KApply[F, G] { val F = af; val G = ag }
  }
  trait SemKApplicative extends SemigroupalK[Applicative] {
    def productK[F[_], G[_]](af: Applicative[F], ag: Applicative[G]): Applicative[Tuple2K[F, G, ?]] =
      new Tup2KApplicative[F, G] { val F = af; val G = ag }
  }
  trait SemKSemigroupK extends SemigroupalK[SemigroupK] {
    def productK[F[_], G[_]](af: SemigroupK[F], ag: SemigroupK[G]): SemigroupK[Tuple2K[F, G, ?]] =
      new Tup2KSemigroupK[F, G] { val F = af; val G = ag }
  }
  trait SemKMonoidK extends SemigroupalK[MonoidK] {
    def productK[F[_], G[_]](af: MonoidK[F], ag: MonoidK[G]): MonoidK[Tuple2K[F, G, ?]] =
      new Tup2KMonoidK[F, G] { val F = af; val G = ag }
  }
  trait SemKAlternative extends SemigroupalK[Alternative] {
    def productK[F[_], G[_]](af: Alternative[F], ag: Alternative[G]): Alternative[Tuple2K[F, G, ?]] =
      new Tup2KAlternative[F, G] { val F = af; val G = ag }
  }
  trait SemKMonad extends SemigroupalK[Monad] {
    def productK[F[_], G[_]](af: Monad[F], ag: Monad[G]): Monad[Tuple2K[F, G, ?]] =
      new Tup2KMonad[F, G] { val F = af; val G = ag }
  }
  trait SemKFlatMap extends SemigroupalK[FlatMap] {
    def productK[F[_], G[_]](af: FlatMap[F], ag: FlatMap[G]): FlatMap[Tuple2K[F, G, ?]] =
      new Tup2KFlatMap[F, G] { val F = af; val G = ag }
  }
  trait SemKFoldable extends SemigroupalK[Foldable] {
    def productK[F[_], G[_]](af: Foldable[F], ag: Foldable[G]): Foldable[Tuple2K[F, G, ?]] =
      new Tup2KFoldable[F, G] { val F = af; val G = ag }
  }
  trait SemKUnorderedFoldable extends SemigroupalK[UnorderedFoldable] {
    def productK[F[_], G[_]](af: UnorderedFoldable[F], ag: UnorderedFoldable[G]): UnorderedFoldable[Tuple2K[F, G, ?]] =
      new Tup2KUnorderedFoldable[F, G] { val F = af; val G = ag }
  }
  trait SemKTraverse extends SemigroupalK[Traverse] {
    def productK[F[_], G[_]](af: Traverse[F], ag: Traverse[G]): Traverse[Tuple2K[F, G, ?]] =
      new Tup2KTraverse[F, G] { val F = af; val G = ag }
  }
  trait SemKUnorderedTraverse extends SemigroupalK[UnorderedTraverse] {
    def productK[F[_], G[_]](af: UnorderedTraverse[F], ag: UnorderedTraverse[G]): UnorderedTraverse[Tuple2K[F, G, ?]] =
      new Tup2KUnorderedTraverse[F, G] { val F = af; val G = ag }
  }
}

private[tagless] trait SemigroupalKTraitsPrivate {

  trait Tup2KSemigroupal[F[_], G[_]] extends Semigroupal[Tuple2K[F, G, ?]] {
    def F: Semigroupal[F]
    def G: Semigroupal[G]
    def product[A, B](fa: Tuple2K[F, G, A], fb: Tuple2K[F, G, B]): Tuple2K[F, G, (A, B)] =
      Tuple2K(F.product(fa.first, fb.first), G.product(fa.second, fb.second))
  }

  trait Tup2KInvariant[F[_], G[_]] extends Invariant[λ[α => Tuple2K[F, G, α]]] {
    def F: Invariant[F]
    def G: Invariant[G]
    override def imap[A, B](fa: Tuple2K[F, G, A])(f: A => B)(g: B => A): Tuple2K[F, G, B] =
      Tuple2K(F.imap(fa.first)(f)(g), G.imap(fa.second)(f)(g))
  }

  trait Tup2KFunctor[F[_], G[_]] extends Functor[λ[α => Tuple2K[F, G, α]]] {
    def F: Functor[F]
    def G: Functor[G]
    override def map[A, B](fa: Tuple2K[F, G, A])(f: A => B): Tuple2K[F, G, B] =
      Tuple2K(F.map(fa.first)(f), G.map(fa.second)(f))
  }

  trait Tup2KContravariant[F[_], G[_]] extends Contravariant[λ[α => Tuple2K[F, G, α]]] {
    def F: Contravariant[F]
    def G: Contravariant[G]
    override def contramap[A, B](fa: Tuple2K[F, G, A])(f: B => A): Tuple2K[F, G, B] =
      Tuple2K(F.contramap(fa.first)(f), G.contramap(fa.second)(f))
  }

  trait Tup2KDistributive[F[_], G[_]] extends Distributive[λ[α => Tuple2K[F, G, α]]] with Tup2KFunctor[F, G] {
    def F: Distributive[F]
    def G: Distributive[G]
    override def distribute[H[_]: Functor, A, B](ha: H[A])(f: A => Tuple2K[F, G, B]): Tuple2K[F, G, H[B]] =
      Tuple2K(F.distribute(ha)(a => f(a).first), G.distribute(ha)(a => f(a).second))
  }

  trait Tup2KContravariantMonoidal[F[_], G[_]]
    extends ContravariantMonoidal[λ[α => Tuple2K[F, G, α]]] with Tup2KSemigroupal[F, G] with Tup2KContravariant[F, G] {
    def F: ContravariantMonoidal[F]
    def G: ContravariantMonoidal[G]

    def unit: Tuple2K[F, G, Unit] = Tuple2K(F.unit, G.unit)
  }

  trait Tup2KApply[F[_], G[_]] extends Apply[λ[α => Tuple2K[F, G, α]]] with Tup2KFunctor[F, G] {
    def F: Apply[F]
    def G: Apply[G]
    override def ap[A, B](f: Tuple2K[F, G, A => B])(fa: Tuple2K[F, G, A]): Tuple2K[F, G, B] =
      Tuple2K(F.ap(f.first)(fa.first), G.ap(f.second)(fa.second))
    override def product[A, B](fa: Tuple2K[F, G, A], fb: Tuple2K[F, G, B]): Tuple2K[F, G, (A, B)] =
      Tuple2K(F.product(fa.first, fb.first), G.product(fa.second, fb.second))
    override def map2Eval[A, B, Z](fa: Tuple2K[F, G, A], fb: Eval[Tuple2K[F, G, B]])(f: (A, B) => Z)
    : Eval[Tuple2K[F, G, Z]] = {
      val fbmemo = fb.memoize // don't recompute this twice internally
      for {
        fz <- F.map2Eval(fa.first, fbmemo.map(_.first))(f)
        gz <- G.map2Eval(fa.second, fbmemo.map(_.second))(f)
      } yield Tuple2K(fz, gz)
    }
  }

  trait Tup2KContravariantSemigroupal[F[_], G[_]]
    extends ContravariantSemigroupal[λ[α => Tuple2K[F, G, α]]] with Tup2KSemigroupal[F, G] with Tup2KContravariant[F, G] {
    def F: ContravariantSemigroupal[F]
    def G: ContravariantSemigroupal[G]
  }

  trait Tup2KInvariantSemigroupal[F[_], G[_]]
    extends InvariantSemigroupal[λ[α => Tuple2K[F, G, α]]] with Tup2KSemigroupal[F, G] with Tup2KInvariant[F, G] {
    def F: InvariantSemigroupal[F]
    def G: InvariantSemigroupal[G]
  }

  trait Tup2KInvariantMonoidal[F[_], G[_]]
    extends InvariantMonoidal[λ[α => Tuple2K[F, G, α]]] with Tup2KSemigroupal[F, G] with Tup2KInvariant[F, G] {
    def F: InvariantMonoidal[F]
    def G: InvariantMonoidal[G]
    override def unit: Tuple2K[F, G, Unit] = Tuple2K[F, G, Unit](F.unit, G.unit)
  }

  trait Tup2KApplicative[F[_], G[_]]
    extends Applicative[λ[α => Tuple2K[F, G, α]]]
      with Tup2KApply[F, G] {
    def F: Applicative[F]
    def G: Applicative[G]
    def pure[A](a: A): Tuple2K[F, G, A] = Tuple2K(F.pure(a), G.pure(a))
  }

  trait Tup2KSemigroupK[F[_], G[_]] extends SemigroupK[λ[α => Tuple2K[F, G, α]]] {
    def F: SemigroupK[F]
    def G: SemigroupK[G]
    override def combineK[A](x: Tuple2K[F, G, A], y: Tuple2K[F, G, A]): Tuple2K[F, G, A] =
      Tuple2K(F.combineK(x.first, y.first), G.combineK(x.second, y.second))
  }

  trait Tup2KMonoidK[F[_], G[_]]
    extends MonoidK[λ[α => Tuple2K[F, G, α]]]
      with Tup2KSemigroupK[F, G] {
    def F: MonoidK[F]
    def G: MonoidK[G]
    override def empty[A]: Tuple2K[F, G, A] =
      Tuple2K(F.empty[A], G.empty[A])
  }

  trait Tup2KAlternative[F[_], G[_]]
    extends Alternative[λ[α => Tuple2K[F, G, α]]]
      with Tup2KApplicative[F, G]
      with Tup2KMonoidK[F, G] {
    def F: Alternative[F]
    def G: Alternative[G]
  }

  trait Tup2KFlatMap[F[_], G[_]]
    extends FlatMap[λ[α => Tuple2K[F, G, α]]] with Tup2KApply[F, G] {
    def F: FlatMap[F]
    def G: FlatMap[G]

    def flatMap[A, B](fa: Tuple2K[F, G, A])(f: A => Tuple2K[F, G, B]): Tuple2K[F, G, B] =
      Tuple2K(F.flatMap(fa.first)(f(_).first), G.flatMap(fa.second)(f(_).second))

    def tailRecM[A, B](a: A)(f: A => Tuple2K[F, G, Either[A, B]]): Tuple2K[F, G, B] =
      Tuple2K(F.tailRecM(a)(f(_).first), G.tailRecM(a)(f(_).second))
  }

  trait Tup2KMonad[F[_], G[_]]
    extends Monad[λ[α => Tuple2K[F, G, α]]]
      with Tup2KApplicative[F, G] with Tup2KFlatMap[F, G] {
    def F: Monad[F]
    def G: Monad[G]
    override def pure[A](a: A): Tuple2K[F, G, A] =
      Tuple2K(F.pure(a), G.pure(a))
  }

  trait Tup2KFoldable[F[_], G[_]] extends Foldable[λ[α => Tuple2K[F, G, α]]] {
    def F: Foldable[F]
    def G: Foldable[G]

    override def foldLeft[A, B](fa: Tuple2K[F, G, A], b: B)(f: (B, A) => B): B =
      G.foldLeft(fa.second, F.foldLeft(fa.first, b)(f))(f)

    override def foldRight[A, B](fa: Tuple2K[F, G, A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] =
      F.foldRight(fa.first, G.foldRight(fa.second, lb)(f))(f)
  }

  trait Tup2KUnorderedFoldable[F[_], G[_]] extends UnorderedFoldable[λ[α => Tuple2K[F, G, α]]] {
    def F: UnorderedFoldable[F]
    def G: UnorderedFoldable[G]

    def unorderedFoldMap[A, B](fa: Tuple2K[F, G, A])(f: A => B)(implicit M: CommutativeMonoid[B]): B =
      F.unorderedFoldMap(fa.first)(f) |+| G.unorderedFoldMap(fa.second)(f)
  }

  trait Tup2KTraverse[F[_], G[_]]
    extends Traverse[λ[α => Tuple2K[F, G, α]]] with Tup2KFoldable[F, G] {
    def F: Traverse[F]
    def G: Traverse[G]

    override def traverse[H[_], A, B](fa: Tuple2K[F, G, A])(f: A => H[B])(implicit H: Applicative[H])
    : H[Tuple2K[F, G, B]] =
      H.map2(F.traverse(fa.first)(f), G.traverse(fa.second)(f))(Tuple2K(_, _))
  }

  trait Tup2KUnorderedTraverse[F[_], G[_]]
    extends UnorderedTraverse[λ[α => Tuple2K[F, G, α]]] with Tup2KUnorderedFoldable[F, G] {
    def F: UnorderedTraverse[F]
    def G: UnorderedTraverse[G]

    def unorderedTraverse[H[_], A, B](fa: Tuple2K[F, G, A])(f: A => H[B])
      (implicit H: CommutativeApplicative[H])
    : H[Tuple2K[F, G, B]] =
      H.map2(F.unorderedTraverse(fa.first)(f), G.unorderedTraverse(fa.second)(f))(Tuple2K(_, _))
  }

}