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

package cats.tagless.optimize.syntax

import cats.Monad
import cats.tagless.optimize.{Program, MonadOptimizer}

trait MonadSyntax {
  implicit def monadOptimizerOps[Alg[_[_]], A](value: Program[Alg, Monad, A]): MonadOptimizerOps[Alg, A] =
    new MonadOptimizerOps(value)
}

class MonadOptimizerOps[Alg[_[_]], A](val value: Program[Alg, Monad, A]) extends AnyVal {
  def optimizeM[F[_]: Monad](interp: Alg[F])(implicit O: MonadOptimizer[Alg, F]): F[A] =
    O.optimizeM(value)(interp)
}

object monadoptimizer extends MonadSyntax
