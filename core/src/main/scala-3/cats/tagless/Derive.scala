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
import cats.tagless.*
import cats.tagless.macros.*

import scala.annotation.experimental

@experimental
object Derive:
  inline def functor[F[_]]: Functor[F] = MacroFunctor.derive[F]
  inline def contravariant[F[_]]: Contravariant[F] = MacroContravariant.derive[F]
  inline def invariant[F[_]]: Invariant[F] = MacroInvariant.derive[F]
  inline def bifunctor[F[_, _]]: Bifunctor[F] = MacroBifunctor.derive[F]
  inline def functorK[Alg[_[_]]]: FunctorK[Alg] = MacroFunctorK.derive[Alg]
  inline def semigroupalK[Alg[_[_]]]: SemigroupalK[Alg] = MacroSemigroupalK.derive[Alg]
  inline def invariantK[Alg[_[_]]]: InvariantK[Alg] = MacroInvariantK.derive[Alg]
  inline def applyK[Alg[_[_]]]: ApplyK[Alg] = MacroApplyK.derive[Alg]
  inline def contravariantK[Alg[_[_]]]: ContravariantK[Alg] = MacroContravariantK.derive[Alg]
