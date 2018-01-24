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

package mainecoon

import cats.~>
import simulacrum.typeclass

@typeclass
trait FunctorK[A[_[_]]] extends InvariantK[A] {
  def mapK[F[_], G[_]](af: A[F])(fk: F ~> G): A[G]

  override def imapK[F[_], G[_]](af: A[F])(fk: F ~> G)(gK: G ~> F): A[G] = mapK(af)(fk)
}




