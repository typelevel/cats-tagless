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

import cats.tagless.derived.DerivedInvariantK
import cats.*
import cats.arrow.FunctionK
import cats.data.*
import cats.kernel.CommutativeMonoid

import scala.annotation.implicitNotFound

@implicitNotFound("Could not find an instance of InvariantK for ${Alg}")
trait InvariantK[Alg[_[_]]] extends Serializable {
  def imapK[F[_], G[_]](af: Alg[F])(fk: F ~> G)(gk: G ~> F): Alg[G]
}

object InvariantK extends InvariantKInstances01 with DerivedInvariantK {
  implicit def catsTaglessApplyKForEitherK[F[_], A]: ApplyK[({ type W[g[_]] = EitherK[F, g, A] })#W] =
    eitherKInstance.asInstanceOf[ApplyK[({ type W[g[_]] = EitherK[F, g, A] })#W]]

  implicit def catsTaglessApplyKForEitherT[A, B]: ApplyK[({ type W[f[_]] = EitherT[f, A, B] })#W] =
    eitherTInstance.asInstanceOf[ApplyK[({ type W[f[_]] = EitherT[f, A, B] })#W]]

  implicit def catsTaglessApplyKForFunc[A, B]: ApplyK[({ type W[f[_]] = Func[f, A, B] })#W] =
    funcInstance.asInstanceOf[ApplyK[({ type W[f[_]] = Func[f, A, B] })#W]]

  implicit def catsTaglessApplyKForIdT[A]: ApplyK[({ type W[f[_]] = IdT[f, A] })#W] =
    idTInstance.asInstanceOf[ApplyK[({ type W[f[_]] = IdT[f, A] })#W]]

  implicit def catsTaglessApplyKForIorT[A, B]: ApplyK[({ type W[f[_]] = IorT[f, A, B] })#W] =
    iOrTInstance.asInstanceOf[ApplyK[({ type W[f[_]] = IorT[f, A, B] })#W]]

  implicit def catsTaglessApplyKForKleisli[A, B]: ApplyK[({ type W[f[_]] = Kleisli[f, A, B] })#W] =
    kleisliInstance.asInstanceOf[ApplyK[({ type W[f[_]] = Kleisli[f, A, B] })#W]]

  implicit def catsTaglessApplyKForOptionT[A]: ApplyK[({ type W[f[_]] = OptionT[f, A] })#W] =
    optionTInstance.asInstanceOf[ApplyK[({ type W[f[_]] = OptionT[f, A] })#W]]

  implicit def catsTaglessApplyKForWriterT[A, B]: ApplyK[({ type W[f[_]] = WriterT[f, A, B] })#W] =
    writerTInstance.asInstanceOf[ApplyK[({ type W[f[_]] = WriterT[f, A, B] })#W]]

  implicit def catsTaglessContravariantKForCokleisli[A, B]: ContravariantK[({ type W[f[_]] = Cokleisli[f, A, B] })#W] =
    cokleisliInstance.asInstanceOf[ContravariantK[({ type W[f[_]] = Cokleisli[f, A, B] })#W]]

  implicit def catsTaglessFunctorKForNested[G[_], A]: FunctorK[({ type W[f[_]] = Nested[f, G, A] })#W] =
    nestedInstance.asInstanceOf[FunctorK[({ type W[f[_]] = Nested[f, G, A] })#W]]

  implicit def catsTaglessApplyKForOneAnd[A](implicit A: Semigroup[A]): ApplyK[({ type W[s[_]] = OneAnd[s, A] })#W] =
    new ApplyK[({ type W[s[_]] = OneAnd[s, A] })#W] {
      def mapK[F[_], G[_]](af: OneAnd[F, A])(fk: F ~> G) = af.mapK(fk)
      def productK[F[_], G[_]](af: OneAnd[F, A], ag: OneAnd[G, A]) =
        OneAnd(A.combine(af.head, ag.head), Tuple2K(af.tail, ag.tail))
    }

  implicit def catsTaglessApplyKForTuple2K1[H[_], A](implicit
      H: SemigroupK[H]
  ): ApplyK[({ type W[f[_]] = Tuple2K[f, H, A] })#W] =
    new ApplyK[({ type W[f[_]] = Tuple2K[f, H, A] })#W] {
      def mapK[F[_], G[_]](af: Tuple2K[F, H, A])(fk: F ~> G) = Tuple2K(fk(af.first), af.second)
      def productK[F[_], G[_]](af: Tuple2K[F, H, A], ag: Tuple2K[G, H, A]): Tuple2K[Tuple2K[F, G, *], H, A] =
        Tuple2K(Tuple2K(af.first, ag.first), H.combineK(af.second, ag.second))
    }

  implicit def catsTaglessApplyKForTuple2K2[H[_], A](implicit
      H: SemigroupK[H]
  ): ApplyK[({ type W[g[_]] = Tuple2K[H, g, A] })#W] =
    new ApplyK[({ type W[g[_]] = Tuple2K[H, g, A] })#W] {
      def mapK[F[_], G[_]](af: Tuple2K[H, F, A])(fk: F ~> G) = af.mapK(fk)
      def productK[F[_], G[_]](af: Tuple2K[H, F, A], ag: Tuple2K[H, G, A]) =
        Tuple2K(H.combineK(af.first, ag.first), Tuple2K(af.second, ag.second))
    }

  implicit def catsTaglessContravariantForKFunctionK[G[_]]: ContravariantK[({ type W[f[_]] = FunctionK[f, G] })#W] =
    functionKContravariantK.asInstanceOf[ContravariantK[({ type W[f[_]] = FunctionK[f, G] })#W]]

  implicit def catsTaglessFunctorKForFunctionK[F[_]]: FunctorK[({ type W[g[_]] = FunctionK[F, g] })#W] =
    functionKFunctorK.asInstanceOf[FunctorK[({ type W[g[_]] = FunctionK[F, g] })#W]]

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

  private val eitherKInstance: ApplyK[({ type W[g[_]] = EitherK[AnyK, g, Any] })#W] =
    new ApplyK[({ type W[g[_]] = EitherK[AnyK, g, Any] })#W] {
      def mapK[F[_], G[_]](af: EitherK[AnyK, F, Any])(fk: F ~> G) = af.mapK(fk)
      def productK[F[_], G[_]](af: EitherK[AnyK, F, Any], ag: EitherK[AnyK, G, Any]) =
        (af.run, ag.run) match {
          case (Left(x), _) => EitherK.leftc[AnyK, Tuple2K[F, G, *], Any](x)
          case (_, Left(y)) => EitherK.leftc[AnyK, Tuple2K[F, G, *], Any](y)
          case (Right(fa), Right(ga)) => EitherK.rightc[AnyK, Tuple2K[F, G, *], Any](Tuple2K(fa, ga))
        }
    }

  private val eitherTInstance: ApplyK[({ type W[f[_]] = EitherT[f, Any, Any] })#W] =
    new ApplyK[({ type W[f[_]] = EitherT[f, Any, Any] })#W] {
      def mapK[F[_], G[_]](af: EitherT[F, Any, Any])(fk: F ~> G) = af.mapK(fk)
      def productK[F[_], G[_]](af: EitherT[F, Any, Any], ag: EitherT[G, Any, Any]) =
        EitherT(Tuple2K(af.value, ag.value))
    }

  private val funcInstance: ApplyK[({ type W[f[_]] = Func[f, Any, Any] })#W] =
    new ApplyK[({ type W[f[_]] = Func[f, Any, Any] })#W] {
      def mapK[F[_], G[_]](af: Func[F, Any, Any])(fk: F ~> G) = af.mapK(fk)
      def productK[F[_], G[_]](af: Func[F, Any, Any], ag: Func[G, Any, Any]) =
        Func.func(x => Tuple2K(af.run(x), ag.run(x)))
    }

  private val idTInstance: ApplyK[({ type W[f[_]] = IdT[f, Any] })#W] = new ApplyK[({ type W[f[_]] = IdT[f, Any] })#W] {
    def mapK[F[_], G[_]](af: IdT[F, Any])(fk: F ~> G) = af.mapK(fk)
    def productK[F[_], G[_]](af: IdT[F, Any], ag: IdT[G, Any]) =
      IdT(Tuple2K(af.value, ag.value))
  }

  private val iOrTInstance: ApplyK[({ type W[f[_]] = IorT[f, Any, Any] })#W] =
    new ApplyK[({ type W[f[_]] = IorT[f, Any, Any] })#W] {
      def mapK[F[_], G[_]](af: IorT[F, Any, Any])(fk: F ~> G) = af.mapK(fk)
      def productK[F[_], G[_]](af: IorT[F, Any, Any], ag: IorT[G, Any, Any]) =
        IorT(Tuple2K(af.value, ag.value))
    }

  private val kleisliInstance: ApplyK[({ type W[f[_]] = Kleisli[f, Any, Any] })#W] =
    new ApplyK[({ type W[f[_]] = Kleisli[f, Any, Any] })#W] {
      def mapK[F[_], G[_]](af: Kleisli[F, Any, Any])(fk: F ~> G) = af.mapK(fk)
      def productK[F[_], G[_]](af: Kleisli[F, Any, Any], ag: Kleisli[G, Any, Any]) =
        Kleisli(x => Tuple2K(af.run(x), ag.run(x)))
    }

  private val optionTInstance: ApplyK[({ type W[f[_]] = OptionT[f, Any] })#W] =
    new ApplyK[({ type W[f[_]] = OptionT[f, Any] })#W] {
      def mapK[F[_], G[_]](af: OptionT[F, Any])(fk: F ~> G) = af.mapK(fk)
      def productK[F[_], G[_]](af: OptionT[F, Any], ag: OptionT[G, Any]) =
        OptionT(Tuple2K(af.value, ag.value))
    }

  private val writerTInstance: ApplyK[({ type W[f[_]] = WriterT[f, Any, Any] })#W] =
    new ApplyK[({ type W[f[_]] = WriterT[f, Any, Any] })#W] {
      def mapK[F[_], G[_]](af: WriterT[F, Any, Any])(fk: F ~> G) = af.mapK(fk)
      def productK[F[_], G[_]](af: WriterT[F, Any, Any], ag: WriterT[G, Any, Any]) =
        WriterT(Tuple2K(af.run, ag.run))
    }

  private val cokleisliInstance: ContravariantK[({ type W[f[_]] = Cokleisli[f, Any, Any] })#W] =
    new ContravariantK[({ type W[f[_]] = Cokleisli[f, Any, Any] })#W] {
      def contramapK[F[_], G[_]](af: Cokleisli[F, Any, Any])(fk: G ~> F) = Cokleisli(ga => af.run(fk(ga)))
    }

  private val nestedInstance: FunctorK[({ type W[f[_]] = Nested[f, AnyK, Any] })#W] =
    new FunctorK[({ type W[f[_]] = Nested[f, AnyK, Any] })#W] {
      def mapK[F[_], G[_]](af: Nested[F, AnyK, Any])(fk: F ~> G) = af.mapK(fk)
    }

  private val functionKContravariantK: ContravariantK[({ type W[f[_]] = FunctionK[f, AnyK] })#W] =
    new ContravariantK[({ type W[f[_]] = FunctionK[f, AnyK] })#W] {
      def contramapK[F[_], G[_]](af: F ~> AnyK)(fk: G ~> F) = af.compose(fk)
    }

  private val functionKFunctorK: FunctorK[({ type W[g[_]] = FunctionK[AnyK, g] })#W] =
    new FunctorK[({ type W[g[_]] = FunctionK[AnyK, g] })#W] {
      def mapK[F[_], G[_]](af: AnyK ~> F)(fn: F ~> G) = af.andThen(fn)
    }

  // =======================
  // Generated by simulacrum
  // =======================

  @inline def apply[Alg[_[_]]](implicit instance: InvariantK[Alg]): InvariantK[Alg] = instance

  trait AllOps[Alg[_[_]], F[_]] extends Ops[Alg, F] {
    type TypeClassType <: InvariantK[Alg]
    val typeClassInstance: TypeClassType
  }

  object ops {
    implicit def toAllInvariantKOps[Alg[_[_]], F[_]](target: Alg[F])(implicit tc: InvariantK[Alg]): AllOps[Alg, F] {
      type TypeClassType = InvariantK[Alg]
    } = new AllOps[Alg, F] {
      type TypeClassType = InvariantK[Alg]
      val self = target
      val typeClassInstance: TypeClassType = tc
    }
  }

  trait Ops[Alg[_[_]], F[_]] {
    type TypeClassType <: InvariantK[Alg]
    val typeClassInstance: TypeClassType
    def self: Alg[F]
    def imapK[G[_]](fk: F ~> G)(gk: G ~> F): Alg[G] =
      typeClassInstance.imapK[F, G](self)(fk)(gk)
  }

  trait ToInvariantKOps {
    implicit def toInvariantKOps[Alg[_[_]], F[_]](target: Alg[F])(implicit tc: InvariantK[Alg]): Ops[Alg, F] {
      type TypeClassType = InvariantK[Alg]
    } = new Ops[Alg, F] {
      type TypeClassType = InvariantK[Alg]
      val self = target
      val typeClassInstance: TypeClassType = tc
    }
  }

  object nonInheritedOps extends ToInvariantKOps
}

sealed private[tagless] trait InvariantKInstances01 {
  implicit def catsTaglessFunctorKForOneAnd[A]: FunctorK[({ type W[s[_]] = OneAnd[s, A] })#W] =
    oneAndFunctorK.asInstanceOf[FunctorK[({ type W[s[_]] = OneAnd[s, A] })#W]]

  implicit def catsTaglessFunctorKForTuple2K[F[_], A]: FunctorK[({ type W[g[_]] = Tuple2K[F, g, A] })#W] =
    tuple2KFunctorK.asInstanceOf[FunctorK[({ type W[g[_]] = Tuple2K[F, g, A] })#W]]

  private val oneAndFunctorK: FunctorK[({ type W[s[_]] = OneAnd[s, Any] })#W] =
    new FunctorK[({ type W[s[_]] = OneAnd[s, Any] })#W] {
      def mapK[F[_], G[_]](af: OneAnd[F, Any])(fk: F ~> G) = af.mapK(fk)
    }

  private val tuple2KFunctorK: FunctorK[({ type W[g[_]] = Tuple2K[AnyK, g, Any] })#W] =
    new FunctorK[({ type W[g[_]] = Tuple2K[AnyK, g, Any] })#W] {
      def mapK[F[_], G[_]](af: Tuple2K[AnyK, F, Any])(fk: F ~> G) = af.mapK(fk)
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
