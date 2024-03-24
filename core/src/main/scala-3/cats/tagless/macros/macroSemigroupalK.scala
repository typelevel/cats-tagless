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

import quoted.*
import scala.annotation.experimental
import compiletime.asMatchable

object macroSemigroupalK:
  import Utils.*

  inline def derive[Alg[_[_]]] = ${ semigroupalK[Alg] }

  @experimental def semigroupalK[Alg[_[_]]: Type](using Quotes): Expr[SemigroupalK[Alg]] =
    import quotes.reflect.*

    '{
      new SemigroupalK[Alg]:
        def productK[F[_], G[_]](af: Alg[F], ag: Alg[G]): Alg[Tuple2K[F, G, *]] =
          ${ capture('af, 'ag) }
    }

  @experimental def capture[Alg[_[_]]: Type, F[_]: Type, G[_]: Type](afe: Expr[Alg[F]], age: Expr[Alg[G]])(using
      Quotes
  ): Expr[Alg[Tuple2K[F, G, *]]] =
    import quotes.reflect.*
    val className = "$anon()"
    val parents = List(TypeTree.of[Object], TypeTree.of[Alg[Tuple2K[F, G, *]]])
    val decls = memberSymbolsAsSeen[Alg, Tuple2K[F, G, *]]

    val cls = Symbol.newClass(Symbol.spliceOwner, className, parents = parents.map(_.tpe), decls, selfType = None)
    val body =
      cls.declaredMethods.map(method => (method, method.tree)).collect { case (method, DefDef(_, _, typedTree, _)) =>
        DefDef(
          method,
          argss =>
            typedTree.tpe.simplified.asMatchable match
              case AppliedType(tr, inner) =>
                val aafe = methodApply(afe)(method, argss)
                val aage = methodApply(age)(method, argss)
                Some(newInstanceCall(Symbol.classSymbol(classNameTuple2K), inner, List(aafe, aage)))
              case e =>
                val apply = methodApply(afe)(method, argss)
                Some(apply)
        )
      }

    val clsDef = ClassDef(cls, parents, body = body)
    val newCls =
      Typed(Apply(Select(New(TypeIdent(cls)), cls.primaryConstructor), Nil), TypeTree.of[Alg[Tuple2K[F, G, *]]])
    val expr = Block(List(clsDef), newCls).asExpr

    expr.asExprOf[Alg[Tuple2K[F, G, *]]]
