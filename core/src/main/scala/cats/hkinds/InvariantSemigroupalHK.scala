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

trait InvariantSemigroupalHK[TC[_[_[_]]]] extends InvariantHK[TC] with SemigroupalHK[TC] {
  def imap2HK[A1[_[_]], A2[_[_]], Alg[_[_]]]
    (tcAlg1: TC[A1], tcAlg2: TC[A2])
    (f: TupleHK[A1, A2, ?[_]] ≈> Alg)
    (g: Alg ≈> TupleHK[A1, A2, ?[_]]): TC[Alg] = imapHK(productHK(tcAlg1, tcAlg2))(f)(g)
}
