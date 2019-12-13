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

import cats.Eval
import cats.data._
import cats.laws.discipline.arbitrary._
import cats.laws.discipline.eq._
import cats.tagless.IdK
import cats.tagless.laws.discipline.SemigroupalKTests.IsomorphismsK
import cats.tagless.laws.discipline.{ApplyKTests, ContravariantKTests, FunctorKTests}

import scala.util.Try

class CatsDataInstancesTests extends CatsTaglessTestSuite {
  implicit def idKisomorphismK[A]: IsomorphismsK[IdK[A]#λ] = IsomorphismsK.invariantK[IdK[A]#λ]

  checkAll("FunctorK[Nested[?[_], Eval, Int]]", FunctorKTests[Nested[?[_], Eval, Int]].functorK[Try, Option, List, Int])
  checkAll("FunctorK[OneAnd[?[_], Boolean]]", FunctorKTests[OneAnd[?[_], Boolean]].functorK[Try, Option, List, Int])
  checkAll("FunctorK[Tuple2K[Eval, ?[_], Int]]", FunctorKTests[Tuple2K[Eval, ?[_], Int]].functorK[Try, Option, List, Int])
  checkAll("ContravariantK[Cokleisli[?[_], String, Int]]", ContravariantKTests[Cokleisli[?[_], String, Int]].contravariantK[Try, Option, List, Int])
  checkAll("ApplyK[IdK[Int]#λ]", ApplyKTests[IdK[Int]#λ].applyK[Try, Option, List, Int])
  checkAll("ApplyK[EitherK[Eval, ?[_], Int]]", ApplyKTests[EitherK[Eval, ?[_], Int]].applyK[Try, Option, List, Int])
  checkAll("ApplyK[EitherT[?[_], String, Int]]", ApplyKTests[EitherT[?[_], String, Int]].applyK[Try, Option, List, Int])
  checkAll("ApplyK[Func[?[_], String, Int]]", ApplyKTests[Func[?[_], String, Int]].applyK[Try, Option, List, Int])
  checkAll("ApplyK[IdT[?[_], Int]]", ApplyKTests[IdT[?[_], Int]].applyK[Try, Option, List, Int])
  checkAll("ApplyK[IorT[?[_], String, Int]]", ApplyKTests[IorT[?[_], String, Int]].applyK[Try, Option, List, Int])
  checkAll("ApplyK[Kleisli[?[_], String, Int]]", ApplyKTests[Kleisli[?[_], String, Int]].applyK[Try, Option, List, Int])
  checkAll("ApplyK[OneAnd[?[_], Int]]", ApplyKTests[OneAnd[?[_], Int]].applyK[Try, Option, List, Int])
  checkAll("ApplyK[OptionT[?[_], Int]]", ApplyKTests[OptionT[?[_], Int]].applyK[Try, Option, List, Int])
  checkAll("ApplyK[Tuple2K[Set, ?[_], Int]]", ApplyKTests[Tuple2K[Set, ?[_], Int]].applyK[Try, Option, List, Int])
  checkAll("ApplyK[WriterT[?[_], String, Int]]", ApplyKTests[WriterT[?[_], String, Int]].applyK[Try, Option, List, Int])
}
