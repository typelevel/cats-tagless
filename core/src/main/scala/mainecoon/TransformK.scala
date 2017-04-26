package mainecoon

import cats.~>
import simulacrum.typeclass

@typeclass
trait TransformK[A[_[_]]] {
  def mapK[F[_], G[_]](af: A[F])(fk: F ~> G): A[G]
}




