package cats.tagless
package laws

import cats.laws._
import cats.~>

trait TraverseKLaws[F[_[_]]] extends FunctorKLaws[F] {
  implicit def F: TraverseK[F]

  def traverseKIdentity[A[_], B[_]](fa: F[A])(f: A ~> B) =
    F.traverseK[A, cats.Id, B](fa)(f) <-> F.mapK(fa)(f)
}

object TraverseKLaws {
  def apply[F[_[_]]](implicit ev: TraverseK[F]): TraverseKLaws[F] =
    new TraverseKLaws[F] { val F = ev }
}
