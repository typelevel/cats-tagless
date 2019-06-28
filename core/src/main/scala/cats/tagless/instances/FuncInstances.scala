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

import cats.data.{Func, Tuple2K}
import cats.tagless.ApplyK
import cats.~>

trait FuncInstances {

  implicit def catsTaglessApplyKForFunc[A, B]: ApplyK[Func[?[_], A, B]] =
    funcInstance.asInstanceOf[ApplyK[Func[?[_], A, B]]]

  private[this] val funcInstance: ApplyK[Func[?[_], Any, Any]] = new ApplyK[Func[?[_], Any, Any]] {
    def mapK[F[_], G[_]](af: Func[F, Any, Any])(fk: F ~> G) = af.mapK(fk)
    def productK[F[_], G[_]](af: Func[F, Any, Any], ag: Func[G, Any, Any]) =
      Func.func(x => Tuple2K(af.run(x), ag.run(x)))
  }
}
