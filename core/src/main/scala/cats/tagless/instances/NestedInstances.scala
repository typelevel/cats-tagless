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

package cats.tagless.instances

import cats.data.Nested
import cats.tagless.FunctorK
import cats.~>

trait NestedInstances {

  implicit def catsTaglessFunctorKForNested[G[_], A]: FunctorK[Nested[?[_], G, A]] =
    nestedInstance.asInstanceOf[FunctorK[Nested[?[_], G, A]]]

  private[this] val nestedInstance: FunctorK[Nested[?[_], Any, Any]] = new FunctorK[Nested[?[_], Any, Any]] {
    def mapK[F[_], G[_]](af: Nested[F, Any, Any])(fk: F ~> G) = af.mapK(fk)
  }
}
