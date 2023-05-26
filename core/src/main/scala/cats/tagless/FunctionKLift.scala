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

import cats.arrow.FunctionK

// https://github.com/typelevel/cats/issues/2553#issuecomment-493712879
// https://github.com/typelevel/cats/blob/v2.9.0/core/src/main/scala-2/src/main/scala/cats/arrow/FunctionKMacros.scala
private[tagless] object FunctionKLift {

  /** Used in the signature of `lift` to emulate a polymorphic function type */
  protected type τ[F[_], G[_]]

  def apply[F[_], G[_]](f: F[τ[F, G]] => G[τ[F, G]]): FunctionK[F, G] =
    new FunctionK[F, G] {
      def apply[A](fa: F[A]): G[A] = f.asInstanceOf[F[A] => G[A]](fa)
    }
}
