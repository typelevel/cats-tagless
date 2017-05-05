package mainecoon

import cats.data.Prod
import simulacrum.typeclass

@typeclass trait CartesianK[A[_[_]]] {
  def product[F[_], G[_]](af: A[F], ag: A[G]): A[Prod[F, G, ?]]
}
