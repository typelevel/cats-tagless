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

import cats.data.{EitherT, Tuple2K}
import cats.tagless.ApplyK
import cats.~>

trait EitherTInstances {

  implicit def catsTaglessApplyKForEitherT[A, B]: ApplyK[EitherT[?[_], A, B]] =
    eitherTInstance.asInstanceOf[ApplyK[EitherT[?[_], A, B]]]

  private[this] val eitherTInstance: ApplyK[EitherT[?[_], Any, Any]] = new ApplyK[EitherT[?[_], Any, Any]] {
    def mapK[F[_], G[_]](af: EitherT[F, Any, Any])(fk: F ~> G) = af.mapK(fk)
    def productK[F[_], G[_]](af: EitherT[F, Any, Any], ag: EitherT[G, Any, Any]) =
      EitherT(Tuple2K(af.value, ag.value))
  }
}
