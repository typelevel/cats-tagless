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

package cats.iso

import cats.arrow.FunctionK
import cats.~>


object IsoFunctor extends Iso2DerivableInstances {

  def naturalReflexive[F[_]]: F <~> F = new Template[F, F] {
    def to[A](fa: F[A]): F[A] = fa
    def from[A](fa: F[A]): F[A] = fa
  }

  def isoConst2ConstA[A]: ShapeConst[A, ?] <~> CatsConst[A, ?] = IsoBifunctor.isoBiConst2Const.unliftA
  def isoConst2ConstB[B]: ShapeConst[?, B] <~> CatsConst[?, B] = IsoBifunctor.isoBiConst2Const.unliftB

  /**Convenience constructor to implement `F <~> G` from F ~> G and G ~> F */
  def apply[F[_], G[_]](to: F ~> G, from: G ~> F): F <~> G = {
    val _to   = to
    val _from = from
    new (F <~> G) {
      override val to = _to
      override val from = _from
    }
  }

  /**Convenience template trait to implement `<~>` */
  trait Template[F[_], G[_]] extends (F <~> G) {
    override final val to: F ~> G = FunctionK.lift(to)
    override final val from: G ~> F = FunctionK.lift(from)

    def to[A](fa: F[A]): G[A]
    def from[A](ga: G[A]): F[A]
  }
}

case class IsoFunctorOps[F[_], G[_]](self: <~>[F, G]) {
  def derive[Alg[_[_]]](implicit isoAlg: Iso2Derivable[Alg], ev: Alg[F]): Alg[G] =
    isoAlg.derive(self)(ev)
}
