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

    val name = Symbol.freshName("$anon")
    val parents = List(TypeTree.of[Object], TypeTree.of[Alg[Const[A]#λ]])
    val cls = Symbol.newClass(Symbol.spliceOwner, name, parents.map(_.tpe), _.overridableMembers, None)

    val members = cls.declarations
      .filterNot(_.isClassConstructor)
      .map: member =>
        member.tree match
          case method: DefDef => DefDef(member, _ => Some(const.asTerm))
          case value: ValDef => ValDef(member, Some(const.asTerm))
          case _ => report.errorAndAbort(s"Not supported: $member in ${member.owner}")

    val newCls = New(TypeIdent(cls)).select(cls.primaryConstructor).appliedToNone
    Block(ClassDef(cls, parents, members) :: Nil, newCls).asExprOf[Alg[Const[A]#λ]]
