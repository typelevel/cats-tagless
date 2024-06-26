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

package cats.tagless.aop

import cats.Eval

trait AspectSupport:
  /** An [[Aspect.Advice]] represents the effect of a particular [[Aspect]] on a single value (the `target`). It
    * witnesses that for some value `F[A]` there exists a compatible type class / constraint instance `G[A]`. The target
    * type `A` is existentially quantified to enable capturing all arguments lists of a method.
    *
    * @tparam F
    *   The carrier type of the target value (`Eval` for method arguments).
    * @tparam G
    *   The type class / constraint that defines the behaviour of this [[Aspect.Advice]].
    */
  trait Advice[F[_], G[_]] extends Serializable:
    type A
    def name: String
    def target: F[A]
    implicit def instance: G[A]
    override def toString: String = s"$name: $target"

  object Advice:
    type Aux[F[_], G[_], T] = Advice[F, G] { type A = T }

    type Applied[F[_], G[_]] = [X] =>> G[X] ?=> Aux[F, G, X]
    type AppliedAux[F[_], G[_], T] = G[T] ?=> Aux[F, G, T]
    type ByName[G[_], T] = AppliedAux[Eval, G, T]
    type ByValue[G[_], T] = AppliedAux[Eval, G, T]

    def apply[F[_], G[_], T](adviceName: String, adviceTarget: F[T]): AppliedAux[F, G, T] =
      new Advice[F, G]:
        type A = T
        val name = adviceName
        val target = adviceTarget
        val instance = summon[G[T]]

    def byValue[G[_], T](name: String, value: T): ByValue[G, T] =
      Advice(name, Eval.now(value))

    def byName[G[_], T](name: String, thunk: => T): ByName[G, T] =
      Advice(name, Eval.always(thunk))
