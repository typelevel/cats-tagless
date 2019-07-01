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

import cats.{Applicative, FlatMap, Monad, Monoid, Semigroup}

trait Optimizer[Alg[_[_]], F[_]] extends SemigroupalOptimizer[Alg, F] {

  def semigroupM: Semigroup[M] = monoidM
  def flatMapF: FlatMap[F] = monadF

  def monoidM: Monoid[M]
  def monadF: Monad[F]

  def optimize[A](p: Program[Alg, Applicative, A]): Alg[F] => F[A] = { interpreter =>
    implicit val M: Monoid[M] = monoidM
    implicit val F: Monad[F] = monadF
    val m: M = p(extract).getConst

    Monad[F].flatMap(rebuild(m, interpreter))(interp => p(interp))
  }

}

object Optimizer {

  def apply[Alg[_[_]], F[_]](implicit ev: Optimizer[Alg, F]): Optimizer[Alg, F] = ev

  implicit class OptimizerOps[Alg[_[_]], A](val value: Program[Alg, Applicative, A]) extends AnyVal {
    def optimize[F[_]: Monad](interp: Alg[F])(implicit O: Optimizer[Alg, F]): F[A] = O.optimize(value)(interp)
  }
}