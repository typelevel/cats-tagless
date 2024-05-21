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

package cats.tagless.macros

import cats.tagless.*
import cats.~>
import cats.data.Tuple2K

import scala.annotation.experimental
import scala.quoted.*

@experimental
object MacroApplyK:
  inline def derive[Alg[_[_]]]: ApplyK[Alg] = ${ applyK }

  def applyK[Alg[_[_]]: Type](using Quotes): Expr[ApplyK[Alg]] = '{
    new ApplyK[Alg]:
      def mapK[F[_], G[_]](af: Alg[F])(fk: F ~> G): Alg[G] =
        ${ MacroFunctorK.deriveMapK('{ af }, '{ fk }) }
      def productK[F[_], G[_]](af: Alg[F], ag: Alg[G]): Alg[Tuple2K[F, G, *]] =
        ${ MacroSemigroupalK.deriveProductK('{ af }, '{ ag }) }
  }
