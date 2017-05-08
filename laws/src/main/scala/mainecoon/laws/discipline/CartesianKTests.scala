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

package mainecoon
package laws
package discipline


import cats.{Eq, ~>}
import cats.data.Prod
import mainecoon.laws.discipline.CartesianKTests.IsomorphismsK
import mainecoon.laws.discipline.CartesianKTests.IsomorphismsK.Tuple3K
import org.scalacheck.Prop._
import org.scalacheck.{Arbitrary, Prop}
import org.typelevel.discipline.Laws

trait CartesianKTests[F[_[_]]] extends Laws {
  def laws: CartesianKLaws[F]

  def CartesianK[A[_], B[_], C[_]](implicit
                                               ArbCF: Arbitrary[F[A]],
                                               ArbCG: Arbitrary[F[B]],
                                               ArbCH: Arbitrary[F[C]],
                                               iso: IsomorphismsK[F],
                                               EqFGH: Eq[F[Tuple3K[A, B, C, ?]]]
                                              ): RuleSet = {
    new DefaultRuleSet(
      name = "CartesianK",
      parent = None,
      "cartesian associativity" -> forAll((af: F[A], ag: F[B], ah: F[C]) => iso.associativity(laws.cartesianAssociativity[A, B, C](af, ag, ah))))
  }
}


object CartesianKTests {
  def apply[F[_[_]]: CartesianK]: CartesianKTests[F] =
    new CartesianKTests[F] { def laws: CartesianKLaws[F] = CartesianKLaws[F] }

  import IsomorphismsK._
  trait IsomorphismsK[F[_[_]]] {
    def associativity[A[_], B[_], C[_]](fs: (F[ProdA_BC[A, B, C, ?]], F[ProdAB_C[A, B, C, ?]]))
                                       (implicit EqFGH: Eq[F[Tuple3K[A, B, C, ?]]]): Prop
  }

  object IsomorphismsK {
    type Tuple3K[A[_], B[_], C[_], T] = (A[T], B[T], C[T])
    type ProdA_BC[A[_], B[_], C[_], T]  =  Prod[A, Prod[B, C, ?], T] 
    type ProdAB_C[A[_], B[_], C[_], T]  = Prod[Prod[A, B, ?], C, T] 

    import cats.kernel.laws._
    implicit def invariantK[F[_[_]]](implicit F: InvariantK[F]): IsomorphismsK[F] =
      new IsomorphismsK[F] {
        def associativity[A[_], B[_], C[_]](fs: (F[ProdA_BC[A, B, C, ?]], F[ProdAB_C[A, B, C, ?]]))
                                           (implicit EqFGH: Eq[F[Tuple3K[A, B, C, ?]]]): Prop = {

          val fkA_BC_T3 = λ[ProdA_BC[A, B, C, ?] ~> Tuple3K[A, B, C, ?] ]{ case Prod(a, Prod(b, c)) => (a, b, c) }
          val fkAB_C_T3 = λ[ProdAB_C[A, B, C, ?] ~> Tuple3K[A, B, C, ?] ]{ case Prod(Prod(a, b), c) => (a, b, c) }
          val fkT3_AB_C = λ[Tuple3K[A, B, C, ?] ~> ProdAB_C[A, B, C, ?]]{ case (a, b, c) => Prod(Prod(a, b), c) }
          val fkT3_A_BC = λ[Tuple3K[A, B, C, ?] ~> ProdA_BC[A, B, C, ?]]{ case (a, b, c) => Prod(a, Prod(b, c)) }

          F.imapK[ProdA_BC[A, B, C, ?], Tuple3K[A, B, C, ?]](fs._1)(fkA_BC_T3)(fkT3_A_BC) ?==
            F.imapK[ProdAB_C[A, B, C, ?], Tuple3K[A, B, C, ?]](fs._2)(fkAB_C_T3)(fkT3_AB_C)
        }

      }
  }
}
