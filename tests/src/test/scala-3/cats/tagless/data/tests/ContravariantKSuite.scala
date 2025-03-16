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

package cats.tagless.data.tests

import cats.Eq
import cats.arrow.FunctionK
import cats.data.Cokleisli
import cats.kernel.laws.discipline.SerializableTests
import cats.laws.discipline.ExhaustiveCheck
import cats.laws.discipline.arbitrary.*
import cats.laws.discipline.eq.*
import cats.tagless.ContravariantK
import cats.tagless.data.{DerivedContravariantK, given}
import cats.tagless.laws.discipline.ContravariantKTests
import cats.tagless.tests.{CatsTaglessTestSuite, experimental}
import org.scalacheck.{Arbitrary, Cogen, Gen}

import scala.util.Try

@experimental
class ContravariantKSuite extends CatsTaglessTestSuite:
  import ContravariantKSuite.*

  checkAll("ContravariantK[Fun]", ContravariantKTests[Fun].contravariantK[Try, Option, List, Int])
  checkAll("ContravariantK is Serializable", SerializableTests.serializable(ContravariantK[Fun]))

object ContravariantKSuite:
  import cats.syntax.all.*

  enum Fun[F[_]] derives ContravariantK:
    case Choice(name: String, choice: FunctionK[F, Option])
    case Switch(name: String, switch: Cokleisli[F, Boolean, String])

  object Fun:
    given [F[_]](using
        Arbitrary[FunctionK[F, Option]],
        Arbitrary[Cokleisli[F, Boolean, String]]
    ): Arbitrary[Fun[F]] = Arbitrary:
      for
        name <- Arbitrary.arbitrary[String]
        fun <- Gen.oneOf(
          Arbitrary.arbitrary[FunctionK[F, Option]].map(Fun.Choice(name, _)),
          Arbitrary.arbitrary[Cokleisli[F, Boolean, String]].map(Fun.Switch(name, _))
        )
      yield fun

    given [F[_]](using Eq[Cokleisli[F, Boolean, String]], ExhaustiveCheck[F[Boolean]]): Eq[Fun[F]] =
      case (Fun.Choice(nx, fx), Fun.Choice(ny, fy)) =>
        nx == ny && ExhaustiveCheck[F[Boolean]].allValues.forall(x => fx(x) == fy(x))
      case (Fun.Switch(nx, fx), Fun.Switch(ny, fy)) =>
        nx == ny && fx === fy
      case (_, _) => false

end ContravariantKSuite
