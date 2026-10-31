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
import cats.data.{Cokleisli, Kleisli}
import cats.kernel.laws.discipline.SerializableTests
import cats.laws.discipline.MiniInt
import cats.laws.discipline.arbitrary.*
import cats.laws.discipline.eq.*
import cats.tagless.InvariantK
import cats.tagless.data.given
import cats.tagless.laws.discipline.InvariantKTests
import cats.tagless.tests.CatsTaglessTestSuite
import org.scalacheck.{Arbitrary, Gen}

import scala.util.Try

class InvariantKSuite extends CatsTaglessTestSuite:
  import InvariantKSuite.*

  checkAll("InvariantK[Codec]", InvariantKTests[Codec].invariantK[Try, Option, List])
  checkAll("InvariantK is Serializable", SerializableTests.serializable(InvariantK[Codec]))

object InvariantKSuite:
  import cats.syntax.all.*

  enum Codec[F[_]] derives InvariantK:
    case Decoder(id: Int, f: Kleisli[F, MiniInt, String])
    case Encoder(id: Int, f: Cokleisli[F, MiniInt, String])

  object Codec:
    given [F[_]](using
        Arbitrary[Kleisli[F, MiniInt, String]],
        Arbitrary[Cokleisli[F, MiniInt, String]]
    ): Arbitrary[Codec[F]] = Arbitrary:
      for
        id <- Arbitrary.arbitrary[Int]
        codec <- Gen.oneOf(
          Arbitrary.arbitrary[Kleisli[F, MiniInt, String]].map(Codec.Decoder(id, _)),
          Arbitrary.arbitrary[Cokleisli[F, MiniInt, String]].map(Codec.Encoder(id, _))
        )
      yield codec

    given [F[_]](using Eq[Kleisli[F, MiniInt, String]], Eq[Cokleisli[F, MiniInt, String]]): Eq[Codec[F]] =
      case (Codec.Decoder(ix, fx), Codec.Decoder(iy, fy)) => ix == iy && fx === fy
      case (Codec.Encoder(ix, fx), Codec.Encoder(iy, fy)) => ix == iy && fx === fy
      case (_, _) => false

end InvariantKSuite
