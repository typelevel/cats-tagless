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
import cats.kernel.laws.discipline.SerializableTests
import cats.tagless.FunctorK
import cats.tagless.data.given
import cats.tagless.laws.discipline.FunctorKTests
import cats.tagless.tests.{CatsTaglessTestSuite, experimental}
import org.scalacheck.{Arbitrary, Gen}

import scala.util.Try

@experimental
class FunctorKSuite extends CatsTaglessTestSuite:
  import FunctorKSuite.*

  checkAll("FunctorK[Tree]", FunctorKTests[Tree].functorK[Try, Option, List, Int])
  checkAll("FunctorK is Serializable", SerializableTests.serializable(FunctorK[Tree]))

object FunctorKSuite:
  import cats.syntax.all.*

  enum Tree[F[_]] derives FunctorK:
    case Leaf(values: F[String])
    case Branch(size: Int, left: Tree[F], right: Tree[F])

  object Tree:
    given [F[_]](using Arbitrary[F[String]]): Arbitrary[Tree[F]] =
      Arbitrary(Gen.lzy(Gen.sized: n =>
        if n <= 1 then Arbitrary.arbitrary[F[String]].map(Tree.Leaf.apply)
        else
          for
            size <- Arbitrary.arbitrary[Int]
            left <- Gen.resize(n / 2, Arbitrary.arbitrary[Tree[F]])
            right <- Gen.resize(n / 2, Arbitrary.arbitrary[Tree[F]])
          yield Tree.Branch(size, left, right)))

    given [F[_]](using Eq[F[String]]): Eq[Tree[F]] with
      override def eqv(x: Tree[F], y: Tree[F]): Boolean = (x, y) match
        case (Tree.Leaf(vx), Tree.Leaf(vy)) => vx === vy
        case (Tree.Branch(sx, lx, rx), Tree.Branch(sy, ly, ry)) =>
          sx == sy && eqv(lx, ly) && eqv(rx, ry)
        case (_, _) => false

end FunctorKSuite
