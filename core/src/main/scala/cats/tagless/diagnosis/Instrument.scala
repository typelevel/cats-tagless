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

package cats.tagless.diagnosis

import simulacrum.typeclass

final case class Instrumentation[F[_], A](
  value: F[A],
  algebraName: String,
  methodName: String
)

object Instrumentation {
  final case class With[F[_], G[_], A](
    instrumentation: Instrumentation[F, A],
    arguments: List[List[Argument[G]]],
    instance: G[A]
  )

  object With {
    def instance[F[_], G[_], A](
      instrumentation: Instrumentation[F, A],
      arguments: List[List[Argument[G]]]
    )(implicit G: G[A]): With[F, G, A] = With(instrumentation, arguments, G)
  }

  sealed trait Argument[F[_]] extends Serializable {
    type A
    def name: String
    def value: A
    def instance: F[A]
    override def toString: String = s"$name = $value"
  }

  object Argument {
    type Aux[F[_], T] = Argument[F] { type A = T }

    def apply[F[_], T](argName: String, argValue: T)(implicit F: F[T]): Aux[F, T] = new Argument[F] {
      type A = T
      val name = argName
      val value = argValue
      val instance = F
    }
  }
}

/**
  * Type classes for instrumenting algebras.
  * This feature is experimental, API is likely to change.
  * @tparam Alg The algebra type you want to instrument.
  */
@typeclass
trait Instrument[Alg[_[_]]] extends Serializable {
  def instrument[F[_]](af: Alg[F]): Alg[Instrumentation[F, *]]
}

object Instrument {
  trait With[Alg[_[_]], G[_]] extends Serializable {
    def instrumentWith[F[_]](af: Alg[F]): Alg[Instrumentation.With[F, G, *]]
  }
}
