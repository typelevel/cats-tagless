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
import cats.data.ReaderT
import cats.tagless.aop.{Aspect, Instrument}
import cats.{Apply, Bifunctor, Contravariant, FlatMap, Functor, Invariant, Semigroupal}

object Derive {

  /** Derives an implementation of `Alg` where all abstract methods return a constant value. */
  def const[Alg[_[_]], A](value: A): Alg[Const[A]#λ] = macro DeriveMacros.const[Alg, A]

  /** Derives an implementation of `Alg` where all abstract methods return `Unit`. */
  def void[Alg[_[_]]]: Alg[Void] = macro DeriveMacros.void[Alg]

  /** Derives an implementation of `Alg` that forwards all calls to another one supplied via `ReaderT`. This enables a
    * form of dependency injection.
    */
  def readerT[Alg[_[_]], F[_]]: Alg[ReaderT[F, Alg[F], *]] = macro DeriveMacros.readerT[Alg, F]

  def functor[F[_]]: Functor[F] = macro DeriveMacros.functor[F]
  def contravariant[F[_]]: Contravariant[F] = macro DeriveMacros.contravariant[F]
  def invariant[F[_]]: Invariant[F] = macro DeriveMacros.invariant[F]
  def profunctor[F[_, _]]: Profunctor[F] = macro DeriveMacros.profunctor[F]
  def bifunctor[F[_, _]]: Bifunctor[F] = macro DeriveMacros.bifunctor[F]
  def semigroupal[F[_]]: Semigroupal[F] = macro DeriveMacros.semigroupal[F]
  def apply[F[_]]: Apply[F] = macro DeriveMacros.apply[F]
  def flatMap[F[_]]: FlatMap[F] = macro DeriveMacros.flatMap[F]

  // format: off
  /** Generates a FunctorK instance.
    * {{{
    * scala> import util.Try
    * scala> import cats.~>
    * scala> trait StringAlg[F[_]] {
    *      |   def head(s: String): F[String]
    *      | }
    * scala> val tryInterpreter = new StringAlg[Try] {
    *      |   //for simplicity we use a Try here, but we are not encouraging it.
    *      |   def head(s: String): Try[String] = Try(s.head).map(_.toString)
    *      | }
    * scala> val derived = cats.tagless.Derive.functorK[StringAlg]
    * scala> val optionInterpreter = derived.mapK(tryInterpreter)(λ[Try ~> Option]{ _.toOption })
    * scala> optionInterpreter.head("blah")
    * res1: Option[String] = Some(b)
    * scala> optionInterpreter.head("")
    * res2: Option[String] = None
    * }}}
    */
  // format: on
  def functorK[Alg[_[_]]]: FunctorK[Alg] = macro DeriveMacros.functorK[Alg]

  def contravariantK[Alg[_[_]]]: ContravariantK[Alg] = macro DeriveMacros.contravariantK[Alg]
  def invariantK[Alg[_[_]]]: InvariantK[Alg] = macro DeriveMacros.invariantK[Alg]
  def semigroupalK[Alg[_[_]]]: SemigroupalK[Alg] = macro DeriveMacros.semigroupalK[Alg]
  def applyK[Alg[_[_]]]: ApplyK[Alg] = macro DeriveMacros.applyK[Alg]

  /** Type class for instrumenting an algebra. Note: This feature is experimental, API is likely to change.
    */
  def instrument[Alg[_[_]]]: Instrument[Alg] = macro DeriveMacros.instrument[Alg]

  /** Type class supporting Aspect-Oriented Programming (AOP) for a tagless final algebra. Note: This feature is
    * experimental, API is likely to change.
    */
  def aspect[Alg[_[_]], Dom[_], Cod[_]]: Aspect[Alg, Dom, Cod] = macro DeriveMacros.aspect[Alg, Dom, Cod]
}
