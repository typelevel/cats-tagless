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

trait ContravariantSemigroupalHK[TC[_[_[_]]]] extends ContravariantHK[TC] with SemigroupalHK[TC] {
  def contramap2HK[A1[_[_]], A2[_[_]], A3[_[_]]](af: TC[A1], ag: TC[A2])(f: A3 ≈> TupleHK[A1, A2, ?[_]]): TC[A3] =
    contramapHK(productHK(af, ag))(f)
}

object ContravariantSemigroupalHK {
  def apply[TC[_[_[_]]]](implicit source: ContravariantSemigroupalHK[TC]): ContravariantSemigroupalHK[TC] = source
}