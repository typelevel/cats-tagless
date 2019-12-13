package cats.tagless.instances

import cats._
import cats.implicits._
import cats.kernel.CommutativeMonoid
import cats.tagless._

trait InvariantKInstances extends InvariantKInstances01 {
  implicit def invKMonad:                InvariantK[Monad]                = new InvKMonad {}
  implicit def invKTraverse:             InvariantK[Traverse]             = new InvKTraverse {}
}
trait InvariantKInstances01 extends InvariantKInstances02 {
  implicit def invKFlatMap:              InvariantK[FlatMap]              = new InvKFlatMap {}
  implicit def invKApplicative:          InvariantK[Applicative]          = new InvKApplicative {}
  implicit def invKUnorderedTraverse:    InvariantK[UnorderedTraverse]    = new InvKUnorderedTraverse {}
}
trait InvariantKInstances02 extends InvariantKInstances03 {
  implicit def invKUnorderedFoldable:    InvariantK[UnorderedFoldable]    = new InvKUnorderedFoldable {}
  implicit def invKContravariantMonoidal: InvariantK[ContravariantMonoidal] = new InvKContravariantMonoidal {}
  implicit def invKApply:                InvariantK[Apply]                = new InvKApply {}
}
trait InvariantKInstances03 extends InvariantKInstances04 {
  implicit def invKAlternative:          InvariantK[Alternative]          = new InvKAlternative {}
  implicit def invKContravariantSemigroupal: InvariantK[ContravariantSemigroupal] = new InvKContravariantSemigroupal {}
}
trait InvariantKInstances04 extends InvariantKInstances05 {
  implicit def invKMonoidK:              InvariantK[MonoidK]              = new InvKMonoidK {}
  implicit def invKInvariantMonoidal:    InvariantK[InvariantMonoidal]    = new InvKInvariantMonoidal {}
  implicit def invKDistributive:         InvariantK[Distributive]         = new InvKDistributive {}
}
trait InvariantKInstances05 extends InvariantKInstances06 {
  implicit def invKInvariantSemigroupal: InvariantK[InvariantSemigroupal] = new InvKInvariantSemigroupal {}
  implicit def invKSemigroupK:           InvariantK[SemigroupK]           = new InvKSemigroupK {}
}
trait InvariantKInstances06 extends InvariantKInstances07 {
  implicit def invKFunctor:              InvariantK[Functor]              = new InvKFunctor {}
  implicit def invKContravariant:        InvariantK[Contravariant]        = new InvKContravariant {}
  implicit def invKSemigroupal:          InvariantK[Semigroupal]          = new InvKSemigroupal {}
}
trait InvariantKInstances07 extends InvariantKInstances08 {
  implicit def invKInvariant:            InvariantK[Invariant]            = new InvKInvariant {}
}
trait InvariantKInstances08 extends InvariantKTraits {
}

