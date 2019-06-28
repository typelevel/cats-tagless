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

trait Algebra2NatTrans[-Alg1[_[_],_,_], +Alg2[_[_],_,_]] { self =>
  def apply[F[_], A, B](alg1: Alg1[F, A, B]): Alg2[F, A, B]

  def compose[Alg[_[_],_,_]](alg3: Algebra2NatTrans[Alg, Alg1]): Algebra2NatTrans[Alg, Alg2] =
    new Algebra2NatTrans[Alg, Alg2] {
      override def apply[F[_], A, B](alg: Alg[F, A, B]): Alg2[F, A, B] = self(alg3(alg))
    }
}

object Algebra2NatTrans {
  /**
    * The identity transformation of `Alg` to `Alg`
    */
  def id[Alg[_[_],_,_]]: Algebra2NatTrans[Alg, Alg] = new Algebra2NatTrans[Alg, Alg] {
    final override def apply[F[_], A, B](f: Alg[F, A, B]): Alg[F, A, B] = f
  }
}