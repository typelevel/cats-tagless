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

package cats.tagless.derived

import scala.annotation.*
import scala.compiletime.summonFrom

// Copied from https://github.com/typelevel/kittens/blob/master/core/src/main/scala-3/cats/derived/Derived.scala
@implicitNotFound("Could not derive an instance of ${A}")
opaque type Derived[A] = A
object Derived:
  def apply[A](instance: A): Derived[A] = instance
  extension [A](derived: Derived[A]) def instance: A = derived
  given [A]: Conversion[A, Derived[A]] = apply

  type Or0[F[_]] = [x] =>> Derived.Or[F[x]]
  type Or1[F[_[_]]] = [x[_]] =>> Derived.Or[F[x]]
  type Or11[F[_[_[_]]]] = [x[_[_]]] =>> Derived.Or[F[x]]
  type Or2[F[_[_, _]]] = [x[_, _]] =>> Derived.Or[F[x]]

  opaque type Or[A] = A
  object Or extends OrInstances:
    def apply[A](instance: A): Or[A] = instance
    extension [A](derived: Or[A]) def unify: A = derived
    extension [I[f[_], t], F[_], T](inst: I[Or0[F], T])
      @targetName("unifyK0")
      def unify: I[F, T] = inst
    extension [I[f[_[_]], t[_]], F[_[_]], T[_]](inst: I[Or1[F], T])
      @targetName("unifyK1")
      def unify: I[F, T] = inst
    extension [I[f[_[_[_]]], t[_[_]]], F[_[_[_]]], T[_[_]]](inst: I[Or11[F], T])
      @targetName("unifyK11")
      def unify: I[F, T] = inst
    extension [I[f[_[_, _]], t[_, _]], F[_[_, _]], T[_, _]](inst: I[Or2[F], T])
      @targetName("unifyK2")
      def unify: I[F, T] = inst

sealed abstract class OrInstances:
  inline given [A]: Derived.Or[A] = summonFrom:
    case instance: A => Derived.Or(instance)
    case derived: Derived[A] => Derived.Or(derived.instance)
