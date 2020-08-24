/*
 * Copyright 2019 cats-tagless maintainers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cats.tagless

import cats._
import cats.arrow._
import cats.implicits._
import simulacrum.typeclass

/**
 * Sort of a higher kinded Functor, but, well, it's complcated.
 * See Daniel Spiewak's comment here
 * https://github.com/typelevel/cats/issues/2697#issuecomment-453883055
 * Also explains why this isn't in `cats-core`.
**/
@typeclass
trait FunctorK[A[_[_]]] extends InvariantK[A] {
  def mapK[F[_], G[_]](af: A[F])(fk: F ~> G): A[G]

  override def imapK[F[_], G[_]](af: A[F])(fk: F ~> G)(gK: G ~> F): A[G] = mapK(af)(fk)

  /** Perform a transformation on all errors. */
  final def transformError[F[_], E](af: A[F])(f: E => F[E])(implicit F: MonadError[F, E]): A[F] =
    mapK(af)(
      new FunctionK[F, F]{
        override def apply[B](fa: F[B]): F[B] =
          fa.handleErrorWith(e =>
            f(e).flatMap(e => F.raiseError[B](e))
          )
      }
    )

  /** Perform a action on error. For example, one can use this to add logging to
    * all errors for a given instance of [[FunctorK]].
    */
  final def flatTapOnError[F[_], E](af: A[F])(f: E => F[Unit])(implicit F: MonadError[F, E]): A[F] =
    transformError[F, E](af)(
      e => f(e) *> F.pure(e)
    )
}
