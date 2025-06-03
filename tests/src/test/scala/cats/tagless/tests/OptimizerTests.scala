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

import cats.*
import cats.data.*
import cats.tagless.ApplyK
import cats.tagless.optimize.*
import cats.tagless.tests.Interpreters.StateInfo

class OptimizerTests extends CatsTaglessTestSuite {
  type StateO[A] = State[StateInfo, A]

  def kVStoreOptimizer[F[_]: Monad]: Optimizer[KVStore, F] = new Optimizer[KVStore, F] {
    type M = Set[String]

    def monoidM = implicitly

    def monadF = implicitly

    def extract = new KVStore[Const[Set[String], *]] {
      def get(key: String) = Const(Set(key))
      def put(key: String, a: String): Const[Set[String], Unit] = Const(Set.empty)
    }

    def rebuild(gs: Set[String], interp: KVStore[F]): F[KVStore[F]] =
      gs.toList
        .traverse(key => interp.get(key).map(_.map(s => (key, s))))
        .map(_.collect { case Some(v) => v }.toMap)
        .map { m =>
          new KVStore[F] {
            override def get(key: String) = m.get(key) match {
              case Some(a) => Option(a).pure[F]
              case None => interp.get(key)
            }

            def put(key: String, a: String): F[Unit] = interp.put(key, a)
          }
        }

  }

  def rebuildPutGet(info: KVStoreInfo, interp: KVStore[Eval]): Eval[KVStore[Eval]] =
    info.queries.toList
      .filterNot(info.cache.contains)
      .traverse(key => interp.get(key).map(_.map(s => (key, s))))
      .map { list =>
        val table: Map[String, String] = list.flatten.toMap

        new KVStore[Eval] {
          def get(key: String) = table.get(key).orElse(info.cache.get(key)) match {
            case Some(a) => Option(a).pure[Eval]
            case None => interp.get(key)
          }

          def put(key: String, a: String): Eval[Unit] =
            interp.put(key, a)
        }
      }

  def kVStorePutGetEliminizer[F[_]: Monad]: Optimizer[KVStore, F] = new Optimizer[KVStore, F] {
    type M = KVStoreInfo

    def monoidM = implicitly

    def monadF = implicitly

    def extract = new KVStore[Const[KVStoreInfo, *]] {
      def get(key: String): Const[KVStoreInfo, Option[String]] =
        Const(KVStoreInfo(Set(key), Map.empty))

      def put(key: String, a: String): Const[KVStoreInfo, Unit] =
        Const(KVStoreInfo(Set.empty, Map(key -> a)))
    }

    def rebuild(info: KVStoreInfo, interp: KVStore[F]): F[KVStore[F]] =
      info.queries.toList
        .filterNot(info.cache.contains)
        .traverse(key => interp.get(key).map(_.map(s => (key, s))))
        .map { list =>
          val table: Map[String, String] = list.flatten.toMap

          new KVStore[F] {
            def get(key: String) = table.get(key).orElse(info.cache.get(key)) match {
              case Some(a) => Option(a).pure[F]
              case None => interp.get(key)
            }

            def put(key: String, a: String): F[Unit] = interp.put(key, a)
          }
        }
  }

  def kVStorePutGetMonadEliminizer[F[_]: Monad]: MonadOptimizer[KVStore, F] = new MonadOptimizer[KVStore, F] {
    type M = Map[String, String]

    def monoidM = implicitly
    def monadF = implicitly
    def applyK: ApplyK[KVStore] = implicitly

    def rebuild(interp: KVStore[F]): KVStore[Kleisli[F, M, *]] = new KVStore[Kleisli[F, M, *]] {
      def get(key: String): Kleisli[F, M, Option[String]] = Kleisli(m =>
        m.get(key) match {
          case o @ Some(_) => Applicative[F].pure(o)
          case None => interp.get(key)
        }
      )

      def put(key: String, a: String): Kleisli[F, M, Unit] = Kleisli(_ => interp.put(key, a))
    }

    def extract: KVStore[* => M] = new KVStore[* => M] {
      def get(key: String): Option[String] => M = {
        case Some(s) => Map(key -> s)
        case None => Map.empty
      }

      def put(key: String, a: String): Unit => M =
        _ => Map(key -> a)
    }

  }

  test("MonadOptimizer should optimize duplicates and put-get-elimination") {
    implicit val optimize: MonadOptimizer[KVStore, StateO] = kVStorePutGetMonadEliminizer[StateO]

    val interp = Interpreters.KVStoreInterpreter

    val (info, result) =
      optimize.optimizeM(Programs.monadProgram).apply(interp).run(StateInfo.empty).value

    assertEquals(info.searches.size, 2)
    assertEquals(info.inserts.size, 3)
    val control = Programs.monadProgram(interp).runA(StateInfo.empty).value
    assertEquals(result, control)
  }

  test("Optimizer should optimize duplicates and put-get-elimination") {

    implicit val optimize: Optimizer[KVStore, StateO] = kVStorePutGetEliminizer

    val interp = Interpreters.KVStoreInterpreter

    val (info, result) = optimize
      .optimize(Programs.putGetProgram)(interp)
      .run(StateInfo(Map.empty, Map.empty))
      .value

    assertEquals(info.searches.size, 2)
    assertEquals(info.inserts.size, 2)
    val control = Programs.putGetProgram(interp).runA(StateInfo(Map.empty, Map.empty)).value
    assertEquals(result, control)
  }

  test("SemigroupalOptimizer duplicates should be removed") {

    implicit val optimizer: SemigroupalOptimizer[KVStore, StateO] = kVStoreOptimizer

    val interp = Interpreters.KVStoreInterpreter

    val (info, result) =
      optimizer.nonEmptyOptimize(Programs.applicativeProgram)(interp).run(StateInfo.empty).value

    assertEquals(info.inserts.size, 1)
    assertEquals(info.searches.size, 2)
    val control = Programs.applicativeProgram(interp).runA(StateInfo.empty).value
    assertEquals(result, control)
  }

  test("Optimizer duplicates should be removed") {

    implicit val optimizer: Optimizer[KVStore, StateO] = kVStoreOptimizer

    def program[F[_]: Applicative](F: KVStore[F]): F[List[String]] =
      List(F.get("Cats"), F.get("Dogs"), F.get("Cats"), F.get("Birds")).sequence
        .map(_.collect { case Some(s) => s })

    val p = new Program[KVStore, Applicative, List[String]] {
      def apply[F[_]: Applicative](alg: KVStore[F]): F[List[String]] = program(alg)
    }

    val interp = Interpreters.KVStoreInterpreter
    val (info, result) = optimizer.optimize(p)(interp).run(StateInfo.empty).value
    assertEquals(info.searches.size, 3)
    val control = program(interp).runA(StateInfo.empty).value
    assertEquals(result, control)
  }
}
