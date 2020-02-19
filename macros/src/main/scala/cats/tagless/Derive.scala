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

import cats.arrow.Profunctor
import cats.tagless.diagnosis.Instrument
import cats.{Apply, Bifunctor, Contravariant, FlatMap, Functor, Invariant, Semigroupal}

object Derive {

  def functor[F[_]]: Functor[F] = macro DeriveMacros.functor[F]
  def contravariant[F[_]]: Contravariant[F] = macro DeriveMacros.contravariant[F]
  def invariant[F[_]]: Invariant[F] = macro DeriveMacros.invariant[F]
  def profunctor[F[_, _]]: Profunctor[F] = macro DeriveMacros.profunctor[F]
  def bifunctor[F[_, _]]: Bifunctor[F] = macro DeriveMacros.bifunctor[F]
  def semigroupal[F[_]]: Semigroupal[F] = macro DeriveMacros.semigroupal[F]
  def apply[F[_]]: Apply[F] = macro DeriveMacros.apply[F]
  def flatMap[F[_]]: FlatMap[F] = macro DeriveMacros.flatMap[F]

  /**
   * Generates a FunctorK instance
   * {{{
   * scala> import util.Try
   * scala> import cats.~>
   * scala> trait StringAlg[F[_]] {
   *      |   def head(s: String): F[String]
   *      | }
   * scala> val tryInterpreter = new StringAlg[Try] {
   *      |    //for simplicity we use a Try here, but we are not encouraging it.
   *      |    def head(s: String): Try[String] = Try(s.head).map(_.toString)
   *      | }
   * scala> val derived = cats.tagless.Derive.functorK[StringAlg]
   * scala> val optionInterpreter = derived.mapK(tryInterpreter)(λ[Try ~> Option]{ _.toOption })
   * scala> optionInterpreter.head("blah")
   * res1: Option[String] = Some(b)
   * scala> optionInterpreter.head("")
   * res2: Option[String] = None
   *
   * }}}
   */
  def functorK[Alg[_[_]]]: FunctorK[Alg] = macro DeriveMacros.functorK[Alg]

  /** Generates a NihilistK instance */
  def nihilistK[Alg[_[_]]]: NihilistK[Alg] = macro DeriveMacros.nihilistK[Alg]

  def contravariantK[Alg[_[_]]]: ContravariantK[Alg] = macro DeriveMacros.contravariantK[Alg]
  def invariantK[Alg[_[_]]]: InvariantK[Alg] = macro DeriveMacros.invariantK[Alg]
  def semigroupalK[Alg[_[_]]]: SemigroupalK[Alg] = macro DeriveMacros.semigroupalK[Alg]
  def applyK[Alg[_[_]]]: ApplyK[Alg] = macro DeriveMacros.applyK[Alg]

  /**
    * Type classes for instrumenting algebras.
    * This feature is experimental, API is likely to change.
    * @tparam Alg The algebra type you want to instrument.
    * @return An instrumented algebra.
    */
  def instrument[Alg[_[_]]]: Instrument[Alg] = macro DeriveMacros.instrument[Alg]
}
