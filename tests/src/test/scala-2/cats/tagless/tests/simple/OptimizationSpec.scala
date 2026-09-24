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
package simple

import cats.*
import cats.data.*
import cats.tagless.optimize.*
import cats.tagless.optimize.syntax.all.*

class OptimizationSpec extends cats.tagless.tests.CatsTaglessTestSuite {

  trait KVStore[F[_]] {
    def get(key: String): F[Option[String]]
    def put(key: String, value: String): F[Unit]
  }

  val mockInterpreter = new KVStore[Id] {
    def get(key: String) = Some(s"Value for $key")
    def put(key: String, value: String) = ()
  }

  def createOptimizer[F[_]: Monad]: Optimizer[KVStore, F] = new Optimizer[KVStore, F] {
    type M = Set[String]

    def monoidM = implicitly[Monoid[Set[String]]]
    def monadF = implicitly[Monad[F]]

    def extract = new KVStore[Lambda[A => Const[Set[String], A]]] {
      def get(key: String) = Const(Set(key))
      def put(key: String, value: String) = Const(Set.empty)
    }

    def rebuild(keys: Set[String], interp: KVStore[F]): F[KVStore[F]] = {
      keys.toList.traverse(key => interp.get(key).map(_.map(value => key -> value))).map { results =>
        val cache = results.flatten.toMap

        new KVStore[F] {
          def get(key: String) = cache.get(key) match {
            case Some(value) => Option(value).pure[F]
            case None => interp.get(key)
          }
          def put(key: String, value: String) = interp.put(key, value)
        }
      }
    }
  }

  test("Optimizer should eliminate duplicate operations") {
    def program[F[_]: Applicative](store: KVStore[F]): F[List[String]] = {
      (store.get("Cats"), store.get("Dogs"), store.get("Cats"), store.get("Birds"))
        .mapN((c, d, c2, b) => List(c, d, c2, b).flatten)
    }

    val programInstance = new Program[KVStore, Applicative, List[String]] {
      def apply[F[_]: Applicative](alg: KVStore[F]): F[List[String]] = program(alg)
    }

    implicit val optimizer = createOptimizer[Id]

    val unoptimized = program(mockInterpreter)
    val optimized = programInstance.optimize(mockInterpreter)

    // Both should produce the same result
    assertEquals(optimized, unoptimized)

    // The optimized version should have the same values but potentially different execution order
    assertEquals(optimized.size, 4)
    assert(optimized.contains("Value for Cats"))
    assert(optimized.contains("Value for Dogs"))
    assert(optimized.contains("Value for Birds"))
  }

  test("Optimizer should work with put-get elimination") {
    case class KVStoreInfo(queries: Set[String], cache: Map[String, String])

    def createPutGetEliminator[F[_]: Monad]: Optimizer[KVStore, F] = new Optimizer[KVStore, F] {
      type M = KVStoreInfo

      def monoidM = implicitly[Monoid[KVStoreInfo]]
      def monadF = implicitly[Monad[F]]

      def extract = new KVStore[Lambda[A => Const[KVStoreInfo, A]]] {
        def get(key: String) = Const(KVStoreInfo(Set(key), Map.empty))
        def put(key: String, value: String) = Const(KVStoreInfo(Set.empty, Map(key -> value)))
      }

      def rebuild(info: KVStoreInfo, interp: KVStore[F]): F[KVStore[F]] = {
        val uncachedQueries = info.queries.filterNot(info.cache.contains)

        uncachedQueries.toList.traverse(key => interp.get(key).map(_.map(value => key -> value))).map { results =>
          val fetched = results.flatten.toMap
          val allCache = info.cache ++ fetched

          new KVStore[F] {
            def get(key: String) = allCache.get(key) match {
              case Some(value) => Option(value).pure[F]
              case None => interp.get(key)
            }
            def put(key: String, value: String) = interp.put(key, value)
          }
        }
      }
    }

    def putGetProgram[F[_]: Applicative](store: KVStore[F]): F[List[String]] = {
      (store.put("Cat", "Cat!"), store.get("Cat"), store.get("Dog"))
        .mapN((_, cat, dog) => List(cat, dog).flatten)
    }

    val putGetProgramInstance = new Program[KVStore, Applicative, List[String]] {
      def apply[F[_]: Applicative](alg: KVStore[F]): F[List[String]] = putGetProgram(alg)
    }

    implicit val putGetOptimizer = createPutGetEliminator[Id]

    val unoptimized = putGetProgram(mockInterpreter)
    val optimized = putGetProgramInstance.optimize(mockInterpreter)

    // Both should produce the same result
    assertEquals(optimized, unoptimized)
  }

  test("MonadOptimizer should work with stateful optimizations") {
    def createMonadOptimizer[F[_]: Monad]: MonadOptimizer[KVStore, F] = new MonadOptimizer[KVStore, F] {
      type M = Map[String, String]

      def monoidM = implicitly[Monoid[Map[String, String]]]
      def monadF = implicitly[Monad[F]]
      def applyK = implicitly[ApplyK[KVStore]]

      def rebuild(interp: KVStore[F]): KVStore[Lambda[A => Kleisli[F, M, A]]] =
        new KVStore[Lambda[A => Kleisli[F, M, A]]] {
          def get(key: String): Kleisli[F, M, Option[String]] = Kleisli { cache =>
            cache.get(key) match {
              case Some(value) => Option(value).pure[F]
              case None => interp.get(key)
            }
          }

          def put(key: String, value: String): Kleisli[F, M, Unit] = Kleisli { _ =>
            interp.put(key, value)
          }
        }

      def extract: KVStore[Lambda[A => A => M]] = new KVStore[Lambda[A => A => M]] {
        def get(key: String): Option[String] => M = {
          case Some(value) => Map(key -> value)
          case None => Map.empty
        }

        def put(key: String, value: String): Unit => M = _ => Map(key -> value)
      }
    }

    def monadProgram[F[_]: Monad](store: KVStore[F]): F[List[String]] = for {
      _ <- store.put("test", "value")
      result <- store.get("test")
    } yield List(result).flatten

    val monadProgramInstance = new Program[KVStore, Monad, List[String]] {
      def apply[F[_]: Monad](alg: KVStore[F]): F[List[String]] = monadProgram(alg)
    }

    implicit val monadOptimizer = createMonadOptimizer[Id]

    val unoptimized = monadProgram(mockInterpreter)
    val optimized = monadProgramInstance.optimizeM(mockInterpreter)

    // Both should produce the same result
    assertEquals(optimized, unoptimized)
  }
}
