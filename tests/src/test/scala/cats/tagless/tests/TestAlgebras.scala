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

package cats.tagless
package tests

import cats.{Eq, Eval, Monoid, ~>}
import cats.data.{EitherT, Kleisli, State, Tuple2K}
import cats.implicits._
import cats.laws.discipline.ExhaustiveCheck
import cats.laws.discipline.arbitrary._
import cats.laws.discipline.eq._
import cats.tagless.instances.all._
import org.scalacheck.{Arbitrary, Cogen}

import scala.util.Try

@finalAlg @autoFunctorK @autoSemigroupalK @autoProductNK
trait SafeAlg[F[_]] {
  def parseInt(str: String): F[Int]
  def divide(dividend: Float, divisor: Float): F[Float]
}

object SafeAlg  {
  import TestInstances._

  implicit def eqForSafeAlg[F[_]](implicit eqFi: Eq[F[Int]], eqFf: Eq[F[Float]]): Eq[SafeAlg[F]] =
    Eq.by(algebra => (algebra.parseInt _, algebra.divide _))

  implicit def arbitrarySafeAlg[F[_]](
    implicit arbFi: Arbitrary[F[Int]], arbFs: Arbitrary[F[Float]]
  ): Arbitrary[SafeAlg[F]] = Arbitrary {
    for {
      pInt <- Arbitrary.arbitrary[String => F[Int]]
      div <- Arbitrary.arbitrary[(Float, Float) => F[Float]]
    } yield new SafeAlg[F] {
      def parseInt(str: String) = pInt(str)
      def divide(dividend: Float, divisor: Float) = div(dividend, divisor)
    }
  }
}

@finalAlg @autoInvariantK
trait SafeInvAlg[F[_]] {
  def parseInt(fs: F[String]): F[Int]
  def doubleParser(precision: Int): Kleisli[F, String, Double]
  def parseIntOrError(fs: EitherT[F, String, String]): F[Int]
}

object SafeInvAlg {
  import TestInstances._

  implicit def eqForSafeInvAlg[F[_]](
    implicit eqFi: Eq[F[Int]],
    eqFd: Eq[F[Double]],
    exFs: ExhaustiveCheck[F[String]],
    exEfs: ExhaustiveCheck[EitherT[F, String, String]]
  ): Eq[SafeInvAlg[F]] = Eq.by {
    algebra => (algebra.parseInt _, algebra.doubleParser _, algebra.parseIntOrError _)
  }

  implicit def arbitrarySafeInvAlg[F[_]](
    implicit coF: Cogen[F[String]],
    coEfs: Cogen[EitherT[F, String, String]],
    arbFi: Arbitrary[F[Int]],
    arbFd: Arbitrary[F[Double]]
  ): Arbitrary[SafeInvAlg[F]] = Arbitrary {
    for {
      pInt <- Arbitrary.arbitrary[F[String] => F[Int]]
      pDouble <- Arbitrary.arbitrary[Int => Kleisli[F, String, Double]]
      pError <- Arbitrary.arbitrary[EitherT[F, String, String] => F[Int]]
    } yield new SafeInvAlg[F] {
      def parseInt(fs: F[String]) = pInt(fs)
      def doubleParser(precision: Int) = pDouble(precision)
      def parseIntOrError(fs: EitherT[F, String, String]) = pError(fs)
    }
  }
}

trait KVStore[F[_]] {
  def get(key: String): F[Option[String]]
  def put(key: String, a: String): F[Unit]
}

object KVStore {
  implicit val applyKForKVStore: ApplyK[KVStore] = new ApplyK[KVStore] {
    def mapK[F[_], G[_]](af: KVStore[F])(f: ~>[F, G]): KVStore[G] = new KVStore[G] {
      def get(key: String): G[Option[String]] = f(af.get(key))
      def put(key: String, a: String): G[Unit] = f(af.put(key, a))
    }

    def productK[F[_], G[_]](af: KVStore[F], ag: KVStore[G]): KVStore[Tuple2K[F, G, ?]] =
      new KVStore[Tuple2K[F, G, ?]] {
        def get(key: String): Tuple2K[F, G, Option[String]] =
          Tuple2K(af.get(key), ag.get(key))

        def put(key: String, a: String): Tuple2K[F, G, Unit] =
          Tuple2K(af.put(key, a), ag.put(key, a))
      }
  }
}

case class KVStoreInfo(queries: Set[String], cache: Map[String, String])

object KVStoreInfo {
  implicit val infoMonoid: Monoid[KVStoreInfo] = new Monoid[KVStoreInfo] {
    def combine(a: KVStoreInfo, b: KVStoreInfo): KVStoreInfo =
      KVStoreInfo(a.queries |+| b.queries, a.cache |+| b.cache)

    def empty: KVStoreInfo = KVStoreInfo(Set.empty, Map.empty)
  }
}

object Interpreters {

  implicit object tryInterpreter extends SafeAlg[Try] {
    def parseInt(str: String): Try[Int] = Try(str.toInt)
    def divide(dividend: Float, divisor: Float): Try[Float] = Try(dividend / divisor)
  }

  implicit object lazyInterpreter extends SafeAlg[Eval] {
    def parseInt(str: String): Eval[Int] = Eval.later(str.toInt)
    def divide(dividend: Float, divisor: Float): Eval[Float] = Eval.later(dividend / divisor)
  }

  object KVStoreInterpreter extends KVStore[State[StateInfo, ?]] {
    def get(key: String): State[StateInfo, Option[String]] =
      State.modify[StateInfo](s => s.copy(searches = s.searches.updated(key, s.searches.getOrElse(key, 0) + 1)))
        .as(Option(key + "!"))

    def put(key: String, a: String): State[StateInfo, Unit] =
      State.modify[StateInfo](s => s.copy(inserts = s.inserts.updated(key, s.inserts.getOrElse(key, 0) + 1)))
  }

  case class StateInfo(searches: Map[String, Int], inserts: Map[String, Int])

  object StateInfo {
    def empty = StateInfo(Map.empty, Map.empty)
  }
}