object InvariantKTraits extends InvariantKTraits
trait InvariantKTraits extends InvariantKTraitsPrivate {
  trait InvKInvariant extends InvariantK[Invariant] {
    override def imapK[F[_], G[_]](af: Invariant[F])(ftog: F ~> G)(gtof: G ~> F): Invariant[G] =
      new Iso2Invariant[F, G] { val F = af; val to = ftog; val from = gtof }
  }
  trait InvKContravariant extends InvariantK[Contravariant] {
    override def imapK[F[_], G[_]](af: Contravariant[F])(ftog: F ~> G)(gtof: G ~> F): Contravariant[G] =
      new Iso2Contravariant[F, G] { val F = af; val to = ftog; val from = gtof }
  }
  trait InvKFunctor extends InvariantK[Functor] {
    override def imapK[F[_], G[_]](af: Functor[F])(fg: F ~> G)(gf: G ~> F): Functor[G] =
      new Iso2Functor[F, G] { val F = af; val to = fg; val from = gf }
  }
  trait InvKSemigroupal extends InvariantK[Semigroupal] {
    override def imapK[F[_], G[_]](af: Semigroupal[F])(fg: F ~> G)(gf: G ~> F): Semigroupal[G] =
      new Iso2Semigroupal[F, G] {val F = af; val to = fg; val from = gf }
  }
  trait InvKInvariantSemigroupal extends InvariantK[InvariantSemigroupal] {
    override def imapK[F[_], G[_]](af: InvariantSemigroupal[F])(fg: F ~> G)(gf: G ~> F): InvariantSemigroupal[G] =
      new Iso2InvariantSemigroupal[F, G] { val F = af; val to = fg; val from = gf }
  }
  trait InvKApply extends InvariantK[Apply] {
    override def imapK[F[_], G[_]](af: Apply[F])(fg: F ~> G)(gf: G ~> F): Apply[G] =
      new Iso2Apply[F, G] { val F = af; val to = fg; val from = gf }
  }
  trait InvKMonoidK extends InvariantK[MonoidK] {
    override def imapK[F[_], G[_]](af: MonoidK[F])(fg: F ~> G)(gf: G ~> F): MonoidK[G] =
      new Iso2MonoidK[F, G] { val M = af; val to = fg; val from = gf }
  }
  trait InvKSemigroupK extends InvariantK[SemigroupK] {
    override def imapK[F[_], G[_]](af: SemigroupK[F])(fg: F ~> G)(gf: G ~> F): SemigroupK[G] =
      new Iso2SemigroupK[F, G] { val M = af; val to = fg; val from = gf }
  }
  trait InvKDistributive extends InvariantK[Distributive] {
    override def imapK[F[_], G[_]](af: Distributive[F])(fg: F ~> G)(gf: G ~> F): Distributive[G] =
      new Iso2Distributive[F, G] { val F = af; val to = fg; val from = gf }
  }
  trait InvKAlternative extends InvariantK[Alternative] {
    override def imapK[F[_], G[_]](af: Alternative[F])(fg: F ~> G)(gf: G ~> F): Alternative[G] =
      new Iso2Alternative[F, G] { val F = af; val to = fg; val from = gf }
  }
  trait InvKInvariantMonoidal extends InvariantK[InvariantMonoidal] {
    override def imapK[F[_], G[_]](af: InvariantMonoidal[F])(fg: F ~> G)(gf: G ~> F): InvariantMonoidal[G] =
      new Iso2InvariantMonoidal[F, G] { val F = af; val to = fg; val from = gf }
  }
  trait InvKContravariantSemigroupal extends InvariantK[ContravariantSemigroupal] {
    override def imapK[F[_], G[_]](af: ContravariantSemigroupal[F])(fg: F ~> G)(gf: G ~> F): ContravariantSemigroupal[G] =
      new Iso2ContravariantSemigroupal[F, G] { val F = af; val to = fg; val from = gf }
  }
  trait InvKContravariantMonoidal extends InvariantK[ContravariantMonoidal] {
    override def imapK[F[_], G[_]](af: ContravariantMonoidal[F])(fg: F ~> G)(gf: G ~> F): ContravariantMonoidal[G] =
      new Iso2ContravariantMonoidal[F, G] { val F = af; val to = fg; val from = gf }
  }
  trait InvKApplicative extends InvariantK[Applicative] {
    override def imapK[F[_], G[_]](af: Applicative[F])(fg: F ~> G)(gf: G ~> F): Applicative[G] =
      new Iso2Applicative[F, G] { val F = af; val to = fg; val from = gf }
  }
  trait InvKFlatMap extends InvariantK[FlatMap] {
    override def imapK[F[_], G[_]](af: FlatMap[F])(fg: F ~> G)(gf: G ~> F): FlatMap[G] =
      new Iso2FlatMap[F, G] { val F = af; val to = fg; val from = gf }
  }
  trait InvKMonad extends InvariantK[Monad] {
    override def imapK[F[_], G[_]](af: Monad[F])(fg: F ~> G)(gf: G ~> F): Monad[G] =
      new Iso2Monad[F, G] { val F = af; val to = fg; val from = gf }
  }
  trait InvKUnorderedFoldable extends InvariantK[UnorderedFoldable] {
    override def imapK[F[_], G[_]](af: UnorderedFoldable[F])(fg: F ~> G)(gf: G ~> F): UnorderedFoldable[G] =
      new Iso2UnorderedFoldable[F, G] { val F = af; val to = fg; val from = gf }
  }
  trait InvKUnorderedTraverse extends InvariantK[UnorderedTraverse] {
    override def imapK[F[_], G[_]](af: UnorderedTraverse[F])(fg: F ~> G)(gf: G ~> F): UnorderedTraverse[G] =
      new Iso2UnorderedTraverse[F, G] { val F = af; val to = fg; val from = gf }
  }
  trait InvKTraverse extends InvariantK[Traverse] {
    override def imapK[F[_], G[_]](af: Traverse[F])(fg: F ~> G)(gf: G ~> F): Traverse[G] =
      new Iso2Traverse[F, G] {val F = af; val to = fg; val from = gf }
  }
}

trait InvariantKTraitsPrivate {

  trait Iso2Invariant[F[_], G[_]] extends Invariant[G] {
    def F: Invariant[F]
    def to: F ~> G
    def from: G ~> F
    override def imap[A, B](fa: G[A])(f: A => B)(g: B => A): G[B] =
      to(F.imap(from(fa))(f)(g))
  }

  trait Iso2Semigroupal[F[_], G[_]] extends Semigroupal[G] {
    def F: Semigroupal[F]
    def to: F ~> G
    def from: G ~> F
    override def product[A, B](fa: G[A], fb: G[B]): G[(A, B)] = to(F.product(from(fa), from(fb)))
  }

  trait Iso2Contravariant[F[_], G[_]] extends Contravariant[G] with Iso2Invariant[F, G] {
    def F: Contravariant[F]
    override def contramap[A, B](fa: G[A])(f: B => A): G[B] = to(F.contramap(from(fa))(f))
  }

