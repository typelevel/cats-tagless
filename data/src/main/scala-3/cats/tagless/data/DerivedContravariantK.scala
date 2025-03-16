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

@implicitNotFound("""Could not derive ContravariantK for ${F}.
Make sure it satisfies one of the following conditions:
  * generic case class where all fields form ContravariantK
  * generic sealed trait where all subclasses form ContravariantK
  * generic enum where all variants form ContravariantK""")
type DerivedContravariantK[F[_[_]]] = Derived[ContravariantK[F]]
object DerivedContravariantK:
  type Of[F[_[_]]] = ContravariantK[F] || DerivedContravariantK[F]

  @nowarn("msg=unused import")
  inline def apply[F[_[_]]]: ContravariantK[F] =
    import DerivedContravariantK.given
    summonInline[DerivedContravariantK[F]].instance

  given const[A]: DerivedContravariantK[ConstK[A]#λ] = new ContravariantK[ConstK[A]#λ]:
    override def contramapK[F[_], G[_]](af: A)(fk: G ~> F): A = af

  given generic[F[_[_]]](using inst: => Instances[Of, F]): DerivedContravariantK[F] =
    given Instances[ContravariantK, F] = inst.unify
    new Generic[ContravariantK, F] {}

  trait Generic[T[f[_[_]]] <: ContravariantK[f], F[_[_]]: InstancesOf[T]] extends ContravariantK[F]:
    final override def contramapK[A[_], B[_]](fa: F[A])(f: B ~> A): F[B] =
      Instances.map(fa)([f[_[_]]] => (F: T[f], fa: f[A]) => F.contramapK(fa)(f))
