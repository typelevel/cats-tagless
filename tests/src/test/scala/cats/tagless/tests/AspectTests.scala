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

import cats.{Show, ~>}
import cats.implicits._
import cats.tagless.aop.Aspect
import cats.tagless.{Derive, Trivial, Void}
import io.circe._
import io.circe.syntax._
import org.scalatest.Assertion

class AspectTests extends CatsTaglessTestSuite {
  import AspectTests._

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
    ): Assertion = {
      weave.domain.map(_.map(a => a.methodName -> a.instance.show(a.target.value)).toMap) shouldBe domain
      weave.codomain.algebraName shouldBe algebraName
      weave.codomain.methodName shouldBe codomainName
      weave.codomain.target.map(weave.codomain.instance.show) shouldBe codomainTarget
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

  test("Json aspect") {
    val void = Derive.void[GeoAlgebra]
    val toRequest = λ[Aspect.Weave[Void, Encoder, Decoder, *] ~> HttpRequest] { weave =>
      import weave.codomain.instance
      val hasArgs = weave.domain.nonEmpty
      val method = if (hasArgs) "POST" else "GET"
      val url = s"https://foo.bar/${weave.codomain.methodName}"
      val body = hasArgs.guard[Option].map { _ =>
        weave.domain.foldLeft(JsonObject.empty) { (body, args) =>
          args.foldLeft(body) { (body, advice) =>
            body.add(advice.methodName, advice.instance(advice.target.value))
          }
        }
      }

      HttpRequest(method, url, body.filter(_.nonEmpty).map(Json.fromJsonObject))
    }

    val client = void.weave[Encoder, Decoder].mapK(toRequest)
    val location: Location = (42.56, 23.27)
    client.currentLocation shouldBe HttpRequest[Json]("GET", "https://foo.bar/currentLocation")

    client.area(location, 1.0) shouldBe HttpRequest[Json](
      "POST",
      "https://foo.bar/area",
      Some(Json.obj("center" -> location.asJson, "radius" -> 1.0.asJson))
    )

    client.nearestCity(location) shouldBe HttpRequest[Json](
      "POST",
      "https://foo.bar/nearestCity",
      Some(Json.obj("to" -> location.asJson))
    )
  }
}

object AspectTests {
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

  type Location = (Double, Double)

  trait GeoAlgebra[F[_]] {
    def currentLocation: F[Location]
    def area(center: Location, radius: Double): F[Double]
    def nearestCity(to: Location): F[String]
  }

  object GeoAlgebra {
    implicit val jsonAspect: Aspect[GeoAlgebra, Encoder, Decoder] = Derive.aspect
  }

  final case class HttpRequest[A: Decoder](method: String, url: String, body: Option[Json] = None)
}
