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

import cats.tagless.aop.Instrument
import cats.data.{EitherT, Kleisli, State, Tuple2K}
import cats.laws.discipline.ExhaustiveCheck
import cats.laws.discipline.arbitrary.*
import cats.laws.discipline.eq.*
import cats.syntax.all.*
import cats.{Eq, Eval, Monoid, ~>}
import cats.tagless.{ApplyK, InvariantK, SemigroupalK}
import org.scalacheck.{Arbitrary, Cogen}

import scala.annotation.experimental
import scala.util.Try

@experimental
trait SafeAlg[F[_]] derives SemigroupalK, Instrument:
  def parseInt(str: String): F[Int]
  def divide(dividend: Float, divisor: Float): F[Float]

object SafeAlg:
  import TestInstances.*

  given [F[_]](using Eq[F[Int]], Eq[F[Float]]): Eq[SafeAlg[F]] =
    Eq.by(algebra => (algebra.parseInt, algebra.divide))

  given [F[_]](using Arbitrary[F[Int]], Arbitrary[F[Float]]): Arbitrary[SafeAlg[F]] = Arbitrary:
    for
      parseIntF <- Arbitrary.arbitrary[String => F[Int]]
      divideF <- Arbitrary.arbitrary[(Float, Float) => F[Float]]
    yield new SafeAlg[F]:
      def parseInt(str: String) = parseIntF(str)
      def divide(dividend: Float, divisor: Float) = divideF(dividend, divisor)

@experimental
trait SafeInvAlg[F[_]] derives InvariantK, SemigroupalK:
  def parseInt(fs: F[String]): F[Int]
  def doubleParser(precision: Int): Kleisli[F, String, Double]
  def parseIntOrError(fs: EitherT[F, String, String]): F[Int]

object SafeInvAlg:
  import TestInstances.*

  given [F[_]](using
      Eq[F[Int]],
      Eq[F[Double]],
      ExhaustiveCheck[F[String]],
      ExhaustiveCheck[EitherT[F, String, String]]
  ): Eq[SafeInvAlg[F]] = Eq.by: algebra =>
    (algebra.parseInt, algebra.doubleParser, algebra.parseIntOrError)

  given [F[_]](using
      Cogen[F[String]],
      Cogen[EitherT[F, String, String]],
      Arbitrary[F[Int]],
      Arbitrary[F[Double]]
  ): Arbitrary[SafeInvAlg[F]] = Arbitrary:
    for
      parseIntF <- Arbitrary.arbitrary[F[String] => F[Int]]
      doubleParserF <- Arbitrary.arbitrary[Int => Kleisli[F, String, Double]]
      parseIntOrErrorF <- Arbitrary.arbitrary[EitherT[F, String, String] => F[Int]]
    yield new SafeInvAlg[F]:
      def parseInt(fs: F[String]) = parseIntF(fs)
      def doubleParser(precision: Int) = doubleParserF(precision)
      def parseIntOrError(fs: EitherT[F, String, String]) = parseIntOrErrorF(fs)

@experimental
trait CalculatorAlg[F[_]] derives InvariantK, SemigroupalK:
  def lit(i: Int): F[Int]
  def add(x: F[Int], y: F[Int]): F[Int]
  def show[A](expr: F[A]): String

object CalculatorAlg:
  import TestInstances.*

  given [F[_]](using Eq[F[Int]], ExhaustiveCheck[F[Int]]): Eq[CalculatorAlg[F]] =
    Eq.by(algebra => (algebra.lit, algebra.add, algebra.show[Int]))

  given [F[_]](using Cogen[F[Int]], Arbitrary[F[Int]]): Arbitrary[CalculatorAlg[F]] = Arbitrary:
    for
      litF <- Arbitrary.arbitrary[Int => F[Int]]
      addF <- Arbitrary.arbitrary[(F[Int], F[Int]) => F[Int]]
    yield new CalculatorAlg[F]:
      def lit(i: Int) = litF(i)
      def add(x: F[Int], y: F[Int]) = addF(x, y)
      def show[A](expr: F[A]) = expr.toString

trait KVStore[F[_]]:
  def get(key: String): F[Option[String]]
  def put(key: String, a: String): F[Unit]

object KVStore:
  given ApplyK[KVStore] with
    def mapK[F[_], G[_]](af: KVStore[F])(f: F ~> G): KVStore[G] = new KVStore[G]:
      def get(key: String): G[Option[String]] = f(af.get(key))
      def put(key: String, a: String): G[Unit] = f(af.put(key, a))

    def productK[F[_], G[_]](af: KVStore[F], ag: KVStore[G]): KVStore[Tuple2K[F, G, *]] =
      new KVStore[Tuple2K[F, G, *]]:
        def get(key: String): Tuple2K[F, G, Option[String]] = Tuple2K(af.get(key), ag.get(key))
        def put(key: String, a: String): Tuple2K[F, G, Unit] = Tuple2K(af.put(key, a), ag.put(key, a))

final case class KVStoreInfo(queries: Set[String], cache: Map[String, String])
object KVStoreInfo:
  given Monoid[KVStoreInfo] = Monoid.instance(
    KVStoreInfo(Set.empty, Map.empty),
    (a, b) => KVStoreInfo(a.queries |+| b.queries, a.cache |+| b.cache)
  )

object Interpreters:
  @experimental
  given tryInterpreter: SafeAlg[Try] with
    def parseInt(str: String): Try[Int] = Try(str.toInt)
    def divide(dividend: Float, divisor: Float): Try[Float] = Try(dividend / divisor)

  @experimental
  given lazyInterpreter: SafeAlg[Eval] with
    def parseInt(str: String): Eval[Int] = Eval.later(str.toInt)
    def divide(dividend: Float, divisor: Float): Eval[Float] = Eval.later(dividend / divisor)

  object KVStoreInterpreter extends KVStore[State[StateInfo, *]]:
    def get(key: String): State[StateInfo, Option[String]] = State
      .modify[StateInfo](s => s.copy(searches = s.searches.updated(key, s.searches.getOrElse(key, 0) + 1)))
      .as(Option(key + "!"))
    def put(key: String, a: String): State[StateInfo, Unit] =
      State.modify[StateInfo](s => s.copy(inserts = s.inserts.updated(key, s.inserts.getOrElse(key, 0) + 1)))

  final case class StateInfo(searches: Map[String, Int], inserts: Map[String, Int])
  object StateInfo:
    val empty = StateInfo(Map.empty, Map.empty)
