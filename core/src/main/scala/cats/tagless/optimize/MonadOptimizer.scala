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

package cats.tagless.optimize

import cats.{Monad, Monoid, ~>}
import cats.data._
import cats.tagless.ApplyK


trait MonadOptimizer[Alg[_[_]], F[_]] {

  type M

  def monoidM: Monoid[M]
  def monadF: Monad[F]

  def applyK: ApplyK[Alg]

  def rebuild(interp: Alg[F]): Alg[Kleisli[F, M, ?]]

  def extract: Alg[? => M]


  def optimizeM[A](p: Program[Alg, Monad, A]): Alg[F] => F[A] = { interpreter =>
    implicit val M: Monoid[M] = monoidM
    implicit val F: Monad[F] = monadF

    type Prod[X] = Tuple2K[Kleisli[F, M, ?], ? => M, X]

    val productAlg =
      applyK.productK[Kleisli[F, M, ?], ? => M](rebuild(interpreter), extract)

    val withState: Alg[StateT[F, M, ?]] =
      applyK.mapK(productAlg)(new (Prod ~> StateT[F, M, ?]) {
        def apply[X](fx: Prod[X]): StateT[F, M, X] =
          StateT(m => F.map(fx.first.run(m))(x => M.combine(fx.second(x), m) -> x))
      })

    p(withState).runEmptyA
  }

}

object MonadOptimizer {
  def apply[Alg[_[_]], F[_]](implicit ev: MonadOptimizer[Alg, F]): MonadOptimizer[Alg, F] = ev
}
