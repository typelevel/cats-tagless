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

package cats.tagless.tests

import cats.data._
import cats.laws.discipline.MiniInt
import cats.laws.discipline.arbitrary._
import cats.laws.discipline.eq._
import cats.tagless.laws.discipline.{ApplyKTests, ContravariantKTests, FunctorKTests}

import scala.util.Try

class InstanceTests extends CatsTaglessTestSuite {
  checkAll("FunctorK[Nested[*[_], Try, Int]]", FunctorKTests[Nested[*[_], Try, Int]].functorK[Try, Option, List, Int])
  checkAll(
    "ContravariantK[Cokleisli[*[_], MiniInt, Boolean]]",
    ContravariantKTests[Cokleisli[*[_], MiniInt, Boolean]].contravariantK[Try, Option, List, Int]
  )

  checkAll("ApplyK[OneAnd[*[_], Int]]", ApplyKTests[OneAnd[*[_], Int]].applyK[Try, Option, List, Int])
  checkAll("ApplyK[IdT[*[_], Int]]", ApplyKTests[IdT[*[_], Int]].applyK[Try, Option, List, Int])
  checkAll("ApplyK[OptionT[*[_], Int]]", ApplyKTests[OptionT[*[_], Int]].applyK[Try, Option, List, Int])

  checkAll("ApplyK[EitherT[*[_], String, Int]]", ApplyKTests[EitherT[*[_], String, Int]].applyK[Try, Option, List, Int])
  checkAll("ApplyK[IorT[*[_], String, Int]]", ApplyKTests[IorT[*[_], String, Int]].applyK[Try, Option, List, Int])
  checkAll("ApplyK[WriterT[*[_], String, Int]]", ApplyKTests[WriterT[*[_], String, Int]].applyK[Try, Option, List, Int])

  checkAll(
    "ApplyK[Func[*[_], MiniInt, Boolean]]",
    ApplyKTests[Func[*[_], MiniInt, Boolean]].applyK[Try, Option, List, Int]
  )
  checkAll(
    "ApplyK[Kleisli[*[_], MiniInt, Boolean]]",
    ApplyKTests[Kleisli[*[_], MiniInt, Boolean]].applyK[Try, Option, List, Int]
  )

  checkAll("ApplyK[EitherK[Try, *[_], Int]]", ApplyKTests[EitherK[Try, *[_], Int]].applyK[Try, Option, List, Int])
  checkAll("ApplyK[Tuple2K[*[_], Option, Int]]", ApplyKTests[Tuple2K[*[_], Option, Int]].applyK[Try, Option, List, Int])
  checkAll("ApplyK[Tuple2K[Option, *[_], Int]]", ApplyKTests[Tuple2K[Option, *[_], Int]].applyK[Try, Option, List, Int])
}
