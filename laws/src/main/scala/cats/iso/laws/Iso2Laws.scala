package cats.iso.laws

import cats.iso.<~>
import cats.laws._
import cats.kernel.laws.IsEq

trait Iso2Laws[F[_], G[_]] {
  def iso2: F <~> G

  def iso2identityLeft[A](f: F[A]): IsEq[F[A]] =
    iso2.from(iso2.to(f)) <-> f

  def iso2identityRight[A](g: G[A]): IsEq[G[A]] =
    iso2.to(iso2.from(g)) <-> g
}

object Iso2Laws {
  def apply[F[_], G[_], A](instance: F <~> G): Iso2Laws[F, G] = new Iso2Laws[F, G] {
    override def iso2: F <~> G = instance
  }
}
