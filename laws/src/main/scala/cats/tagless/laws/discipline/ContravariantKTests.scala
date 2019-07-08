/*
 * Copyright 2019 cats-tagless maintainers
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

package cats.tagless.laws.discipline

import cats.laws.discipline._
import cats.tagless.ContravariantK
import cats.tagless.laws.ContravariantKLaws
import cats.{Eq, ~>}
import org.scalacheck.Arbitrary
import org.scalacheck.Prop._

trait ContravariantKTests[F[_[_]]] extends InvariantKTests[F] {
  def laws: ContravariantKLaws[F]

  def contravariantK[A[_], B[_], C[_], T: Arbitrary](
    implicit
    arbFa: Arbitrary[F[A]],
    arbFkAB: Arbitrary[A ~> B],
    arbFkBC: Arbitrary[B ~> C],
    arbFkBA: Arbitrary[B ~> A],
    arbFkCB: Arbitrary[C ~> B],
    eqFa: Eq[F[A]],
    eqFc: Eq[F[C]]
  ): RuleSet = new DefaultRuleSet(
    name = "contravariantK",
    parent = Some(invariantK[A, B, C]),
    "contravariant identity" -> forAll(laws.contravariantIdentity[A] _),
    "contravariant composition" -> forAll(laws.contravariantComposition[A, B, C] _)
  )
}

object ContravariantKTests {
  def apply[F[_[_]]: ContravariantK]: ContravariantKTests[F] =
    new ContravariantKTests[F] { val laws = ContravariantKLaws[F] }
}
