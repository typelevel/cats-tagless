package mainecoon

import cats.~>
import simulacrum.typeclass

@typeclass
trait FunctorK[A[_[_]]] extends InvariantK[A] {
  def mapK[F[_], G[_]](af: A[F])(fk: F ~> G): A[G]

  override def imapK[F[_], G[_]](af: A[F])(fk: F ~> G)(gK: G ~> F): A[G] = mapK(af)(fk)

}




