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

import cats.tagless.{ContravariantK, Derive}

import scala.annotation.experimental
import scala.compiletime.summonFrom

trait DerivedContravariantK:
  @experimental inline def derived[Alg[_[_]]]: ContravariantK[Alg] = summonFrom:
    case derived: Derived[ContravariantK[Alg]] => derived.instance
    case _ => Derive.contravariantK[Alg]
