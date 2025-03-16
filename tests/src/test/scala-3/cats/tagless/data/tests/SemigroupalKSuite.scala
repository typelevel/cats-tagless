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
import cats.data.{EitherT, OptionT}
import cats.kernel.laws.discipline.SerializableTests
import cats.laws.discipline.arbitrary.*
import cats.tagless.data.given
import cats.tagless.laws.discipline.SemigroupalKTests
import cats.tagless.tests.{CatsTaglessTestSuite, experimental}
import cats.tagless.{InvariantK, SemigroupalK}
import org.scalacheck.Arbitrary

import scala.util.Try

@experimental
class SemigroupalKSuite extends CatsTaglessTestSuite:
  import SemigroupalKSuite.*

  checkAll("SemigroupalK[Parsed]", SemigroupalKTests[Parsed].semigroupalK[Try, Option, List])
  checkAll("SemigroupalK is Serializable", SerializableTests.serializable(SemigroupalK[Parsed]))

object SemigroupalKSuite:
  import cats.syntax.all.*

  case class Parsed[F[_]](
      position: Int,
      labels: F[String],
      metadata: OptionT[F, String],
      errors: EitherT[F, String, Int]
  ) derives InvariantK,
        SemigroupalK

  object Parsed:
    given [F[_]](using
        Arbitrary[F[Int]],
        Arbitrary[F[String]],
        Arbitrary[F[Option[String]]],
        Arbitrary[F[Either[String, Int]]]
    ): Arbitrary[Parsed[F]] = Arbitrary:
      for
        position <- Arbitrary.arbitrary[Int]
        labels <- Arbitrary.arbitrary[F[String]]
        metadata <- Arbitrary.arbitrary[OptionT[F, String]]
        errors <- Arbitrary.arbitrary[EitherT[F, String, Int]]
      yield Parsed(position, labels, metadata, errors)

  given [F[_]](using
      Eq[F[Int]],
      Eq[F[String]],
      Eq[F[Option[String]]],
      Eq[F[Either[String, Int]]]
  ): Eq[Parsed[F]] = (x, y) =>
    x.position == y.position && x.labels === y.labels && x.metadata === y.metadata && x.errors === y.errors

end SemigroupalKSuite
