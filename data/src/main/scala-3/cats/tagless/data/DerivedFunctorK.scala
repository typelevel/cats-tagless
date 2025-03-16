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

import cats.~>
import cats.tagless.derived.*
import shapeless3.deriving.K11.*

import scala.annotation.*
import scala.compiletime.*

@implicitNotFound("""Could not derive FunctorK for ${F}.
Make sure it satisfies one of the following conditions:
  * generic case class where all fields form FunctorK
  * generic sealed trait where all subclasses form FunctorK
  * generic enum where all variants form FunctorK""")
type DerivedFunctorK[F[_[_]]] = Derived[FunctorK[F]]
object DerivedFunctorK:
  type Of[F[_[_]]] = FunctorK[F] || DerivedFunctorK[F]

  @nowarn("msg=unused import")
  inline def apply[F[_[_]]]: FunctorK[F] =
    import DerivedFunctorK.given
    summonInline[DerivedFunctorK[F]].instance

  given generic[F[_[_]]](using inst: => Instances[Of, F]): DerivedFunctorK[F] =
    given Instances[FunctorK, F] = inst.unify
    new Generic[FunctorK, F] {}

  trait Generic[T[f[_[_]]] <: FunctorK[f], F[_[_]]: InstancesOf[T]] extends FunctorK[F]:
    final override def mapK[A[_], B[_]](fa: F[A])(fk: A ~> B): F[B] =
      Instances.map(fa)([f[_[_]]] => (F: T[f], fa: f[A]) => F.mapK(fa)(fk))
