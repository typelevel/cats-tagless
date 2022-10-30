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

import cats.arrow.FunctionK
import cats.data.ReaderT
import cats.tagless.{Derive, InvariantK}
import cats.{FlatMap, ~>}

import scala.util.{Failure, Success, Try}

class ReaderTTests extends CatsTaglessTestSuite {
  import ReaderTTests.*

  type F[A] = ReaderT[Try, SpaceAlg[Try], A]
  type SyncIO[A] = () => Try[A] // poor man's SyncIO

  val dependency: SpaceAlg[Try] = new SpaceAlg[Try] {
    def blackHole[A](anything: Try[A]) = anything.void
    def distance(x: Galaxy, y: Galaxy) = Success((x.name.sum + y.name.sum) / 2.0)
    def collision(x: Try[Galaxy], y: Try[Galaxy]) = for {
      x <- x.map(_.name)
      y <- y.map(_.name)
    } yield Galaxy(x.zip(y).map { case (x, y) => s"$x$y" }.mkString(""))
  }

  test("Dependency injection") {
    assertEquals(spaceAlg.blackHole(ReaderT(_ => eventHorizon)).run(dependency), eventHorizon)
    assertEquals(spaceAlg.distance(milkyWay, andromeda).run(dependency), Success(881.0))
    assertEquals(
      spaceAlg.collision(milkyWay.pure[F], andromeda.pure[F]).run(dependency),
      Success(Galaxy("MAinldkryo mWeadya"))
    )
  }

  // The same approach could be used for Future ~> IO
  test("Try ~> SyncIO") {
    def provide[R](service: R) = λ[ReaderT[Try, R, *] ~> SyncIO](r => () => r(service))
    def require[R] = λ[SyncIO ~> ReaderT[Try, R, *]](io => ReaderT(_ => io()))

    var successful, failed = 0
    // Make the dependency side-effecting
    val sideEffecting = dependency.imapK(λ[Try ~> Try] {
      case success @ Success(_) => successful += 1; success
      case failure @ Failure(_) => failed += 1; failure
    })(FunctionK.id)

    val alg = spaceAlg.imapK(provide(sideEffecting))(require)
    val blackHole = alg.blackHole(() => eventHorizon)
    val distance = alg.distance(milkyWay, andromeda)
    val collision = alg.collision(() => Success(milkyWay), () => Success(andromeda))

    assertEquals(failed, 0)
    assertEquals(blackHole(), eventHorizon)
    assertEquals(failed, 1)
    assertEquals(successful, 0)
    assertEquals(distance(), Success(881.0))
    assertEquals(successful, 1)
    assertEquals(collision(), Success(Galaxy("MAinldkryo mWeadya")))
    assertEquals(successful, 2)
  }

  // The same approach could be used for accessing a service in a Ref
  test("Try ~> Try") {
    def provide[G[_]: FlatMap, A](service: G[A]) =
      λ[ReaderT[G, A, *] ~> G](r => service.flatMap(r.run))

    val alg = spaceAlg.imapK(provide(Success(dependency)))(ReaderT.liftK)
    assertEquals(alg.blackHole(eventHorizon), eventHorizon)
    assertEquals(alg.distance(milkyWay, andromeda), Success(881.0))
    assertEquals(alg.collision(Success(milkyWay), Success(andromeda)), Success(Galaxy("MAinldkryo mWeadya")))

    val failingAlg = spaceAlg.imapK(provide(eventHorizon))(ReaderT.liftK)
    assertEquals(failingAlg.blackHole(eventHorizon), eventHorizon)
    assertEquals(failingAlg.distance(milkyWay, andromeda), eventHorizon)
    assertEquals(failingAlg.collision(Success(milkyWay), Success(andromeda)), eventHorizon)
  }
}

object ReaderTTests {
  final case class Galaxy(name: String)
  final case class Error(message: String) extends RuntimeException(message)

  val spaceAlg = Derive.readerT[SpaceAlg, Try]
  implicit val invariantK: InvariantK[SpaceAlg] = Derive.invariantK

  val milkyWay = Galaxy("Milky Way")
  val andromeda = Galaxy("Andromeda")
  val eventHorizon = Failure(Error("past event horizon"))

  trait SpaceAlg[F[_]] {
    def blackHole[A](anything: F[A]): F[Unit]
    def distance(x: Galaxy, y: Galaxy): F[Double]
    def collision(x: F[Galaxy], y: F[Galaxy]): F[Galaxy]
  }
}
