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

    val RT = TypeRepr.of[ReaderT[F, Alg[F], ?]]

    def readerT[T: Type](owner: Symbol)(body: Term => Term): Term =
      '{ ReaderT((af: Alg[F]) => ${ body('af.asTerm).asExprOf[F[T]] }) }.asTerm.changeOwner(owner)

    def argTransformer(af: Term): dm.Transform =
      case (_, tpe, arg) if tpe <:< RT => Select.unique(arg, "apply").appliedTo(af)

    def transformDef(method: DefDef)(argss: List[List[Tree]]): Option[Term] =
      method.returnTpt.tpe.asType match
        case '[ReaderT[F, Alg[F], t]] =>
          Some(readerT[t](method.symbol): af =>
            af.call(method.symbol):
              for (clause, args) <- method.paramss.zip(argss)
              yield for paramAndArg <- clause.params.zip(args)
              yield argTransformer(af).transformArg(method.symbol, paramAndArg))
        case _ =>
          report.errorAndAbort:
            s"Expected method ${method.name} to return ${RT.show} but found ${method.returnTpt.tpe.show}"

    def transformVal(value: ValDef): Option[Term] =
      value.tpt.tpe.asType match
        case '[ReaderT[F, Alg[F], t]] =>
          Some(readerT[t](value.symbol)(_.select(value.symbol)))
        case _ =>
          report.errorAndAbort:
            s"Expected value ${value.name} to be of type ${RT.show} but found ${value.tpt.tpe.show}"

    None.newClassOf[Alg[[X] =>> ReaderT[F, Alg[F], X]]](transformDef, transformVal)
