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
import cats.tagless.SemigroupalK

trait ApplyHK[TC[_[_[_]]]] extends FunctorHK[TC] with SemigroupalHK[TC] {
  def map2HK[A1[_[_]], A2[_[_]], A3[_[_]]](af: TC[A1], ag: TC[A2])(f: TupleHK[A1, A2, ?[_]] ≈> A3): TC[A3] =
    mapHK(productHK(af, ag))(f)
}

object ApplyHK {
  def apply[TC[_[_[_]]]](implicit source: ApplyHK[TC]): ApplyHK[TC] = source

//  def aa: SemigroupalHK[SemigroupalK] = new SemigroupalHK[SemigroupalK] {
//    def productHK[A1[_[_]], A2[_[_]]](tc1: SemigroupalK[A1], tc2: SemigroupalK[A2]): SemigroupalK[TupleHK[A1, A2, ?[_]]] =
//      new IsoAlgSemigroupal[A1, A2] {
//        override def F: SemigroupalK[A1] = ???
//
//        override def to: ≈>[A1, A2] = ???
//
//        override def from: ≈>[A2, A1] = ???
//      }
//  }

  trait IsoAlgSemigroupal[A1[_[_]], A2[_[_]]] extends SemigroupalK[A2] {
    def F: SemigroupalK[A1]
    def to: A1 ≈> A2
    def from: A2 ≈> A1
    override def productK[F[_], G[_]](af: A2[F], ag: A2[G]): A2[Tuple2K[F, G, ?]] =
      to(F.productK(from(af), from(ag)))
  }


}