  trait Iso2Functor[F[_], G[_]] extends Functor[G] with Iso2Invariant[F, G] {
    def F: Functor[F]
    override def map[A, B](fa: G[A])(f: A => B): G[B] = to(F.map(from(fa))(f))
  }

  trait Iso2InvariantSemigroupal[F[_], G[_]] extends InvariantSemigroupal[G]
    with Iso2Invariant[F, G] with Iso2Semigroupal[F, G]
  { def F: InvariantSemigroupal[F] }

  trait Iso2ContravariantSemigroupal[F[_], G[_]] extends ContravariantSemigroupal[G]
    with Iso2Contravariant[F, G] with Iso2InvariantSemigroupal[F, G]
  { def F: ContravariantSemigroupal[F] }

  trait Iso2Apply[F[_], G[_]] extends Apply[G]
    with Iso2Functor[F, G] with Iso2InvariantSemigroupal[F, G] {
    def F: Apply[F]
    override def ap[A, B](ff: G[A => B])(fa: G[A]): G[B] = to(F.ap(from(ff))(from(fa)))
  }

  trait Iso2SemigroupK[F[_], G[_]] extends SemigroupK[G] {
    def M: SemigroupK[F]
    def to: F ~> G
    def from: G ~> F
    def combineK[A](x: G[A], y: G[A]): G[A] = to(M.combineK(from(x), from(y)))
  }

  trait Iso2MonoidK[F[_], G[_]] extends MonoidK[G] with Iso2SemigroupK[F, G] {
    def M: MonoidK[F]
    override def empty[A]: G[A] = to(M.empty[A])
  }

  trait Iso2Distributive[F[_], G[_]] extends Distributive[G] with Iso2Functor[F, G] {
    def F: Distributive[F]
    def distribute[H[_], A, B](ha: H[A])(f: A => G[B])(implicit H: Functor[H]): G[H[B]] =
      to(F.distribute[H, A, B](ha)(a => from(f(a))))
  }

  trait Iso2Alternative[F[_], G[_]] extends Alternative[G] with Iso2Applicative[F, G] with Iso2MonoidK[F, G] {
    final val M: Alternative[F] = F
    def F: Alternative[F]
  }

  trait Iso2InvariantMonoidal[F[_], G[_]] extends InvariantMonoidal[G] with Iso2InvariantSemigroupal[F, G] {
    def F: InvariantMonoidal[F]
    override def unit: G[Unit] = to(F.unit)
  }

  trait Iso2ContravariantMonoidal[F[_], G[_]] extends ContravariantMonoidal[G]
    with Iso2ContravariantSemigroupal[F, G] with Iso2InvariantMonoidal[F, G]
  { def F: ContravariantMonoidal[F] }

  trait Iso2Applicative[F[_], G[_]] extends Applicative[G]
    with Iso2Apply[F, G] with Iso2InvariantMonoidal[F, G] {
    def F: Applicative[F]
    override def pure[A](x: A): G[A] = to(F.pure(x))
  }

  trait Iso2FlatMap[F[_], G[_]] extends FlatMap[G] with Iso2Apply[F, G] {
    def F: FlatMap[F]
    override def flatMap[A, B](fa: G[A])(f: A => G[B]): G[B] =
      to(F.flatMap(from(fa))(f >>> from.apply))
    override def tailRecM[A, B](a: A)(f: A => G[Either[A, B]]): G[B] =
      to(F.tailRecM(a)(f >>> from.apply))
  }

  trait Iso2Monad[F[_], G[_]] extends Monad[G] with Iso2Applicative[F, G] with Iso2FlatMap[F, G]
  { def F: Monad[F] }

  trait Iso2UnorderedTraverse[F[_], G[_]] extends UnorderedTraverse[G] with Iso2UnorderedFoldable[F, G] {
    def F: UnorderedTraverse[F]
    def to: F ~> G
    def from: G ~> F
    override def unorderedTraverse[H[_], A, B](sa: G[A])(f: A => H[B])(implicit ev: CommutativeApplicative[H]): H[G[B]] =
      Applicative[H].map(F.unorderedTraverse(from(sa))(f))(to.apply)
  }

  trait Iso2UnorderedFoldable[F[_], G[_]] extends UnorderedFoldable[G] {
    def F: UnorderedFoldable[F]
    def to: F ~> G
    def from: G ~> F
    override def unorderedFoldMap[A, B](fa: G[A])(f: A => B)(implicit M: CommutativeMonoid[B]): B =
      F.unorderedFoldMap(from(fa))(f)
  }

  trait Iso2Traverse[F[_], G[_]] extends Traverse[G] with Iso2Functor[F, G] {
    def F: Traverse[F]
    override def foldLeft[A, B](fa: G[A], b: B)(f: (B, A) => B): B =
      F.foldLeft(from(fa), b)(f)
    override def foldRight[A, B](fa: G[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] =
      F.foldRight(from(fa), lb)(f)

    override def traverse[H[_], A, B](fa: G[A])(f: A => H[B])(implicit evidence$1: Applicative[H]): H[G[B]] =
      Applicative[H].map(F.traverse(from(fa))(f))(to.apply)
  }

}