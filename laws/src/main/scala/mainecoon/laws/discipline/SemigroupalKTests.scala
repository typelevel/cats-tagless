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
import cats.data.Tuple2K
import mainecoon.laws.discipline.SemigroupalKTests.IsomorphismsK
import org.scalacheck.Prop._
import org.scalacheck.{Arbitrary, Prop}
import org.typelevel.discipline.Laws

trait SemigroupalKTests[F[_[_]]] extends Laws {
  def laws: SemigroupalKLaws[F]

  def semigroupalK[A[_], B[_], C[_]](implicit
                                               ArbCF: Arbitrary[F[A]],
                                               ArbCG: Arbitrary[F[B]],
                                               ArbCH: Arbitrary[F[C]],
                                               iso: IsomorphismsK[F],
                                               EqFGH: Eq[F[Tuple3K[A, B, C]#λ]]
                                              ): RuleSet = {
    new DefaultRuleSet(
      name = "SemigroupalK",
      parent = None,
      "semigroupal associativity" -> forAll((af: F[A], ag: F[B], ah: F[C]) => iso.associativity(laws.semigroupalAssociativity[A, B, C](af, ag, ah))))
  }
}


object SemigroupalKTests {
  def apply[F[_[_]]: SemigroupalK]: SemigroupalKTests[F] =
    new SemigroupalKTests[F] { def laws: SemigroupalKLaws[F] = SemigroupalKLaws[F] }

  import IsomorphismsK._

  trait IsomorphismsK[F[_[_]]] {
    def associativity[A[_], B[_], C[_]](fs: (F[ProdA_BC[A, B, C]#λ], F[ProdAB_C[A, B, C]#λ]))
                                       (implicit EqFGH: Eq[F[Tuple3K[A, B, C]#λ]]): Prop
  }

  object IsomorphismsK {
    
    type ProdA_BC[A[_], B[_], C[_]]  = { type λ[T] = Tuple2K[A, Tuple2K[B, C, ?], T] }
    type ProdAB_C[A[_], B[_], C[_]]  = { type λ[T] = Tuple2K[Tuple2K[A, B, ?], C, T] }

    implicit def invariantK[F[_[_]]](implicit F: InvariantK[F]): IsomorphismsK[F] =
      new IsomorphismsK[F] {
        def associativity[A[_], B[_], C[_]](fs: (F[ProdA_BC[A, B, C]#λ], F[ProdAB_C[A, B, C]#λ]))
                                           (implicit EqFGH: Eq[F[Tuple3K[A, B, C]#λ]]): Prop = {

          val fkA_BC_T3 = λ[ProdA_BC[A, B, C]#λ ~> Tuple3K[A, B, C]#λ ]{ case Tuple2K(a, Tuple2K(b, c)) => (a, b, c) }
          val fkAB_C_T3 = λ[ProdAB_C[A, B, C]#λ ~> Tuple3K[A, B, C]#λ ]{ case Tuple2K(Tuple2K(a, b), c) => (a, b, c) }
          val fkT3_AB_C = λ[Tuple3K[A, B, C]#λ ~> ProdAB_C[A, B, C]#λ]{ case (a, b, c) => Tuple2K(Tuple2K(a, b), c) }
          val fkT3_A_BC = λ[Tuple3K[A, B, C]#λ ~> ProdA_BC[A, B, C]#λ]{ case (a, b, c) => Tuple2K(a, Tuple2K(b, c)) }

          EqFGH.eqv(
            F.imapK[ProdA_BC[A, B, C]#λ, Tuple3K[A, B, C]#λ](fs._1)(fkA_BC_T3)(fkT3_A_BC),
            F.imapK[ProdAB_C[A, B, C]#λ, Tuple3K[A, B, C]#λ](fs._2)(fkAB_C_T3)(fkT3_AB_C)
          )
        }

      }
  }
}
