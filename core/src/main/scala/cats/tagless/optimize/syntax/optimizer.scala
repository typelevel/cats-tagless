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

import cats.Applicative
import cats.tagless.optimize.{Optimizer, Program}

trait OptimizerSyntax {
  implicit def optimizerOps[Alg[_[_]], A](value: Program[Alg, Applicative, A]): OptimizerOps[Alg, A] =
    new OptimizerOps[Alg, A](value)
}

class OptimizerOps[Alg[_[_]], A](val value: Program[Alg, Applicative, A]) extends AnyVal {
  def optimize[F[_]](interp: Alg[F])(implicit O: Optimizer[Alg, F]): F[A] = O.optimize(value)(interp)
}

object optimizer extends OptimizerSyntax
