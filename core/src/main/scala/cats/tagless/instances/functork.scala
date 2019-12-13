package cats.tagless.instances

import cats.arrow.FunctionK
import cats.tagless.FunctorK
import cats.~>

trait FunctorKInstances extends FunctorKInstances01 {
  implicit def functorKFunctionK[J[_]]: FunctorK[FunctionK[J, ?[_]]] = {
    type FunK[F[_]] = FunctionK[J, F]
    new FunctorK[FunK] {
      override def mapK[F[_], G[_]](af: FunK[F])(fn: ~>[F, G]): FunK[G] = fn.compose(af)
    }
  }
}
trait FunctorKInstances01