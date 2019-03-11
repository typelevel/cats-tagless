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

package cats.tagless

import cats.{Contravariant, FlatMap, Functor, Invariant}

object Derive {
  def functor[F[_]]: Functor[F] = macro DeriveMacros.functor[F]
  def contravariant[F[_]]: Contravariant[F] = macro DeriveMacros.contravariant[F]
  def invariant[F[_]]: Invariant[F] = macro DeriveMacros.invariant[F]
  def flatMap[F[_]]: FlatMap[F] = macro DeriveMacros.flatMap[F]
  def functorK[Alg[_[_]]]: FunctorK[Alg] = macro DeriveMacros.functorK[Alg]
  def invariantK[Alg[_[_]]]: InvariantK[Alg] = macro DeriveMacros.invariantK[Alg]
  def semigroupalK[Alg[_[_]]]: SemigroupalK[Alg] = macro DeriveMacros.semigroupalK[Alg]
  def applyK[Alg[_[_]]]: ApplyK[Alg] = macro DeriveMacros.applyK[Alg]
}
