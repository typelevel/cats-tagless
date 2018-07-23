/*
 * Copyright 2017 Kailuo Wang
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

import cats.{FlatMap, Apply, Semigroup}
import cats.data.Const

trait SemigroupalOptimizer[Alg[_[_]], F[_]] {

  type M

  def semigroupM: Semigroup[M]
  def flatMapF: FlatMap[F]

  def extract: Alg[Const[M, ?]]
  def rebuild(m: M, interpreter: Alg[F]): F[Alg[F]]

  def nonEmptyOptimize[A](p: Program[Alg, Apply, A]): Alg[F] => F[A] = { interpreter =>
    implicit val M: Semigroup[M] = semigroupM
    implicit val F: FlatMap[F] = flatMapF
    val m: M = p(extract).getConst

    FlatMap[F].flatMap(rebuild(m, interpreter))(interp => p(interp))
  }

}

object SemigroupalOptimizer {
  def apply[Alg[_[_]], F[_]](implicit ev: SemigroupalOptimizer[Alg, F]): SemigroupalOptimizer[Alg, F] = ev
}
