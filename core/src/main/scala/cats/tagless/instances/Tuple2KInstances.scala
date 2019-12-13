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

import cats.data.Tuple2K
import cats.tagless.{ApplyK, FunctorK}
import cats.{SemigroupK, ~>}

trait Tuple2KInstances extends Tuple2KInstances1 {
  implicit def catsTaglessApplyKForTuple2K[H[_]: SemigroupK, A]: ApplyK[Tuple2K[H, ?[_], A]] =
    new ApplyK[Tuple2K[H, ?[_], A]] {
      def mapK[F[_], G[_]](af: Tuple2K[H, F, A])(fk: F ~> G) = af.mapK(fk)
      def productK[F[_], G[_]](af: Tuple2K[H, F, A], ag: Tuple2K[H, G, A]) =
        Tuple2K(SemigroupK[H].combineK(af.first, ag.first), Tuple2K(af.second, ag.second))
    }
}

private[instances] trait Tuple2KInstances1 {

  implicit def catsTaglessFunctorKForTuple2K[F[_], A]: FunctorK[Tuple2K[F, ?[_], A]] =
    tuple2KFunctorK.asInstanceOf[FunctorK[Tuple2K[F, ?[_], A]]]

  private[this] val tuple2KFunctorK: FunctorK[Tuple2K[Any, ?[_], Any]] = new FunctorK[Tuple2K[Any, ?[_], Any]] {
    def mapK[F[_], G[_]](af: Tuple2K[Any, F, Any])(fk: F ~> G) = af.mapK(fk)
  }
}
