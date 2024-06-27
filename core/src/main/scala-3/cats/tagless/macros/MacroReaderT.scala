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
import cats.data.ReaderT

import scala.annotation.experimental
import scala.quoted.*

@experimental
object MacroReaderT:

  inline def derive[Alg[_[_]], F[_]]: Alg[[X] =>> ReaderT[F, Alg[F], X]] = ${ deriveReaderT }

  private[macros] def deriveReaderT[Alg[_[_]]: Type, F[_]: Type](using
      q: Quotes
  ): Expr[Alg[[X] =>> ReaderT[F, Alg[F], X]]] =
    import quotes.reflect.*
    given dm: DeriveMacros[q.type] = new DeriveMacros

    val F = TypeRepr.of[F]
    val ReaderT = TypeRepr.of[ReaderT]
    val AlgF = TypeRepr.of[Alg[F]]

    val name = Symbol.freshName("$anon")
    val parents = List(TypeTree.of[Object], TypeTree.of[Alg[[X] =>> ReaderT[F, Alg[F], X]]])
    val cls = Symbol.newClass(Symbol.spliceOwner, name, parents.map(_.tpe), _.overridableMembers, None)

    def readerT(owner: Symbol, resultTpe: TypeRepr)(body: Term => Term): Term =
      val methodType = MethodType(List("af"))(_ => List(AlgF), _ => F.appliedTo(resultTpe))
      val lambda = Lambda(
        owner,
        methodType,
        {
          case (sym, (af: Term) :: Nil) => body(af)
          case (_, List(tree)) => tree
          case (sym, args) =>
            report.errorAndAbort(s"Unexpected: $sym with args ${args.map(_.show)}")
        }
      )
      Select
        .unique(Ident(ReaderT.typeSymbol.companionModule.termRef), "apply")
        .appliedToTypes(F :: AlgF :: resultTpe :: Nil)
        .appliedTo(lambda)

    def argTransformer(af: Term): dm.Transform = {
      case (_, tpe, arg) if tpe <:< TypeRepr.of[ReaderT[F, Alg[F], ?]] =>
        val newArg = tpe.typeArgs.last.asType match
          case '[t] =>
            '{ ${ arg.asExprOf[ReaderT[F, Alg[F], t]] }.run(${ af.asExprOf[Alg[F]] }) }
        newArg.asTerm
    }

    def transformDef(method: DefDef)(argss: List[List[Tree]]): Option[Term] =
      val methodReturnTpe = method.returnTpt.tpe.typeArgs.drop(2).head

      Some(readerT(method.symbol, methodReturnTpe) { af =>
        val transformedArgss =
          for (clause, xs) <- method.paramss.zip(argss)
          yield for paramAndArg <- clause.params.zip(xs)
          yield argTransformer(af).transformArg(method.symbol, paramAndArg)

        af.call(method.symbol)(transformedArgss)
      })

    def transformVal(value: ValDef): Option[Term] =
      val valType = value.tpt.tpe.typeArgs.drop(2).head
      Some(readerT(value.symbol, valType) { af =>
        af.select(value.symbol)
      })

    val members = cls.declarations.filterNot(_.isClassConstructor).map { member =>
      member.tree match
        case method: DefDef => DefDef(member, transformDef(method))
        case value: ValDef => ValDef(member, transformVal(value))
        case _ => report.errorAndAbort(s"Not supported: $member in ${member.owner}")
    }

    val newCls = New(TypeIdent(cls)).select(cls.primaryConstructor).appliedToNone
    Block(ClassDef(cls, parents, members) :: Nil, newCls).asExprOf[Alg[[X] =>> ReaderT[F, Alg[F], X]]]
