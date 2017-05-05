package mainecoon
package laws

import cats.data.Prod

trait CartesianKLaws[A[_[_]]] {
  implicit def A: CartesianK[A]

  def cartesianAssociativity[F[_], G[_], H[_]](af: A[F], ag: A[G], ah: A[H]):
  (A[Prod[F, Prod[G, H, ?], ?]], A[Prod[Prod[F, G, ?], H, ?]]) =
    (A.product(af, A.product(ag, ah)), A.product(A.product(af, ag), ah))

}

object CartesianKLaws {
  def apply[A[_[_]]](implicit ev: CartesianK[A]): CartesianKLaws[A] =
    new CartesianKLaws[A] { val A = ev }
}
