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

infix type ||[A, B] = OrElse[A, B]

// Copied from https://github.com/typelevel/kittens/blob/master/core/src/main/scala-3/cats/derived/Derived.scala
@implicitNotFound("Could not derive an instance of ${A}")
opaque type Derived[A] = A
object Derived:
  def apply[A](instance: A): Derived[A] = instance
  given conv[A]: Conversion[A, Derived[A]] = identity
  extension [A](derived: Derived[A]) def instance: A = derived
  extension [A](derived: OrElse[A, Derived[A]]) def unify: A = OrElse.unify(derived)

/** A type-level `orElse` - tries to summon `A` first and if not found, then `B`. */
opaque type OrElse[A, B] = A | B
object OrElse extends OrElseInstances:
  def apply[A, B](instance: A | B): OrElse[A, B] = instance
  given conv[A, B]: Conversion[OrElse[A, B], A | B] = identity
  extension [A, B](orElse: OrElse[A, B]) def unify: A | B = orElse
  extension [I[k, t], K, T](instances: I[K, OrElse[T, Derived[T]]])
    @targetName("unifyInstances") def unify: I[K, T] = instances

sealed abstract class OrElseInstances:
  inline given orElse[A, B]: OrElse[A, B] = summonFrom:
    case instance: A => OrElse(instance)
    case instance: B => OrElse(instance)
