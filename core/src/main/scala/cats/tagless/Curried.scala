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

package cats
package tagless

private object Curried {
  type OneAnd[A] = { type λ[F[_]] = data.OneAnd[F, A] }
  type IdT[A] = { type λ[F[_]] = data.IdT[F, A] }
  type OptionT[A] = { type λ[F[_]] = data.OptionT[F, A] }
  type EitherT[A, B] = { type λ[F[_]] = data.EitherT[F, A, B] }
  type IorT[A, B] = { type λ[F[_]] = data.IorT[F, A, B] }
  type WriterT[A, B] = { type λ[F[_]] = data.WriterT[F, A, B] }
  type Func[A, B] = { type λ[F[_]] = data.Func[F, A, B] }
  type Kleisli[A, B] = { type λ[F[_]] = data.Kleisli[F, A, B] }
  type Cokleisli[A, B] = { type λ[F[_]] = data.Cokleisli[F, A, B] }
  type FunctionKFrom[G[_]] = { type λ[F[_]] = arrow.FunctionK[F, G] }
  type FunctionKTo[F[_]] = { type λ[G[_]] = arrow.FunctionK[F, G] }
  type Tuple2KFirst[G[_], A] = { type λ[F[_]] = data.Tuple2K[F, G, A] }
  type Tuple2KSecond[F[_], A] = { type λ[G[_]] = data.Tuple2K[F, G, A] }
  type EitherKLeft[G[_], A] = { type λ[F[_]] = data.EitherK[F, G, A] }
  type EitherKRight[F[_], A] = { type λ[G[_]] = data.EitherK[F, G, A] }
  type NestedOuter[G[_], A] = { type λ[F[_]] = data.Nested[F, G, A] }
  type NestedInner[F[_], A] = { type λ[G[_]] = data.Nested[F, G, A] }
}
