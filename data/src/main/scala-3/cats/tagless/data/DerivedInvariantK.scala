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

@implicitNotFound("""Could not derive InvariantK for ${F}.
Make sure it satisfies one of the following conditions:
  * generic case class where all fields form InvariantK
  * generic sealed trait where all subclasses form InvariantK
  * generic enum where all variants form InvariantK""")
type DerivedInvariantK[F[_[_]]] = Derived[InvariantK[F]]
object DerivedInvariantK:
  type Of[F[_[_]]] = InvariantK[F] || DerivedInvariantK[F]

  inline def apply[F[_[_]]]: InvariantK[F] =
    import DerivedInvariantK.given
    summonInline[DerivedInvariantK[F]].instance

  given const[A]: DerivedInvariantK[ConstK[A]#λ] = new InvariantK[ConstK[A]#λ]:
    override def imapK[F[_], G[_]](af: A)(fk: F ~> G)(gk: G ~> F): A = af

  given generic[F[_[_]]](using inst: => Instances[Of, F]): DerivedInvariantK[F] =
    given Instances[InvariantK, F] = inst.unify
    new Generic[InvariantK, F] {}

  trait Generic[T[f[_[_]]] <: InvariantK[f], F[_[_]]: InstancesOf[T]] extends InvariantK[F]:
    final override def imapK[A[_], B[_]](fa: F[A])(fk: A ~> B)(gk: B ~> A): F[B] =
      Instances.map(fa)([f[_[_]]] => (F: T[f], fa: f[A]) => F.imapK(fa)(fk)(gk))
