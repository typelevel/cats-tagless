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

trait BiAlgebraNatTrans[-Alg1[_[_,_]], +Alg2[_[_,_]]] { self =>
  def apply[F[_,_]](alg1: Alg1[F]): Alg2[F]

  def compose[Alg[_[_,_]]](trans: BiAlgebraNatTrans[Alg, Alg1]): BiAlgebraNatTrans[Alg, Alg2] =
    new BiAlgebraNatTrans[Alg, Alg2] {
      def apply[F[_,_]](alg: Alg[F]): Alg2[F] = self(trans(alg))
    }
}

object BiAlgebraNatTrans {
  /**
    * The identity transformation of `Alg` to `Alg`
    */
  final def id[Alg[_[_,_]]]: BiAlgebraNatTrans[Alg, Alg] = new BiAlgebraNatTrans[Alg, Alg] {
    def apply[F[_,_]](alg: Alg[F]): Alg[F] = alg
  }
}
