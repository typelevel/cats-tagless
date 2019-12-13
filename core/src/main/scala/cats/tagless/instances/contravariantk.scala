package cats.tagless.instances

import cats._
import cats.kernel.CommutativeMonoid
import cats.tagless._

trait ContravariantKInstances {
  import ContravariantKTraits._
  implicit def conKUnorderedFoldable: ContravariantK[UnorderedFoldable] = new ConKUnorderedFoldable {}
  implicit def conKFoldable:          ContravariantK[Foldable]          = new ConKFoldable {}
}

object ContravariantKTraits extends ContravariantKTraits

trait ContravariantKTraits extends ContravariantKPrivate {
  trait ConKUnorderedFoldable extends ContravariantK[UnorderedFoldable] {
    override def contramapK[F[_], G[_]](af: UnorderedFoldable[F])(gf: ~>[G, F]): UnorderedFoldable[G] =
      new Nat2UnorderedFoldable[F, G] { val F = af; val from = gf }
  }

  trait ConKFoldable extends ContravariantK[Foldable] {
    override def contramapK[F[_], G[_]](af: Foldable[F])(gf: ~>[G, F]): Foldable[G] =
      new Nat2Foldable[F, G] { val F = af; val from = gf }
  }

}

trait ContravariantKPrivate {
  trait Nat2UnorderedFoldable[F[_], G[_]] extends UnorderedFoldable[G] {
    def F: UnorderedFoldable[F]
    def from : G ~> F
    override def unorderedFoldMap[A, B](fa: G[A])(f: A => B)(implicit M: CommutativeMonoid[B]): B =
      F.unorderedFoldMap(from(fa))(f)
  }

  trait Nat2Foldable[F[_], G[_]] extends Foldable[G] with Nat2UnorderedFoldable[F, G] {
    def F: Foldable[F]
    override def foldLeft[A, B](fa: G[A], b: B)(f: (B, A) => B): B =
      F.foldLeft(from(fa), b)(f)
    override def foldRight[A, B](fa: G[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] =
      F.foldRight(from(fa), lb)(f)
  }
}