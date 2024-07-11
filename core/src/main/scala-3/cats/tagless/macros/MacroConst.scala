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

import scala.annotation.experimental
import scala.quoted.*

@experimental
object MacroConst:
  inline def derive[Alg[_[_]], A](const: A): Alg[Const[A]#λ] = ${ deriveConst[Alg, A]('const) }

  private[macros] def deriveConst[Alg[_[_]]: Type, A: Type](const: Expr[A])(using q: Quotes): Expr[Alg[Const[A]#λ]] =
    import quotes.reflect.*
    given DeriveMacros[q.type] = new DeriveMacros

    Symbol.newClassOf[Alg[Const[A]#λ]](
      transformDef = _ => _ => Some(const.asTerm),
      transformVal = _ => Some(const.asTerm)
    )
