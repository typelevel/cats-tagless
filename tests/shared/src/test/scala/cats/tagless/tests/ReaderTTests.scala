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

import cats.data.ReaderT
import cats.tagless.Derive

import scala.util.{Failure, Success, Try}

class ReaderTTests extends CatsTaglessTestSuite {
  import ReaderTTests._
  type F[A] = ReaderT[Try, SpaceAlg[Try], A]

  val dependency: SpaceAlg[Try] = new SpaceAlg[Try] {
    def blackHole[A](anything: Try[A]) = anything.void
    def distance(x: Galaxy, y: Galaxy) = Success((x.name.sum + y.name.sum) / 2.0)
    def collision(x: Try[Galaxy], y: Try[Galaxy]) = for {
      x <- x.map(_.name)
      y <- y.map(_.name)
    } yield Galaxy(x.zip(y).map { case (x, y) => s"$x$y" }.mkString(""))
  }

  test("Dependency injection") {
    val eventHorizon = Failure(Error("past event horizon"))
    assertEquals(spaceAlg.blackHole(ReaderT(_ => eventHorizon)).run(dependency), eventHorizon)
    assertEquals(spaceAlg.distance(milkyWay, andromeda).run(dependency), Success(881.0))
    assertEquals(
      spaceAlg.collision(milkyWay.pure[F], andromeda.pure[F]).run(dependency),
      Success(Galaxy("MAinldkryo mWeadya"))
    )
  }
}

object ReaderTTests {
  final case class Galaxy(name: String)
  final case class Error(message: String) extends RuntimeException(message)

  val spaceAlg = Derive.readerT[SpaceAlg, Try]
  val milkyWay = Galaxy("Milky Way")
  val andromeda = Galaxy("Andromeda")

  trait SpaceAlg[F[_]] {
    def blackHole[A](anything: F[A]): F[Unit]
    def distance(x: Galaxy, y: Galaxy): F[Double]
    def collision(x: F[Galaxy], y: F[Galaxy]): F[Galaxy]
  }
}
