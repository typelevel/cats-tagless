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

package cats.tagless.instances

import cats.data.{IdT, Tuple2K}
import cats.tagless.ApplyK
import cats.~>

trait IdTInstances {

  implicit def catsTaglessApplyKForIdT[A]: ApplyK[IdT[*[_], A]] =
    idTInstance.asInstanceOf[ApplyK[IdT[*[_], A]]]

  private[this] val idTInstance: ApplyK[IdT[*[_], Any]] = new ApplyK[IdT[*[_], Any]] {
    def mapK[F[_], G[_]](af: IdT[F, Any])(fk: F ~> G) = af.mapK(fk)
    def productK[F[_], G[_]](af: IdT[F, Any], ag: IdT[G, Any]) =
      IdT(Tuple2K(af.value, ag.value))
  }
}
