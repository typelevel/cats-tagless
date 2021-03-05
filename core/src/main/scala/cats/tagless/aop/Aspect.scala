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

import cats.tagless.Trivial
import cats.{Eval, ~>}

/** Type class supporting Aspect-Oriented Programming (AOP) for a tagless final algebra.
  * The behaviour of this aspect is defined by two type classes (or constraints) -
  * one for the domain (arguments) and one for the codomain (result) of each method in the algebra.
  *
  * Note: This feature is experimental, API is likely to change.
  *
  * @see `Aspect.Domain`, `Aspect.Codomain` and `Aspect.Function` for convenient type aliases.
  * @tparam Alg The algebra to which this aspect applies.
  * @tparam Dom Type class / constraint that should exist for all parameter types of each algebra methods (domain).
  * @tparam Cod Type class / constraint that should exist for the return type of each algebra methods (codomain).
  */
trait Aspect[Alg[_[_]], Dom[_], Cod[_]] extends Instrument[Alg] {
  def weave[F[_]](af: Alg[F]): Alg[Aspect.Weave[F, Dom, Cod, *]]
  def instrument[F[_]](af: Alg[F]): Alg[Instrumentation[F, *]] =
    mapK(weave(af))(Aspect.Weave.instrumentationK)
}

object Aspect {
  type Domain[Alg[_[_]], F[_]] = Aspect[Alg, F, Trivial]
  type Codomain[Alg[_[_]], F[_]] = Aspect[Alg, Trivial, F]
  type Function[Alg[_[_]], F[_]] = Aspect[Alg, F, F]

  def apply[Alg[_[_]], Dom[_], Cod[_]](implicit aspect: Aspect[Alg, Dom, Cod]): Aspect[Alg, Dom, Cod] = aspect
  def domain[Alg[_[_]], F[_]](implicit aspect: Domain[Alg, F]): Domain[Alg, F] = aspect
  def codomain[Alg[_[_]], F[_]](implicit aspect: Codomain[Alg, F]): Codomain[Alg, F] = aspect
  def function[Alg[_[_]], F[_]](implicit aspect: Function[Alg, F]): Function[Alg, F] = aspect

  /** An [[Aspect.Weave]] represents a reified cross-cutting concern for a single method of an algebra.
    * It can be applied to all method arguments, result, or both. Its behaviour is driven by type classes.
    *
    * @see `Weave.Domain`, `Weave.Codomain` and `Weave.Function` for convenient type aliases.
    * @param domain `Aspect.Advice` for all arguments except implicits. Target in `Eval` to capture by-name arguments.
    * @param codomain `Aspect.Advice` for the result of the method. Target in `F` - the underlying algebra carrier.
    * @tparam F The underlying algebra carrier type which is the result of forwarding the method call.
    * @tparam Dom Type class / constraint that should exist for all parameter types except implicits (domain).
    * @tparam Cod Type class / constraint that should exist for the return type of the method (codomain).
    * @tparam A Return type of the method.
    */
  final case class Weave[F[_], Dom[_], Cod[_], A](
      algebraName: String,
      domain: List[List[Advice[Eval, Dom]]],
      codomain: Advice.Aux[F, Cod, A]
  ) {

    /** Convert this [[Weave]] to an [[Instrumentation]], throwing away information about the `domain`. */
    def instrumentation: Instrumentation[F, A] =
      Instrumentation(codomain.target, algebraName, codomain.name)
  }

  object Weave {
    type Domain[F[_], G[_], A] = Weave[F, G, Trivial, A]
    type Codomain[F[_], G[_], A] = Weave[F, Trivial, G, A]
    type Function[F[_], G[_], A] = Weave[F, G, G, A]

    def instrumentationK[F[_], Dom[_], Cod[_]]: Weave[F, Dom, Cod, *] ~> Instrumentation[F, *] =
      λ[Aspect.Weave[F, Dom, Cod, *] ~> Instrumentation[F, *]](_.instrumentation)
  }

  /** An [[Aspect.Advice]] represents the effect of a particular [[Aspect]] on a single value (the `target`).
    * It witnesses that for some value `F[A]` there exists a compatible type class / constraint instance `G[A]`.
    * The target type `A` is existentially quantified to enable capturing all arguments lists of a method.
    *
    * @tparam F The carrier type of the target value (`Eval` for method arguments).
    * @tparam G The type class / constraint that defines the behaviour of this [[Aspect.Advice]].
    */
  trait Advice[F[_], G[_]] extends Serializable {
    type A
    def name: String
    def target: F[A]
    implicit def instance: G[A]
    override def toString: String = s"$name: $target"
  }

  object Advice {
    type Aux[F[_], G[_], T] = Advice[F, G] { type A = T }

    def apply[F[_], G[_], T](adviceName: String, adviceTarget: F[T])(implicit G: G[T]): Aux[F, G, T] =
      new Advice[F, G] {
        type A = T
        val name = adviceName
        val target = adviceTarget
        val instance = G
      }

    def byValue[G[_], T: G](name: String, value: T): Aux[Eval, G, T] =
      Advice(name, Eval.now(value))

    def byName[G[_], T: G](name: String, thunk: => T): Aux[Eval, G, T] =
      Advice(name, Eval.always(thunk))
  }
}
