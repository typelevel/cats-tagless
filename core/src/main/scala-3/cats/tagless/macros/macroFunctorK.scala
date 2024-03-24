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

import quoted.*
import scala.annotation.experimental
import compiletime.asMatchable

object macroFunctorK:
  @experimental inline def derive[Alg[_[_]]] = ${ functorK[Alg] }

  @experimental def functorK[Alg[_[_]]: Type](using Quotes): Expr[FunctorK[Alg]] = '{
    new FunctorK[Alg]:
      def mapK[F[_], G[_]](af: Alg[F])(fk: F ~> G): Alg[G] =
        ${ capture('af, 'fk) }
  }

  @experimental def capture[Alg[_[_]]: Type, F[_]: Type, G[_]: Type](eaf: Expr[Alg[F]], efk: Expr[F ~> G])(using
      Quotes
  ): Expr[Alg[G]] =
    import quotes.reflect.*
    val utils = Utils.make

    val className = "$anon()"
    val parents = List(TypeTree.of[Object], TypeTree.of[Alg[G]])
    val decls = utils.membersAsSeenFrom(TypeRepr.of[Alg[G]])

    val cls = Symbol.newClass(Symbol.spliceOwner, className, parents = parents.map(_.tpe), decls, selfType = None)
    val body =
      cls.declaredMethods.map(method => (method, method.tree)).collect { case (method, DefDef(_, _, typedTree, _)) =>
        DefDef(
          method,
          argss =>
            typedTree.tpe.simplified.asMatchable match
              case at @ AppliedType(o, inner) =>
                val apply = utils.call(eaf.asTerm, method, argss)
                Some(Select.overloaded(efk.asTerm, "apply", inner, List(apply)))
              case e =>
                val apply = utils.call(eaf.asTerm, method, argss)
                Some(apply)
        )
      }

    val clsDef = ClassDef(cls, parents, body = body)
    val newCls = Typed(Apply(Select(New(TypeIdent(cls)), cls.primaryConstructor), Nil), TypeTree.of[Alg[G]])
    val expr = Block(List(clsDef), newCls).asExpr

    expr.asExprOf[Alg[G]]
