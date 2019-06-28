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

package cats.hkinds

import cats.data.Tuple2K
import cats.tagless._

trait SemigroupalHK[TC[_[_[_]]]] {
  def productHK[A1[_[_]], A2[_[_]]](tc1: TC[A1], tc2: TC[A2]): TC[TupleHK[A1, A2, ?[_]]]
}

object SemigroupalHK {
  def apply[TC[_[_[_]]]](implicit source: SemigroupalHK[TC]): SemigroupalHK[TC] = source

  import cats.hkinds.SemigroupalHKTraits._
  val semHKSemigroupalK: SemigroupalHK[SemigroupalK] = new SemHKSemigroupalK {}

}

object SemigroupalHKTraits extends SemigroupalHKTraits

trait SemigroupalHKTraits extends SemigroupalHKTraitsPrivate {
  trait SemHKSemigroupalK extends SemigroupalHK[SemigroupalK] {
    def productHK[A1[_[_]], A2[_[_]]](tc1: SemigroupalK[A1], tc2: SemigroupalK[A2]): SemigroupalK[TupleHK[A1, A2, ?[_]]] =
      new TupHKSemigroupalK[A1, A2] { val F = tc1; val G = tc2 }
  }
}

trait SemigroupalHKTraitsPrivate {
  sealed trait TupHKSemigroupalK[A1[_[_]], A2[_[_]]] extends SemigroupalK[TupleHK[A1, A2, ?[_]]] {
    def F: SemigroupalK[A1]
    def G: SemigroupalK[A2]
    override def productK[F[_], G[_]](af: TupleHK[A1, A2, F], ag: TupleHK[A1, A2, G]): TupleHK[A1, A2, Tuple2K[F, G, ?]] =
      TupleHK[A1, A2, Tuple2K[F, G, ?]](F.productK(af.first, ag.first), G.productK(af.second, ag.second))
  }
}