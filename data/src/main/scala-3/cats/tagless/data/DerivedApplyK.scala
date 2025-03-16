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

package cats.tagless
package data

import cats.tagless.derived.*
import shapeless3.deriving.K11.*

import scala.annotation.*
import scala.compiletime.*

@implicitNotFound("""Could not derive ApplyK for ${F}.
Make sure it satisfies one of the following conditions:
  * generic case class where all fields form ApplyK""")
type DerivedApplyK[F[_[_]]] = Derived[ApplyK[F]]
object DerivedApplyK:
  type Of[F[_[_]]] = ApplyK[F] || DerivedApplyK[F]

  @nowarn("msg=unused import")
  inline def apply[F[_[_]]]: ApplyK[F] =
    import DerivedApplyK.given
    summonInline[DerivedApplyK[F]].instance

  given product[F[_[_]]](using inst: => ProductInstances[Of, F]): DerivedApplyK[F] =
    given ProductInstances[ApplyK, F] = inst.unify
    new Product[ApplyK, F] {}

  trait Product[T[f[_[_]]] <: ApplyK[f], F[_[_]]]
      extends DerivedFunctorK.Generic[T, F]
      with DerivedSemigroupalK.Product[T, F]
      with ApplyK[F]
