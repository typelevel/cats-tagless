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

package cats.tagless.instances

import cats.data.{OneAnd, Tuple2K}
import cats.tagless.{ApplyK, FunctorK}
import cats.{Semigroup, ~>}

trait OneAndInstances extends OneAndInstances1 {
  implicit def catsTaglessApplyKForOneAnd[A: Semigroup]: ApplyK[OneAnd[?[_], A]] = new ApplyK[OneAnd[?[_], A]] {
    def mapK[F[_], G[_]](af: OneAnd[F, A])(fk: F ~> G) = af.mapK(fk)
    def productK[F[_], G[_]](af: OneAnd[F, A], ag: OneAnd[G, A]) =
      OneAnd(Semigroup[A].combine(af.head, ag.head), Tuple2K(af.tail, ag.tail))
  }
}

private[instances] trait OneAndInstances1 {

  implicit def catsTaglessFunctorKForOneAnd[A]: FunctorK[OneAnd[?[_], A]] =
    oneAndFunctorK.asInstanceOf[FunctorK[OneAnd[?[_], A]]]

  private[this] val oneAndFunctorK: FunctorK[OneAnd[?[_], Any]] = new FunctorK[OneAnd[?[_], Any]] {
    def mapK[F[_], G[_]](af: OneAnd[F, Any])(fk: F ~> G) = af.mapK(fk)
  }
}
