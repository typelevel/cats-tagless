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

import cats.Show
import cats.kernel.laws.discipline.SerializableTests
import cats.tagless.aop.Aspect
import cats.tagless.laws.discipline
import cats.tagless.{Derive, Trivial}

import scala.util.Try

class AspectTests extends CatsTaglessTestSuite {
  import AspectTests._

  checkAll("Aspect[SafeAlg]", discipline.AspectTests[SafeAlg, Show, Show].aspect[Try, Option, List, Int])
  checkAll("Aspect is Serializable", SerializableTests.serializable(Aspect.function[SafeAlg, Show]))

  test("Show aspect") {
    val algebra: ShowFAlgebra[List] = new ShowFAlgebra[List] {
      def showF[A: Show](a: A) = List(a.show)
      def showAll[A: Show](as: A*) = as.map(_.show).toList
      def showProduct[A: Show](a: A) = List((a, a.show))
      def logF[A: Show](message: => A) = Nil
    }

    def testWeave[A](weave: Aspect.Weave.Function[List, Show, A])(
        algebraName: String,
        domain: List[Map[String, String]],
        codomainName: String,
        codomainTarget: List[String]
    ): Unit = {
      assertEquals(weave.domain.map(_.map(a => a.name -> a.instance.show(a.target.value)).toMap), domain)
      assertEquals(weave.algebraName, algebraName)
      assertEquals(weave.codomain.name, codomainName)
      assertEquals(weave.codomain.target.map(weave.codomain.instance.show), codomainTarget)
    }

    val weaved = algebra.weaveFunction[Show]
    testWeave(weaved.showF(42))("ShowFAlgebra", List(Map("a" -> "42")), "showF", List("42"))
    testWeave(weaved.showAll("foo", "bar", "baz"))(
      "ShowFAlgebra",
      List(Map("as" -> "foo", "as" -> "bar", "as" -> "baz")),
      "showAll",
      List("foo", "bar", "baz")
    )

    testWeave(weaved.showProduct(3.14))("ShowFAlgebra", List(Map("a" -> "3.14")), "showProduct", List("(3.14,3.14)"))
    val it = (1 to 3).iterator
    val logF = weaved.logF(it.next().toString)
    testWeave(logF)("ShowFAlgebra", List(Map("message" -> "1")), "logF", Nil)
    testWeave(logF)("ShowFAlgebra", List(Map("message" -> "2")), "logF", Nil)
    testWeave(logF)("ShowFAlgebra", List(Map("message" -> "3")), "logF", Nil)
  }
}

object AspectTests extends TestInstances {
  implicit val showSafeAlg: Aspect.Function[SafeAlg, Show] = Derive.aspect

  trait ShowFAlgebra[F[_]] {
    def showF[A: Show](a: A): F[String]
    def showAll[A: Show](as: A*): F[String]
    def showProduct[A: Show](a: A): F[(A, String)]
    def logF[A: Show](message: => A): F[Unit]
  }

  object ShowFAlgebra {
    implicit val showAspect: Aspect.Function[ShowFAlgebra, Show] = Derive.aspect
    implicit val trivialAspect: Aspect.Function[ShowFAlgebra, Trivial] = Derive.aspect
  }
}
