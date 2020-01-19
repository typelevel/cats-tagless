package cats.tagless

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

import simulacrum.typeclass
import cats.~>
import cats.Apply
import cats.arrow.FunctionK

@typeclass
trait TraverseK[Alg[_[_]]] extends FunctorK[Alg] {

  def traverseK[F[_], G[_]: Apply, H[_]](alg: Alg[F])(fk: F ~> λ[a => G[H[a]]]): G[Alg[H]]

  def sequenceK[F[_]: Apply, G[_]](alg: Alg[λ[a => F[G[a]]]]): F[Alg[G]] =
    traverseK[λ[a => F[G[a]]], F, G](alg)(FunctionK.id[λ[a => F[G[a]]]])

  override def mapK[F[_], G[_]](af: Alg[F])(fk: F ~> G): Alg[G] = traverseK[F, cats.Id, G](af)(fk)

  def sequenceKId[F[_]: Apply](alg: Alg[F]): F[Alg[cats.Id]] = sequenceK[F, cats.Id](alg)
}
