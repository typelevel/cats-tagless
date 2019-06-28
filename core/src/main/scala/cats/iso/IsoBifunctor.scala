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


object IsoBifunctor {
  /**Convenience constructor to implement `F <~~> G` from F ~~> G and G ~~> F */
  def apply[F[_, _], G[_, _]](to: F ~~> G, from: G ~~> F): F <~~> G = {
    val _to   = to
    val _from = from
    new (F <~~> G) {
      val to = _to
      val from = _from
    }
  }

  val isoBiConst2Const: ShapeConst <~~> CatsConst =
    new Template[ShapeConst, CatsConst] {
      def _to  [A, B](fa: ShapeConst[A, B]): CatsConst[A, B] = CatsConst.of[B](fa)
      def _from[A, B](ga: CatsConst[A, B]): ShapeConst[A, B] = ga.getConst
    }

  /**Convenience template trait to implement `<~~>` */
  trait Template[F[_, _], G[_, _]] extends IsoBifunctor[F, G] {
    final val to: BiNaturalTransformation[F, G] = new (F ~~> G) {
      def apply[A, B](fab: F[A, B]): G[A, B] = _to[A, B](fab)
    }
    final val from: BiNaturalTransformation[G, F] = new (G ~~> F) {
      def apply[A, B](gab: G[A, B]): F[A, B] = _from[A, B](gab)
    }

    def _to[A, B](fa: F[A, B]): G[A, B]
    def _from[A, B](ga: G[A, B]): F[A, B]
  }

}

case class IsoBifunctorOps[F[_,_], G[_,_]](self: <~~>[F, G]) {
  def derive[Alg[_[_,_]]](implicit isoAlg: Iso3Derivation[Alg], ev: Alg[G]): Alg[F] =
    isoAlg.get(self)
}
