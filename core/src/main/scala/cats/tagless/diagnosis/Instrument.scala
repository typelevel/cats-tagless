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

package cats.tagless.diagnosis

import simulacrum.typeclass


final case class Instrumentation[F[_], A](
  value: F[A],
  algebraName: String,
  methodName: String
)

/**
  * Type classes for instrumenting algebras.
  * This feature is experimental, API is likely to change.
  * @tparam Alg The algebra type you want to instrument.
  */
@typeclass
trait Instrument[Alg[_[_]]] {
  def instrument[F[_]](af: Alg[F]): Alg[Instrumentation[F, *]]
}
