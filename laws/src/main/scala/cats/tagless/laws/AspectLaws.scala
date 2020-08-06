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
import cats.tagless.aop.{Aspect, Instrumentation}
import cats.~>

trait AspectLaws[F[_[_]], Dom[_], Cod[_]] extends InstrumentLaws[F] {
  implicit def F: Aspect[F, Dom, Cod]

  def weavePreservingSemantics[A[_]](fa: F[A]): IsEq[F[A]] =
    F.mapK(F.weave(fa))(λ[Aspect.Weave[A, Dom, Cod, *] ~> A](_.codomain.target)) <-> fa

  def weaveInstrumentConsistency[A[_]](fa: F[A]): IsEq[F[Instrumentation[A, *]]] =
    F.mapK(F.weave(fa))(Aspect.Weave.instrumentationK) <-> F.instrument(fa)
}

object AspectLaws {
  def apply[F[_[_]], Dom[_], Cod[_]](implicit ev: Aspect[F, Dom, Cod]): AspectLaws[F, Dom, Cod] =
    new AspectLaws[F, Dom, Cod] { val F = ev }
}
