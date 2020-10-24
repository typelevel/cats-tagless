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
import cats.arrow.FunctionK
import cats.data._
import cats.kernel.CommutativeMonoid
import simulacrum.typeclass

@typeclass trait InvariantK[A[_[_]]] {
  def imapK[F[_], G[_]](af: A[F])(fk: F ~> G)(gK: G ~> F): A[G]
}

object InvariantK extends InvariantKInstances01 {
  implicit def catsTaglessApplyKForEitherK[F[_], A]: ApplyK[EitherK[F, *[_], A]] =
    eitherKInstance.asInstanceOf[ApplyK[EitherK[F, *[_], A]]]

  implicit def catsTaglessApplyKForEitherT[A, B]: ApplyK[EitherT[*[_], A, B]] =
    eitherTInstance.asInstanceOf[ApplyK[EitherT[*[_], A, B]]]

  implicit def catsTaglessApplyKForFunc[A, B]: ApplyK[Func[*[_], A, B]] =
    funcInstance.asInstanceOf[ApplyK[Func[*[_], A, B]]]

  implicit def catsTaglessApplyKForIdT[A]: ApplyK[IdT[*[_], A]] =
    idTInstance.asInstanceOf[ApplyK[IdT[*[_], A]]]

  implicit def catsTaglessApplyKForIorT[A, B]: ApplyK[IorT[*[_], A, B]] =
    iOrTInstance.asInstanceOf[ApplyK[IorT[*[_], A, B]]]

  implicit def catsTaglessApplyKForKleisli[A, B]: ApplyK[Kleisli[*[_], A, B]] =
    kleisliInstance.asInstanceOf[ApplyK[Kleisli[*[_], A, B]]]

  implicit def catsTaglessApplyKForOptionT[A]: ApplyK[OptionT[*[_], A]] =
    optionTInstance.asInstanceOf[ApplyK[OptionT[*[_], A]]]

  implicit def catsTaglessApplyKForWriterT[A, B]: ApplyK[WriterT[*[_], A, B]] =
    writerTInstance.asInstanceOf[ApplyK[WriterT[*[_], A, B]]]

  implicit def catsTaglessContravariantKForCokleisli[A, B]: ContravariantK[Cokleisli[*[_], A, B]] =
    cokleisliInstance.asInstanceOf[ContravariantK[Cokleisli[*[_], A, B]]]

  implicit def catsTaglessFunctorKForNested[G[_], A]: FunctorK[Nested[*[_], G, A]] =
    nestedInstance.asInstanceOf[FunctorK[Nested[*[_], G, A]]]

  implicit def catsTaglessApplyKForOneAnd[A](implicit A: Semigroup[A]): ApplyK[OneAnd[*[_], A]] =
    new ApplyK[OneAnd[*[_], A]] {
      def mapK[F[_], G[_]](af: OneAnd[F, A])(fk: F ~> G) = af.mapK(fk)
      def productK[F[_], G[_]](af: OneAnd[F, A], ag: OneAnd[G, A]) =
        OneAnd(A.combine(af.head, ag.head), Tuple2K(af.tail, ag.tail))
    }

  implicit def catsTaglessApplyKForTuple2K1[H[_], A](implicit H: SemigroupK[H]): ApplyK[Tuple2K[*[_], H, A]] =
    new ApplyK[Tuple2K[*[_], H, A]] {
      def mapK[F[_], G[_]](af: Tuple2K[F, H, A])(fk: F ~> G) = Tuple2K(fk(af.first), af.second)
      def productK[F[_], G[_]](af: Tuple2K[F, H, A], ag: Tuple2K[G, H, A]): Tuple2K[Tuple2K[F, G, *], H, A] =
        Tuple2K(Tuple2K(af.first, ag.first), H.combineK(af.second, ag.second))
    }

  implicit def catsTaglessApplyKForTuple2K2[H[_], A](implicit H: SemigroupK[H]): ApplyK[Tuple2K[H, *[_], A]] =
    new ApplyK[Tuple2K[H, *[_], A]] {
      def mapK[F[_], G[_]](af: Tuple2K[H, F, A])(fk: F ~> G) = af.mapK(fk)
      def productK[F[_], G[_]](af: Tuple2K[H, F, A], ag: Tuple2K[H, G, A]) =
        Tuple2K(H.combineK(af.first, ag.first), Tuple2K(af.second, ag.second))
    }

