package mainecoon
package laws

import cats.arrow.FunctionK
import cats.laws._
import syntax.all._
import cats.~>

trait FunctorKLaws[F[_[_]]] extends InvariantKLaws[F]{
  implicit def F: FunctorK[F]

  def covariantIdentity[A[_]](fg: F[A]): IsEq[F[A]] =
    fg.mapK(FunctionK.id[A]) <-> fg

  def covariantComposition[A[_], B[_], C[_]](fa: F[A], f: A ~> B, g: B ~> C): IsEq[F[C]] =
    fa.mapK(f).mapK(g) <-> fa.mapK(f andThen g)

}

object FunctorKLaws {
  def apply[F[_[_]]](implicit ev: FunctorK[F]): FunctorKLaws[F] =
    new FunctorKLaws[F] { def F: FunctorK[F] = ev }
}
