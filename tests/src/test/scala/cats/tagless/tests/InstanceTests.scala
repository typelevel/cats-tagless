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

import cats.data.*
import cats.laws.discipline.MiniInt
import cats.laws.discipline.arbitrary.*
import cats.laws.discipline.eq.*
import cats.tagless.laws.discipline.{ApplyKTests, ContravariantKTests, FunctorKTests}

import scala.util.Try

class InstanceTests extends CatsTaglessTestSuite {
  checkAll(
    "FunctorK[Nested[*[_], Try, Int]]",
    FunctorKTests[({ type W[g[_]] = Nested[g, Try, Int] })#W].functorK[Try, Option, List, Int]
  )
  checkAll(
    "ContravariantK[Cokleisli[*[_], MiniInt, Boolean]]",
    ContravariantKTests[({ type W[g[_]] = Cokleisli[g, MiniInt, Boolean] })#W].contravariantK[Try, Option, List, Int]
  )

  checkAll(
    "ApplyK[OneAnd[*[_], Int]]",
    ApplyKTests[({ type W[g[_]] = OneAnd[g, Int] })#W].applyK[Try, Option, List, Int]
  )
  checkAll("ApplyK[IdT[*[_], Int]]", ApplyKTests[({ type W[g[_]] = IdT[g, Int] })#W].applyK[Try, Option, List, Int])
  checkAll(
    "ApplyK[OptionT[*[_], Int]]",
    ApplyKTests[({ type W[g[_]] = OptionT[g, Int] })#W].applyK[Try, Option, List, Int]
  )

  checkAll(
    "ApplyK[EitherT[*[_], String, Int]]",
    ApplyKTests[({ type W[g[_]] = EitherT[g, String, Int] })#W].applyK[Try, Option, List, Int]
  )
  checkAll(
    "ApplyK[IorT[*[_], String, Int]]",
    ApplyKTests[({ type W[g[_]] = IorT[g, String, Int] })#W].applyK[Try, Option, List, Int]
  )
  checkAll(
    "ApplyK[WriterT[*[_], String, Int]]",
    ApplyKTests[({ type W[g[_]] = WriterT[g, String, Int] })#W].applyK[Try, Option, List, Int]
  )

  checkAll(
    "ApplyK[Func[*[_], MiniInt, Boolean]]",
    ApplyKTests[({ type W[g[_]] = Func[g, MiniInt, Boolean] })#W].applyK[Try, Option, List, Int]
  )
  checkAll(
    "ApplyK[Kleisli[*[_], MiniInt, Boolean]]",
    ApplyKTests[({ type W[g[_]] = Kleisli[g, MiniInt, Boolean] })#W].applyK[Try, Option, List, Int]
  )

  checkAll(
    "ApplyK[EitherK[Try, *[_], Int]]",
    ApplyKTests[({ type W[g[_]] = EitherK[Try, g, Int] })#W].applyK[Try, Option, List, Int]
  )
  checkAll(
    "ApplyK[Tuple2K[*[_], Option, Int]]",
    ApplyKTests[({ type W[g[_]] = Tuple2K[g, Option, Int] })#W].applyK[Try, Option, List, Int]
  )
  checkAll(
    "ApplyK[Tuple2K[Option, *[_], Int]]",
    ApplyKTests[({ type W[g[_]] = Tuple2K[Option, g, Int] })#W].applyK[Try, Option, List, Int]
  )
}