  implicit def catsTaglessContravariantForKFunctionK[G[_]]: ContravariantK[FunctionK[*[_], G]] =
    functionKContravariantK.asInstanceOf[ContravariantK[FunctionK[*[_], G]]]

  implicit def catsTaglessFunctorKForFunctionK[F[_]]: FunctorK[FunctionK[F, *[_]]] =
    functionKFunctorK.asInstanceOf[FunctorK[FunctionK[F, *[_]]]]

  implicit def catsTaglessContravariantKForFoldable: ContravariantK[Foldable] =
    new ContravariantK[Foldable] {
      override def contramapK[F[_], G[_]](af: Foldable[F])(gf: G ~> F): Foldable[G] =
        new ContraFoldable[F, G] { val F = af; val from = gf }
    }

  implicit def catsTaglessContravariantKForUnorderedFoldable: ContravariantK[UnorderedFoldable] =
    new ContravariantK[UnorderedFoldable] {
      override def contramapK[F[_], G[_]](af: UnorderedFoldable[F])(gf: G ~> F): UnorderedFoldable[G] =
        new ContraUnorderedFoldable[F, G] { val F = af; val from = gf }
    }

  private val eitherKInstance: ApplyK[EitherK[Any, *[_], Any]] = new ApplyK[EitherK[Any, *[_], Any]] {
    def mapK[F[_], G[_]](af: EitherK[Any, F, Any])(fk: F ~> G) = af.mapK(fk)
    def productK[F[_], G[_]](af: EitherK[Any, F, Any], ag: EitherK[Any, G, Any]) =
      (af.run, ag.run) match {
        case (Left(x), _) => EitherK.leftc[Any, Tuple2K[F, G, *], Any](x)
        case (_, Left(y)) => EitherK.leftc[Any, Tuple2K[F, G, *], Any](y)
        case (Right(fa), Right(ga)) => EitherK.rightc[Any, Tuple2K[F, G, *], Any](Tuple2K(fa, ga))
      }
  }

  private val eitherTInstance: ApplyK[EitherT[*[_], Any, Any]] = new ApplyK[EitherT[*[_], Any, Any]] {
    def mapK[F[_], G[_]](af: EitherT[F, Any, Any])(fk: F ~> G) = af.mapK(fk)
    def productK[F[_], G[_]](af: EitherT[F, Any, Any], ag: EitherT[G, Any, Any]) =
      EitherT(Tuple2K(af.value, ag.value))
  }

  private val funcInstance: ApplyK[Func[*[_], Any, Any]] = new ApplyK[Func[*[_], Any, Any]] {
    def mapK[F[_], G[_]](af: Func[F, Any, Any])(fk: F ~> G) = af.mapK(fk)
    def productK[F[_], G[_]](af: Func[F, Any, Any], ag: Func[G, Any, Any]) =
      Func.func(x => Tuple2K(af.run(x), ag.run(x)))
  }

  private val idTInstance: ApplyK[IdT[*[_], Any]] = new ApplyK[IdT[*[_], Any]] {
    def mapK[F[_], G[_]](af: IdT[F, Any])(fk: F ~> G) = af.mapK(fk)
    def productK[F[_], G[_]](af: IdT[F, Any], ag: IdT[G, Any]) =
      IdT(Tuple2K(af.value, ag.value))
  }

  private val iOrTInstance: ApplyK[IorT[*[_], Any, Any]] = new ApplyK[IorT[*[_], Any, Any]] {
    def mapK[F[_], G[_]](af: IorT[F, Any, Any])(fk: F ~> G) = af.mapK(fk)
    def productK[F[_], G[_]](af: IorT[F, Any, Any], ag: IorT[G, Any, Any]) =
      IorT(Tuple2K(af.value, ag.value))
  }

  private val kleisliInstance: ApplyK[Kleisli[*[_], Any, Any]] = new ApplyK[Kleisli[*[_], Any, Any]] {
    def mapK[F[_], G[_]](af: Kleisli[F, Any, Any])(fk: F ~> G) = af.mapK(fk)
    def productK[F[_], G[_]](af: Kleisli[F, Any, Any], ag: Kleisli[G, Any, Any]) =
      Kleisli(x => Tuple2K(af.run(x), ag.run(x)))
  }

