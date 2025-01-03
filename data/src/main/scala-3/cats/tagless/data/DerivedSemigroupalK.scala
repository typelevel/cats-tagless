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

import cats.data.Tuple2K
import cats.tagless.derived.Derived
import shapeless3.deriving.K11.*

import scala.annotation.*
import scala.compiletime.*

@implicitNotFound("""Could not derive SemigroupalK for ${F}.
Make sure it satisfies one of the following conditions:
  * generic case class where all fields form SemigroupalK""")
type DerivedSemigroupalK[F[_[_]]] = Derived[SemigroupalK[F]]
object DerivedSemigroupalK:
  type Or[F[_[_]]] = Derived.Or[SemigroupalK[F]]

  @nowarn("msg=unused import")
  inline def apply[F[_[_]]]: SemigroupalK[F] =
    import DerivedSemigroupalK.given
    summonInline[DerivedSemigroupalK[F]].instance

  given product[F[_[_]]](using inst: => ProductInstances[Or, F]): DerivedSemigroupalK[F] =
    new Product[SemigroupalK, F](using inst.unify) {}

  trait Product[T[f[_[_]]] <: SemigroupalK[f], F[_[_]]: ProductInstancesOf[T]] extends SemigroupalK[F]:
    final override def productK[A[_], B[_]](fa: F[A], fb: F[B]): F[Tuple2K[A, B, *]] =
      ProductInstances.map2(fa, fb)([f[_[_]]] => (F: T[f], fa: f[A], fb: f[B]) => F.productK(fa, fb))
