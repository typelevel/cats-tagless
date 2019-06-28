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

package cats.iso

import cats.~>

trait BiNaturalTransformation[F[_,_], G[_,_]] { self =>

  def apply[A, B](f: F[A, B]): G[A, B]

  def compose[E[_, _]](f: BiNaturalTransformation[E, F]): BiNaturalTransformation[E, G] =
    new BiNaturalTransformation[E, G] {
      def apply[A, B](eab: E[A, B]): G[A, B] = self(f(eab))
    }

  def unliftA[A]: F[A, ?] ~> G[A, ?] = {
    type FA[ᵒ] = F[A, ᵒ]
    type GA[ᵒ] = G[A, ᵒ]
    λ[FA ~> GA](self.apply(_))
  }

  def unliftB[B]: F[?, B] ~> G[?, B] = {
    type FA[ᵒ] = F[ᵒ, B]
    type GA[ᵒ] = G[ᵒ, B]
    λ[FA ~> GA](self.apply(_))
  }
}

object BiNaturalTransformation {
  /**
    * The identity transformation of `F` to `F`
    */
  def id[F[_,_]]: BiNaturalTransformation[F, F] = new BiNaturalTransformation[F, F] {
    final override def apply[A, B](f: F[A, B]): F[A, B] = f
  }

}
