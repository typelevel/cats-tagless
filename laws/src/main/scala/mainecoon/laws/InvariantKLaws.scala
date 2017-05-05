package mainecoon
package laws

import cats.arrow.FunctionK
import cats.laws._
import cats.~>
import syntax.all._

trait InvariantKLaws[F[_[_]]] {
  implicit def F: InvariantK[F]

  def invariantIdentity[A[_]](fa: F[A]): IsEq[F[A]] =
    fa.imapK(FunctionK.id[A])(FunctionK.id[A]) <-> fa

  def invariantComposition[A[_], B[_], C[_]](fa: F[A], f1: A ~> B, f2: B ~> A, g1: B ~> C, g2: C ~> B): IsEq[F[C]] =
    fa.imapK(f1)(f2).imapK(g1)(g2) <-> fa.imapK(g1 compose f1)(f2 compose g2)

}



object InvariantKLaws {
  def apply[F[_[_]]](implicit ev: InvariantK[F]): InvariantKLaws[F] =
    new InvariantKLaws[F] { def F: InvariantK[F] = ev }
}
