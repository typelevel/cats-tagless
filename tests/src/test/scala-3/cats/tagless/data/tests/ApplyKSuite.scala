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
import cats.data.{EitherK, Tuple2K}
import cats.kernel.laws.discipline.SerializableTests
import cats.laws.discipline.arbitrary.*
import cats.tagless.ApplyK
import cats.tagless.data.given
import cats.tagless.laws.discipline.ApplyKTests
import cats.tagless.tests.{CatsTaglessTestSuite, experimental}
import org.scalacheck.Arbitrary

import scala.util.Try

@experimental
class ApplyKSuite extends CatsTaglessTestSuite:
  import ApplyKSuite.*

  checkAll("ApplyK[Spec]", ApplyKTests[Spec].applyK[Try, Option, List, Int])
  checkAll("ApplyK is Serializable", SerializableTests.serializable(ApplyK[Spec]))

object ApplyKSuite:
  import cats.syntax.all.*

  case class Spec[F[_]](
      replicas: Int,
      labels: F[String],
      containers: Tuple2K[F, Option, Long],
      service: EitherK[List, F, String]
  ) derives ApplyK

  object Spec:
    given [F[_]](using Arbitrary[F[Long]], Arbitrary[F[String]]): Arbitrary[Spec[F]] = Arbitrary:
      for
        replicas <- Arbitrary.arbitrary[Int]
        labels <- Arbitrary.arbitrary[F[String]]
        containers <- Arbitrary.arbitrary[Tuple2K[F, Option, Long]]
        service <- Arbitrary.arbitrary[EitherK[List, F, String]]
      yield Spec(replicas, labels, containers, service)

  given [F[_]](using Eq[F[Long]], Eq[F[String]]): Eq[Spec[F]] = (x, y) =>
    x.replicas == y.replicas && x.labels === y.labels && x.containers === y.containers && x.service === y.service

end ApplyKSuite
