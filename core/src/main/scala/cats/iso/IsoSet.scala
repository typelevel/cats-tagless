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

object IsoSet {
  def apply[A, B](to: A => B, from: B => A): A <=> B = {
    val _to   = to
    val _from = from
    new Iso[Function1, A, B] {
      override val to   = _to
      override val from = _from
    }
  }
}

case class IsoSetOps[A, B](self: A <=> B) {
  def derive[F[_]](implicit der: IsoDerivable[F], ev: F[A]): F[B] =
    der.derive(self)(ev)
}
