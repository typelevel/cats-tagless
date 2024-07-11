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

import cats.*
import cats.data.ReaderT
import cats.arrow.Profunctor
import cats.tagless.*
import cats.tagless.aop.*
import cats.tagless.macros.*

import scala.annotation.experimental

@experimental
object Derive:
  inline def functor[F[_]]: Functor[F] = MacroFunctor.derive
  inline def contravariant[F[_]]: Contravariant[F] = MacroContravariant.derive
  inline def invariant[F[_]]: Invariant[F] = MacroInvariant.derive
  inline def semigroupal[F[_]]: Semigroupal[F] = MacroSemigroupal.derive
  inline def apply[F[_]]: Apply[F] = MacroApply.derive
  inline def flatMap[F[_]]: FlatMap[F] = MacroFlatMap.derive
  inline def semigroupK[F[_]]: SemigroupK[F] = MacroSemigroupK.derive
  inline def monoidK[F[_]]: MonoidK[F] = MacroMonoidK.derive
  inline def bifunctor[F[_, _]]: Bifunctor[F] = MacroBifunctor.derive
  inline def profunctor[F[_, _]]: Profunctor[F] = MacroProfunctor.derive
  inline def functorK[Alg[_[_]]]: FunctorK[Alg] = MacroFunctorK.derive
  inline def contravariantK[Alg[_[_]]]: ContravariantK[Alg] = MacroContravariantK.derive
  inline def invariantK[Alg[_[_]]]: InvariantK[Alg] = MacroInvariantK.derive
  inline def semigroupalK[Alg[_[_]]]: SemigroupalK[Alg] = MacroSemigroupalK.derive
  inline def applyK[Alg[_[_]]]: ApplyK[Alg] = MacroApplyK.derive
  inline def const[Alg[_[_]], A](value: A): Alg[Const[A]#λ] = MacroConst.derive[Alg, A](value)
  inline def void[Alg[_[_]]]: Alg[Const[Unit]#λ] = MacroConst.derive[Alg, Unit](())

  /** Derives an implementation of `Alg` that forwards all calls to another one supplied via `ReaderT`. This enables a
    * form of dependency injection.
    */
  inline def readerT[Alg[_[_]], F[_]]: Alg[[X] =>> ReaderT[F, Alg[F], X]] = MacroReaderT.derive
  inline def instrument[Alg[_[_]]]: Instrument[Alg] = MacroInstrument.derive
