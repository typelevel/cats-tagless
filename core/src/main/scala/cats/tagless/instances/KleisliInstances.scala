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

import cats.data.{Kleisli, Tuple2K}
import cats.tagless.ApplyK
import cats.~>

trait KleisliInstances {

  implicit def catsTaglessApplyKForKleisli[A, B]: ApplyK[Kleisli[?[_], A, B]] =
    kleisliInstance.asInstanceOf[ApplyK[Kleisli[?[_], A, B]]]

  private[this] val kleisliInstance: ApplyK[Kleisli[?[_], Any, Any]] = new ApplyK[Kleisli[?[_], Any, Any]] {
    def mapK[F[_], G[_]](af: Kleisli[F, Any, Any])(fk: F ~> G) = af.mapK(fk)
    def productK[F[_], G[_]](af: Kleisli[F, Any, Any], ag: Kleisli[G, Any, Any]) =
      Kleisli(x => Tuple2K(af.run(x), ag.run(x)))
  }
}
