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
                                               EqFGH: Eq[F[Tuple3K[A, B, C]#T]]
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
    def associativity[A[_], B[_], C[_]](fs: (F[ProdA_BC[A, B, C]#T], F[ProdAB_C[A, B, C]#T]))
                                       (implicit EqFGH: Eq[F[Tuple3K[A, B, C]#T]]): Prop
  }

  object IsomorphismsK {
    trait Tuple3K[A[_], B[_], C[_]] { type T[D] = (A[D], B[D], C[D]) }
    trait ProdA_BC[A[_], B[_], C[_]] { type T[D] = Prod[A, Prod[B, C, ?], D] }
    trait ProdAB_C[A[_], B[_], C[_]] { type T[D] = Prod[Prod[A, B, ?], C, D] }

    import cats.kernel.laws._
    implicit def invariantK[F[_[_]]](implicit F: InvariantK[F]): IsomorphismsK[F] =
      new IsomorphismsK[F] {
        def associativity[A[_], B[_], C[_]](fs: (F[ProdA_BC[A, B, C]#T], F[ProdAB_C[A, B, C]#T]))
                                           (implicit EqFGH: Eq[F[Tuple3K[A, B, C]#T]]): Prop = {

          val fkA_BC_T3 = λ[ProdA_BC[A, B, C]#T ~> Tuple3K[A, B, C]#T ](p => (p.first, p.second.first, p.second.second))
          val fkAB_C_T3 = λ[ProdAB_C[A, B, C]#T ~> Tuple3K[A, B, C]#T ](p => (p.first.first, p.first.second, p.second))
          val fkT3_AB_C = λ[Tuple3K[A, B, C]#T ~> ProdAB_C[A, B, C]#T](t => Prod(Prod(t._1, t._2), t._3))
          val fkT3_A_BC = λ[Tuple3K[A, B, C]#T ~> ProdA_BC[A, B, C]#T](t => Prod(t._1, Prod(t._2, t._3)))

          F.imapK[ProdA_BC[A, B, C]#T, Tuple3K[A, B, C]#T](fs._1)(fkA_BC_T3)(fkT3_A_BC) ?==
            F.imapK[ProdAB_C[A, B, C]#T, Tuple3K[A, B, C]#T](fs._2)(fkAB_C_T3)(fkT3_AB_C)
        }

      }
  }
}
