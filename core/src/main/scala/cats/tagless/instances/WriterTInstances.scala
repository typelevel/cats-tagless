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

import cats.data.{Tuple2K, WriterT}
import cats.tagless.ApplyK
import cats.~>

trait WriterTInstances {

  implicit def catsTaglessApplyKForWriterT[A, B]: ApplyK[WriterT[?[_], A, B]] =
    writerTInstance.asInstanceOf[ApplyK[WriterT[?[_], A, B]]]

  private[this] val writerTInstance: ApplyK[WriterT[?[_], Any, Any]] = new ApplyK[WriterT[?[_], Any, Any]] {
    def mapK[F[_], G[_]](af: WriterT[F, Any, Any])(fk: F ~> G) = af.mapK(fk)
    def productK[F[_], G[_]](af: WriterT[F, Any, Any], ag: WriterT[G, Any, Any]) =
      WriterT(Tuple2K(af.run, ag.run))
  }
}
