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

import cats.data.Cokleisli
import cats.tagless.ContravariantK
import cats.~>

trait CokleisliInstances {

  implicit def catsTaglessContravariantKForCokleisli[A, B]: ContravariantK[Cokleisli[*[_], A, B]] =
    cokleisliInstance.asInstanceOf[ContravariantK[Cokleisli[*[_], A, B]]]

  private[this] val cokleisliInstance: ContravariantK[Cokleisli[*[_], Any, Any]] =
    new ContravariantK[Cokleisli[*[_], Any, Any]] {
      def contramapK[F[_], G[_]](af: Cokleisli[F, Any, Any])(fk: G ~> F) = Cokleisli(ga => af.run(fk(ga)))
    }
}
