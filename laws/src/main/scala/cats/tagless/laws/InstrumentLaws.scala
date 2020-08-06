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

package cats.tagless.laws

import cats.laws._
import cats.tagless.aop.{Instrument, Instrumentation}
import cats.~>

trait InstrumentLaws[F[_[_]]] extends FunctorKLaws[F] {
  implicit def F: Instrument[F]

  def instrumentPreservingSemantics[A[_]](fa: F[A]): IsEq[F[A]] =
    F.mapK(F.instrument(fa))(λ[Instrumentation[A, *] ~> A](_.value)) <-> fa
}

object InstrumentLaws {
  def apply[F[_[_]]](implicit ev: Instrument[F]): InstrumentLaws[F] =
    new InstrumentLaws[F] { val F = ev }
}
