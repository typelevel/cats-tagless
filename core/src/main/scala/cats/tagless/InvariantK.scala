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

import cats.*
import cats.data.*
import cats.kernel.CommutativeMonoid
import cats.tagless.derived.DerivedInvariantK

import scala.annotation.implicitNotFound

@implicitNotFound("Could not find an instance of InvariantK for ${Alg}")
trait InvariantK[Alg[_[_]]] extends Serializable {
  def imapK[F[_], G[_]](af: Alg[F])(fk: F ~> G)(gk: G ~> F): Alg[G]
}

object InvariantK extends InvariantKInstances01 with DerivedInvariantK {
  implicit def catsTaglessApplyKForEitherK[F[_], A]: ApplyK[Curried.EitherKRight[F, A]#λ] =
    eitherKInstance.asInstanceOf[ApplyK[Curried.EitherKRight[F, A]#λ]]

  implicit def catsTaglessApplyKForEitherT[A, B]: ApplyK[Curried.EitherT[A, B]#λ] =
    eitherTInstance.asInstanceOf[ApplyK[Curried.EitherT[A, B]#λ]]

  implicit def catsTaglessApplyKForFunc[A, B]: ApplyK[Curried.Func[A, B]#λ] =
    funcInstance.asInstanceOf[ApplyK[Curried.Func[A, B]#λ]]

  implicit def catsTaglessApplyKForIdT[A]: ApplyK[Curried.IdT[A]#λ] =
    idTInstance.asInstanceOf[ApplyK[Curried.IdT[A]#λ]]

  implicit def catsTaglessApplyKForIorT[A, B]: ApplyK[Curried.IorT[A, B]#λ] =
    iOrTInstance.asInstanceOf[ApplyK[Curried.IorT[A, B]#λ]]

  implicit def catsTaglessApplyKForKleisli[A, B]: ApplyK[Curried.Kleisli[A, B]#λ] =
    kleisliInstance.asInstanceOf[ApplyK[Curried.Kleisli[A, B]#λ]]

  implicit def catsTaglessApplyKForOptionT[A]: ApplyK[Curried.OptionT[A]#λ] =
    optionTInstance.asInstanceOf[ApplyK[Curried.OptionT[A]#λ]]

  implicit def catsTaglessApplyKForWriterT[A, B]: ApplyK[Curried.WriterT[A, B]#λ] =
    writerTInstance.asInstanceOf[ApplyK[Curried.WriterT[A, B]#λ]]

  implicit def catsTaglessContravariantKForCokleisli[A, B]: ContravariantK[Curried.Cokleisli[A, B]#λ] =
    cokleisliInstance.asInstanceOf[ContravariantK[Curried.Cokleisli[A, B]#λ]]

  implicit def catsTaglessFunctorKForNested[G[_], A]: FunctorK[Curried.NestedOuter[G, A]#λ] =
    nestedInstance.asInstanceOf[FunctorK[Curried.NestedOuter[G, A]#λ]]

  implicit def catsTaglessApplyKForOneAnd[A](implicit A: Semigroup[A]): ApplyK[Curried.OneAnd[A]#λ] =
    new ApplyK[Curried.OneAnd[A]#λ] {
      def mapK[F[_], G[_]](af: OneAnd[F, A])(fk: F ~> G) = af.mapK(fk)
      def productK[F[_], G[_]](af: OneAnd[F, A], ag: OneAnd[G, A]) =
        OneAnd(A.combine(af.head, ag.head), Tuple2K(af.tail, ag.tail))
    }

  implicit def catsTaglessApplyKForTuple2K1[G[_], A](implicit G: SemigroupK[G]): ApplyK[Curried.Tuple2KFirst[G, A]#λ] =
    new ApplyK[Curried.Tuple2KFirst[G, A]#λ] {
      def mapK[F[_], H[_]](af: Tuple2K[F, G, A])(fk: F ~> H) = Tuple2K(fk(af.first), af.second)
      def productK[F[_], H[_]](af: Tuple2K[F, G, A], ah: Tuple2K[H, G, A]): Tuple2K[Tuple2K[F, H, *], G, A] =
        Tuple2K(Tuple2K(af.first, ah.first), G.combineK(af.second, ah.second))
    }

  implicit def catsTaglessApplyKForTuple2K2[F[_], A](implicit F: SemigroupK[F]): ApplyK[Curried.Tuple2KSecond[F, A]#λ] =
    new ApplyK[Curried.Tuple2KSecond[F, A]#λ] {
      def mapK[G[_], H[_]](ag: Tuple2K[F, G, A])(fk: G ~> H) = ag.mapK(fk)
      def productK[G[_], H[_]](ag: Tuple2K[F, G, A], ah: Tuple2K[F, H, A]) =
        Tuple2K(F.combineK(ag.first, ah.first), Tuple2K(ag.second, ah.second))
    }

  implicit def catsTaglessContravariantForKFunctionK[G[_]]: ContravariantK[Curried.FunctionKLeft[G]#λ] =
    functionKContravariantK.asInstanceOf[ContravariantK[Curried.FunctionKLeft[G]#λ]]

  implicit def catsTaglessFunctorKForFunctionK[F[_]]: FunctorK[Curried.FunctionKRight[F]#λ] =
    functionKFunctorK.asInstanceOf[FunctorK[Curried.FunctionKRight[F]#λ]]

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

  private val eitherKInstance: ApplyK[Curried.EitherKRight[AnyK, Any]#λ] =
    new ApplyK[Curried.EitherKRight[AnyK, Any]#λ] {
      def mapK[F[_], G[_]](af: EitherK[AnyK, F, Any])(fk: F ~> G) = af.mapK(fk)
      def productK[F[_], G[_]](af: EitherK[AnyK, F, Any], ag: EitherK[AnyK, G, Any]) =
        (af.run, ag.run) match {
          case (Left(x), _) => EitherK.leftc[AnyK, Tuple2K[F, G, *], Any](x)
          case (_, Left(y)) => EitherK.leftc[AnyK, Tuple2K[F, G, *], Any](y)
          case (Right(fa), Right(ga)) => EitherK.rightc[AnyK, Tuple2K[F, G, *], Any](Tuple2K(fa, ga))
        }
    }

  private val eitherTInstance: ApplyK[Curried.EitherT[Any, Any]#λ] =
    new ApplyK[Curried.EitherT[Any, Any]#λ] {
      def mapK[F[_], G[_]](af: EitherT[F, Any, Any])(fk: F ~> G) = af.mapK(fk)
      def productK[F[_], G[_]](af: EitherT[F, Any, Any], ag: EitherT[G, Any, Any]) =
        EitherT(Tuple2K(af.value, ag.value))
    }

  private val funcInstance: ApplyK[Curried.Func[Any, Any]#λ] =
    new ApplyK[Curried.Func[Any, Any]#λ] {
      def mapK[F[_], G[_]](af: Func[F, Any, Any])(fk: F ~> G) = af.mapK(fk)
      def productK[F[_], G[_]](af: Func[F, Any, Any], ag: Func[G, Any, Any]) =
        Func.func(x => Tuple2K(af.run(x), ag.run(x)))
    }

  private val idTInstance: ApplyK[Curried.IdT[Any]#λ] = new ApplyK[Curried.IdT[Any]#λ] {
    def mapK[F[_], G[_]](af: IdT[F, Any])(fk: F ~> G) = af.mapK(fk)
    def productK[F[_], G[_]](af: IdT[F, Any], ag: IdT[G, Any]) =
      IdT(Tuple2K(af.value, ag.value))
  }

  private val iOrTInstance: ApplyK[Curried.IorT[Any, Any]#λ] =
    new ApplyK[Curried.IorT[Any, Any]#λ] {
      def mapK[F[_], G[_]](af: IorT[F, Any, Any])(fk: F ~> G) = af.mapK(fk)
      def productK[F[_], G[_]](af: IorT[F, Any, Any], ag: IorT[G, Any, Any]) =
        IorT(Tuple2K(af.value, ag.value))
    }

  private val kleisliInstance: ApplyK[Curried.Kleisli[Any, Any]#λ] =
    new ApplyK[Curried.Kleisli[Any, Any]#λ] {
      def mapK[F[_], G[_]](af: Kleisli[F, Any, Any])(fk: F ~> G) = af.mapK(fk)
      def productK[F[_], G[_]](af: Kleisli[F, Any, Any], ag: Kleisli[G, Any, Any]) =
        Kleisli(x => Tuple2K(af.run(x), ag.run(x)))
    }

  private val optionTInstance: ApplyK[Curried.OptionT[Any]#λ] =
    new ApplyK[Curried.OptionT[Any]#λ] {
      def mapK[F[_], G[_]](af: OptionT[F, Any])(fk: F ~> G) = af.mapK(fk)
      def productK[F[_], G[_]](af: OptionT[F, Any], ag: OptionT[G, Any]) =
        OptionT(Tuple2K(af.value, ag.value))
    }

  private val writerTInstance: ApplyK[Curried.WriterT[Any, Any]#λ] =
    new ApplyK[Curried.WriterT[Any, Any]#λ] {
      def mapK[F[_], G[_]](af: WriterT[F, Any, Any])(fk: F ~> G) = af.mapK(fk)
      def productK[F[_], G[_]](af: WriterT[F, Any, Any], ag: WriterT[G, Any, Any]) =
        WriterT(Tuple2K(af.run, ag.run))
    }

  private val cokleisliInstance: ContravariantK[Curried.Cokleisli[Any, Any]#λ] =
    new ContravariantK[Curried.Cokleisli[Any, Any]#λ] {
      def contramapK[F[_], G[_]](af: Cokleisli[F, Any, Any])(fk: G ~> F) = Cokleisli(ga => af.run(fk(ga)))
    }

  private val nestedInstance: FunctorK[Curried.NestedOuter[AnyK, Any]#λ] =
    new FunctorK[Curried.NestedOuter[AnyK, Any]#λ] {
      def mapK[F[_], G[_]](af: Nested[F, AnyK, Any])(fk: F ~> G) = af.mapK(fk)
    }

  private val functionKContravariantK: ContravariantK[Curried.FunctionKLeft[AnyK]#λ] =
    new ContravariantK[Curried.FunctionKLeft[AnyK]#λ] {
      def contramapK[F[_], G[_]](af: F ~> AnyK)(fk: G ~> F) = af.compose(fk)
    }

  private val functionKFunctorK: FunctorK[Curried.FunctionKRight[AnyK]#λ] =
    new FunctorK[Curried.FunctionKRight[AnyK]#λ] {
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
  implicit def catsTaglessFunctorKForOneAnd[A]: FunctorK[Curried.OneAnd[A]#λ] =
    oneAndFunctorK.asInstanceOf[FunctorK[Curried.OneAnd[A]#λ]]

  implicit def catsTaglessFunctorKForTuple2K[F[_], A]: FunctorK[Curried.Tuple2KSecond[F, A]#λ] =
    tuple2KFunctorK.asInstanceOf[FunctorK[Curried.Tuple2KSecond[F, A]#λ]]

  private val oneAndFunctorK: FunctorK[Curried.OneAnd[Any]#λ] =
    new FunctorK[Curried.OneAnd[Any]#λ] {
      def mapK[F[_], G[_]](af: OneAnd[F, Any])(fk: F ~> G) = af.mapK(fk)
    }

  private val tuple2KFunctorK: FunctorK[Curried.Tuple2KSecond[AnyK, Any]#λ] =
    new FunctorK[Curried.Tuple2KSecond[AnyK, Any]#λ] {
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
