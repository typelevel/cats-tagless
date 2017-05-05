package mainecoon

import cats.~>
import simulacrum.typeclass

@typeclass trait InvariantK[A[_[_]]] {
  def imapK[F[_], G[_]](af: A[F])(fk: F ~> G)(gK: G ~> F): A[G]
}