  private val optionTInstance: ApplyK[OptionT[*[_], Any]] = new ApplyK[OptionT[*[_], Any]] {
    def mapK[F[_], G[_]](af: OptionT[F, Any])(fk: F ~> G) = af.mapK(fk)
    def productK[F[_], G[_]](af: OptionT[F, Any], ag: OptionT[G, Any]) =
      OptionT(Tuple2K(af.value, ag.value))
  }

  private val writerTInstance: ApplyK[WriterT[*[_], Any, Any]] = new ApplyK[WriterT[*[_], Any, Any]] {
    def mapK[F[_], G[_]](af: WriterT[F, Any, Any])(fk: F ~> G) = af.mapK(fk)
    def productK[F[_], G[_]](af: WriterT[F, Any, Any], ag: WriterT[G, Any, Any]) =
      WriterT(Tuple2K(af.run, ag.run))
  }

  private val cokleisliInstance: ContravariantK[Cokleisli[*[_], Any, Any]] =
    new ContravariantK[Cokleisli[*[_], Any, Any]] {
      def contramapK[F[_], G[_]](af: Cokleisli[F, Any, Any])(fk: G ~> F) = Cokleisli(ga => af.run(fk(ga)))
    }

  private val nestedInstance: FunctorK[Nested[*[_], Any, Any]] = new FunctorK[Nested[*[_], Any, Any]] {
    def mapK[F[_], G[_]](af: Nested[F, Any, Any])(fk: F ~> G) = af.mapK(fk)
  }

  private val functionKContravariantK: ContravariantK[FunctionK[*[_], Any]] =
    new ContravariantK[FunctionK[*[_], Any]] {
      def contramapK[F[_], G[_]](af: F ~> Any)(fk: G ~> F) = af.compose(fk)
    }

  private val functionKFunctorK: FunctorK[FunctionK[Any, *[_]]] =
    new FunctorK[FunctionK[Any, *[_]]] {
      def mapK[F[_], G[_]](af: Any ~> F)(fn: F ~> G) = af.andThen(fn)
    }
}

sealed private[tagless] trait InvariantKInstances01 {
  implicit def catsTaglessFunctorKForOneAnd[A]: FunctorK[OneAnd[*[_], A]] =
    oneAndFunctorK.asInstanceOf[FunctorK[OneAnd[*[_], A]]]

  implicit def catsTaglessFunctorKForTuple2K[F[_], A]: FunctorK[Tuple2K[F, *[_], A]] =
    tuple2KFunctorK.asInstanceOf[FunctorK[Tuple2K[F, *[_], A]]]

  private val oneAndFunctorK: FunctorK[OneAnd[*[_], Any]] = new FunctorK[OneAnd[*[_], Any]] {
    def mapK[F[_], G[_]](af: OneAnd[F, Any])(fk: F ~> G) = af.mapK(fk)
  }

  private val tuple2KFunctorK: FunctorK[Tuple2K[Any, *[_], Any]] = new FunctorK[Tuple2K[Any, *[_], Any]] {
    def mapK[F[_], G[_]](af: Tuple2K[Any, F, Any])(fk: F ~> G) = af.mapK(fk)
  }
}

private trait ContraUnorderedFoldable[F[_], G[_]] extends UnorderedFoldable[G] {
  def F: UnorderedFoldable[F]
  def from: G ~> F
  final override def unorderedFoldMap[A, B](fa: G[A])(f: A => B)(implicit M: CommutativeMonoid[B]): B =
    F.unorderedFoldMap(from(fa))(f)
}

private trait ContraFoldable[F[_], G[_]] extends Foldable[G] with ContraUnorderedFoldable[F, G] {
  def F: Foldable[F]
  final override def foldLeft[A, B](fa: G[A], b: B)(f: (B, A) => B): B =
    F.foldLeft(from(fa), b)(f)
  final override def foldRight[A, B](fa: G[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] =
    F.foldRight(from(fa), lb)(f)
}
