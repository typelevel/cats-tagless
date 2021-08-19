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

import cats.~>

import scala.annotation.implicitNotFound

/** Sort of a higher kinded Functor, but, well, it's complicated. See Daniel Spiewak's comment here
  * https://github.com/typelevel/cats/issues/2697#issuecomment-453883055 Also explains why this isn't in `cats-core`.
  */
@implicitNotFound("Could not find an instance of FunctorK for ${Alg}")
trait FunctorK[Alg[_[_]]] extends InvariantK[Alg] {
  def mapK[F[_], G[_]](af: Alg[F])(fk: F ~> G): Alg[G]
  override def imapK[F[_], G[_]](af: Alg[F])(fk: F ~> G)(gk: G ~> F): Alg[G] = mapK(af)(fk)
}

object FunctorK {

  // =======================
  // Generated by simulacrum
  // =======================

  @inline def apply[Alg[_[_]]](implicit instance: FunctorK[Alg]): FunctorK[Alg] = instance

  trait AllOps[Alg[_[_]], F[_]] extends Ops[Alg, F] with InvariantK.AllOps[Alg, F] {
    type TypeClassType <: FunctorK[Alg]
    val typeClassInstance: TypeClassType
  }

  object ops {
    implicit def toAllFunctorKOps[Alg[_[_]], F[_]](target: Alg[F])(implicit tc: FunctorK[Alg]): AllOps[Alg, F] {
      type TypeClassType = FunctorK[Alg]
    } = new AllOps[Alg, F] {
      type TypeClassType = FunctorK[Alg]
      val self = target
      val typeClassInstance: TypeClassType = tc
    }
  }

  trait Ops[Alg[_[_]], F[_]] {
    type TypeClassType <: FunctorK[Alg]
    val typeClassInstance: TypeClassType
    def self: Alg[F]
    def mapK[G[_]](fk: F ~> G): Alg[G] =
      typeClassInstance.mapK[F, G](self)(fk)
  }

  trait ToFunctorKOps {
    implicit def toFunctorKOps[Alg[_[_]], F[_]](target: Alg[F])(implicit tc: FunctorK[Alg]): Ops[Alg, F] {
      type TypeClassType = FunctorK[Alg]
    } = new Ops[Alg, F] {
      type TypeClassType = FunctorK[Alg]
      val self = target
      val typeClassInstance: TypeClassType = tc
    }
  }

  object nonInheritedOps extends ToFunctorKOps
}
