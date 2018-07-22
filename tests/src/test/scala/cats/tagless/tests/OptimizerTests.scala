/*
 * Copyright 2017 Kailuo Wang
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

import cats._
import cats.data._
import cats.effect._
import cats.tagless.ApplyK
import cats.tagless.optimize._

import scala.concurrent.ExecutionContext.Implicits.global

class OptimizerTests extends CatsTaglessTestSuite {
  def kVStoreIOOptimizer: Optimizer[KVStore, IO] = new Optimizer[KVStore, IO] {
    type M = Set[String]

    def monoidM = implicitly

    def monadF = implicitly

    def extract = new KVStore[Const[Set[String], ?]] {
      def get(key: String) = Const(Set(key))
      def put(key: String, a: String): Const[Set[String], Unit] = Const(Set.empty)
    }

    def rebuild(gs: Set[String], interp: KVStore[IO]): IO[KVStore[IO]] =
      gs.toList
        .traverse(key => interp.get(key).map(_.map(s => (key, s))))
        .map(_.collect { case Some(v) => v }.toMap)
        .map { m =>
          new KVStore[IO] {
            override def get(key: String) = m.get(key) match {
              case Some(a) => Option(a).pure[IO]
              case None => interp.get(key)
            }

            def put(key: String, a: String): IO[Unit] = interp.put(key, a)
          }
        }

  }

  def rebuildPutGet(info: KVStoreInfo, interp: KVStore[IO]): IO[KVStore[IO]] =
    info.queries.toList.filterNot(info.cache.contains)
      .parTraverse(key => interp.get(key).map(_.map(s => (key, s))))
      .map { list =>
        val table: Map[String, String] = list.flatten.toMap

        new KVStore[IO] {
          def get(key: String) = table.get(key).orElse(info.cache.get(key)) match {
            case Some(a) => Option(a).pure[IO]
            case None => interp.get(key)
          }

          def put(key: String, a: String): IO[Unit] = interp.put(key, a)
        }
      }

  def kVStorePutGetEliminizer: Optimizer[KVStore, IO] = new Optimizer[KVStore, IO] {
    type M = KVStoreInfo

    def monoidM = implicitly

    def monadF = implicitly

    def extract = new KVStore[Const[KVStoreInfo, ?]] {
      def get(key: String): Const[KVStoreInfo, Option[String]] =
        Const(KVStoreInfo(Set(key), Map.empty))

      def put(key: String, a: String): Const[KVStoreInfo, Unit] =
        Const(KVStoreInfo(Set.empty, Map(key -> a)))
    }

    def rebuild(info: KVStoreInfo, interp: KVStore[IO]): IO[KVStore[IO]] =
      info.queries.toList.filterNot(info.cache.contains)
        .parTraverse(key => interp.get(key).map(_.map(s => (key, s))))
        .map { list =>
          val table: Map[String, String] = list.flatten.toMap

          new KVStore[IO] {
            def get(key: String) = table.get(key).orElse(info.cache.get(key)) match {
              case Some(a) => Option(a).pure[IO]
              case None => interp.get(key)
            }

            def put(key: String, a: String): IO[Unit] = interp.put(key, a)
          }
        }
  }


  def kVStorePutGetMonadEliminizer[F[_]: Sync]: MonadOptimizer[KVStore, F] = new MonadOptimizer[KVStore, F] {
    type M = Map[String, String]

    def monoidM = implicitly

    def monadF = implicitly

    def applyK: ApplyK[KVStore] = implicitly

    type Cache = Map[String, String]
    type CachedAction[A] = StateT[F, Cache, A]

    def transform(interp: KVStore[F]): KVStore[CachedAction] = new KVStore[CachedAction] {
      def put(key: String, v: String): CachedAction[Unit] =
        StateT.liftF[F, Cache, Unit](interp.put(key, v)) *> StateT.modify(_.updated(key, v))

      def get(key: String): CachedAction[Option[String]] = for {
        cache <- StateT.get[F, Cache]
        result <- cache.get(key) match {
          case s @ Some(_) => s.pure[CachedAction]
          case None => StateT.liftF[F, Cache, Option[String]](interp.get(key))
                         .flatTap(updateCache(key))
        }
      } yield result

      def updateCache(key: String)(ov: Option[String]): CachedAction[Unit] = ov match {
        case Some(v) => StateT.modify(_.updated(key, v))
        case None => ().pure[CachedAction]
      }
    }

    def rebuild(interp: KVStore[F]): KVStore[Kleisli[F, M, ?]] = new KVStore[Kleisli[F, M, ?]] {
      def get(key: String): Kleisli[F, M, Option[String]] = Kleisli(m => m.get(key) match {
        case o @ Some(_) => Applicative[F].pure(o)
        case None => interp.get(key)
      })

      def put(key: String, a: String): Kleisli[F, M, Unit] = Kleisli(m => interp.put(key, a))
    }

    def extract: KVStore[? => M] = new KVStore[? => M] {
      def get(key: String): Option[String] => M = {
        case Some(s) => Map(key -> s)
        case None => Map.empty
      }

      def put(key: String, a: String): Unit => M =
        _ => Map(key -> a)
    }

  }

  test("MonadOptimizer should optimize duplicates and put-get-elimination") {
    implicit val optimize: MonadOptimizer[KVStore, IO] = kVStorePutGetMonadEliminizer[IO]

    val interp = Interpreters.CrazyInterpreter.create.unsafeRunSync()

    val result = optimize.optimizeM(Programs.monadProgram).apply(interp).unsafeRunSync()


    interp.searches.size shouldBe 2
    interp.inserts.size shouldBe 3

    val control = Programs.monadProgram(interp).unsafeRunSync()

    result shouldBe control

  }

  test("Optimizer should optimize duplicates and put-get-elimination") {

    implicit val optimize: Optimizer[KVStore, IO] = kVStorePutGetEliminizer

    val interp = Interpreters.CrazyInterpreter.create.unsafeRunSync()

    val result = optimize
      .optimize(Programs.putGetProgram)(interp)
      .unsafeRunSync()

    interp.searches.size shouldBe 2
    interp.inserts.size shouldBe 2

    val control = Programs.putGetProgram(interp).unsafeRunSync()

    result shouldBe control


  }

  test("SemigroupalOptimizer duplicates should be removed") {

    implicit val optimizer: SemigroupalOptimizer[KVStore, IO] = kVStoreIOOptimizer

    val interp = Interpreters.CrazyInterpreter.create.unsafeRunSync()

    val result = optimizer.nonEmptyOptimize(Programs.applicativeProgram)(interp).unsafeRunSync()

    interp.inserts.size shouldBe 1
    interp.searches.size shouldBe 2

    val control = Programs.applicativeProgram(interp).unsafeRunSync()

    result shouldBe control
  }


  test("Optimizer duplicates should be removed") {

    implicit val optimizer: Optimizer[KVStore, IO] = kVStoreIOOptimizer

    def program[F[_]: Applicative](F: KVStore[F]): F[List[String]] =
      List(F.get("Cats"), F.get("Dogs"), F.get("Cats"), F.get("Birds"))
        .sequence
        .map(_.collect { case Some(s) => s })


    val p = new Program[KVStore, Applicative, List[String]] {
      def apply[F[_]: Applicative](alg: KVStore[F]): F[List[String]] = program(alg)
    }

    val interp = Interpreters.CrazyInterpreter.create.unsafeRunSync()

    val result = optimizer.optimize(p)(interp).unsafeRunSync()

    interp.searches.size shouldBe 3

    val control = program(interp).unsafeRunSync()

    result shouldBe control
  }
}
