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

import cats.tagless.aop.Aspect
import cats.tagless.{Derive, Void}
import cats.~>
import io.circe._
import io.circe.syntax._

class AspectTestsJVM extends CatsTaglessTestSuite {
  import AspectTestsJVM._

  test("Json aspect") {
    val void = Derive.void[GeoAlgebra]
    val toRequest = λ[Aspect.Weave[Void, Encoder, Decoder, *] ~> HttpRequest] { weave =>
      import weave.codomain.instance
      val hasArgs = weave.domain.nonEmpty
      val method = if (hasArgs) "POST" else "GET"
      val url = s"https://foo.bar/${weave.codomain.name}"
      val body = hasArgs.guard[Option].map { _ =>
        weave.domain.foldLeft(JsonObject.empty) { (body, args) =>
          args.foldLeft(body) { (body, advice) =>
            body.add(advice.name, advice.instance(advice.target.value))
          }
        }
      }

      HttpRequest(method, url, body.filter(_.nonEmpty).map(Json.fromJsonObject))
    }

    val client = void.weave[Encoder, Decoder].mapK(toRequest)
    val location: Location = (42.56, 23.27)
    assertEquals(client.currentLocation, HttpRequest[Location]("GET", "https://foo.bar/currentLocation"))

    assertEquals(
      client.area(location, 1.0),
      HttpRequest[Double](
        "POST",
        "https://foo.bar/area",
        Some(Json.obj("center" -> location.asJson, "radius" -> 1.0.asJson))
      )
    )

    assertEquals(
      client.nearestCity(location),
      HttpRequest[String](
        "POST",
        "https://foo.bar/nearestCity",
        Some(Json.obj("to" -> location.asJson))
      )
    )
  }
}

object AspectTestsJVM {
  type Location = (Double, Double)

  trait GeoAlgebra[F[_]] {
    def currentLocation: F[Location]
    def area(center: Location, radius: Double): F[Double]
    def nearestCity(to: Location): F[String]
  }

  object GeoAlgebra {
    implicit val jsonAspect: Aspect[GeoAlgebra, Encoder, Decoder] = Derive.aspect
  }

  final case class HttpRequest[A](
      method: String,
      url: String,
      body: Option[Json] = None
  )(implicit val decoder: Decoder[A])
}
