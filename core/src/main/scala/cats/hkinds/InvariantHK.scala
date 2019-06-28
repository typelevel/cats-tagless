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

import cats.tagless._
import cats.~>

trait InvariantHK[TC[_[_[_]]]] {
  def imapHK[A1[_[_]], A2[_[_]]](tc: TC[A1])(ab: A1 ≈> A2)(ba: A2 ≈> A1): TC[A2]
}

object InvariantHK {
  def apply[TC[_[_[_]]]](implicit source: InvariantHK[TC]): InvariantHK[TC] = source

//  def aa: InvariantHK[ApplyK] = new InvariantHK[ApplyK] {
//    def imapHK[A1[_[_]], A2[_[_]]](tc: ApplyK[A1])(ab: ≈>[A1, A2])(ba: ≈>[A2, A1]): ApplyK[A2] =
//      new ApplyK[A2] {
//        def productK[F[_], G[_]](af: A2[F], ag: A2[G]): A2[Tuple2K[F, G, ?]] = ???
//        def mapK[F[_], G[_]](af: A2[F])(fk: ~>[F, G]): A2[G] = ???
//      }
//  }

  implicit def invHKFunctorK: InvariantHK[FunctorK] = new InvariantHK[FunctorK] {
    def imapHK[A1[_[_]], A2[_[_]]](tc: FunctorK[A1])(ab: ≈>[A1, A2])(ba: ≈>[A2, A1]): FunctorK[A2] =
      new FunctorK[A2] {
        def mapK[F[_], G[_]](af: A2[F])(fg: ~>[F, G]): A2[G] =
          ab(tc.mapK(ba.apply(af))(fg))
      }
  }

  implicit def invHKContravariantK: InvariantHK[ContravariantK] = new InvariantHK[ContravariantK] {
    def imapHK[A1[_[_]], A2[_[_]]](tc: ContravariantK[A1])(ab: ≈>[A1, A2])(ba: ≈>[A2, A1]): ContravariantK[A2] =
      new ContravariantK[A2] {
        def contramapK[F[_], G[_]](af: A2[F])(gf: ~>[G, F]): A2[G] =
          ab(tc.contramapK(ba.apply(af))(gf))
      }
  }
}

