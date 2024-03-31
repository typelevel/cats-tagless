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

import cats.laws.discipline.MiniInt
import cats.laws.discipline.arbitrary.*
import cats.laws.discipline.eq.*
import cats.tagless.Curried
import cats.tagless.laws.discipline.{ApplyKTests, ContravariantKTests, FunctorKTests}

import scala.util.Try

class InstanceTests extends CatsTaglessTestSuite {
  checkAll(
    "FunctorK[Nested[*[_], Try, Int]]",
    FunctorKTests[Curried.NestedOuter[Try, Int]#λ].functorK[Try, Option, List, Int]
  )
  checkAll(
    "ContravariantK[Cokleisli[*[_], MiniInt, Boolean]]",
    ContravariantKTests[Curried.Cokleisli[MiniInt, Boolean]#λ].contravariantK[Try, Option, List, Int]
  )

  checkAll("ApplyK[OneAnd[*[_], Int]]", ApplyKTests[Curried.OneAnd[Int]#λ].applyK[Try, Option, List, Int])
  checkAll("ApplyK[IdT[*[_], Int]]", ApplyKTests[Curried.IdT[Int]#λ].applyK[Try, Option, List, Int])
  checkAll("ApplyK[OptionT[*[_], Int]]", ApplyKTests[Curried.OptionT[Int]#λ].applyK[Try, Option, List, Int])

  checkAll(
    "ApplyK[EitherT[*[_], String, Int]]",
    ApplyKTests[Curried.EitherT[String, Int]#λ].applyK[Try, Option, List, Int]
  )
  checkAll("ApplyK[IorT[*[_], String, Int]]", ApplyKTests[Curried.IorT[String, Int]#λ].applyK[Try, Option, List, Int])
  checkAll(
    "ApplyK[WriterT[*[_], String, Int]]",
    ApplyKTests[Curried.WriterT[String, Int]#λ].applyK[Try, Option, List, Int]
  )

  checkAll(
    "ApplyK[Func[*[_], MiniInt, Boolean]]",
    ApplyKTests[Curried.Func[MiniInt, Boolean]#λ].applyK[Try, Option, List, Int]
  )
  checkAll(
    "ApplyK[Kleisli[*[_], MiniInt, Boolean]]",
    ApplyKTests[Curried.Kleisli[MiniInt, Boolean]#λ].applyK[Try, Option, List, Int]
  )

  checkAll(
    "ApplyK[EitherK[Try, *[_], Int]]",
    ApplyKTests[Curried.EitherKRight[Try, Int]#λ].applyK[Try, Option, List, Int]
  )
  checkAll(
    "ApplyK[Tuple2K[*[_], Option, Int]]",
    ApplyKTests[Curried.Tuple2KFirst[Option, Int]#λ].applyK[Try, Option, List, Int]
  )
  checkAll(
    "ApplyK[Tuple2K[Option, *[_], Int]]",
    ApplyKTests[Curried.Tuple2KSecond[Option, Int]#λ].applyK[Try, Option, List, Int]
  )
}
