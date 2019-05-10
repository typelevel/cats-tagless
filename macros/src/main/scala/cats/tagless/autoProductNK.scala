/*
 * Copyright 2017 Kailuo Wang
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

package cats.tagless
import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.collection.immutable.Seq
import scala.reflect.macros.whitebox

/**
  * auto generates methods in companion object to compose multiple interpreters into an interpreter of a `TupleNK` effects
  */
@compileTimeOnly("Cannot expand @autoProductK")
class autoProductNK extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro autoProductNKMacros.productKInst
}

private [tagless] class autoProductNKMacros(override val c: whitebox.Context) extends MacroUtils {
  import c.universe._

  def productMethod(typedef: TypeDefinition)(arity: Int): Tree = {
    def af(n: Int) = TermName("af" + n)

    val name = typedef.ident
    val range = 1 to arity

    // tparam"F1[_], F2[_], F3[_]"
    val effectTypeParams: List[TypeDef] =
      range.map(n => createTypeParam("F" + n, 1)).toList
    val effectTypeParamsNames = tArgs(effectTypeParams)

    //Tuple3K
    val productTypeName = tq"_root_.cats.tagless.${TypeName("Tuple" + arity + "K")}"

    val methods = typedef.impl.body.map {
      case q"def $methodName[..$mTParams](..$params): ${_}[$resultType]" =>
        val returnItems = range.map { n =>
          q"${af(n)}.$methodName(..${arguments(params)})"
        }
        q"""def $methodName[..$mTParams](..$params): $productTypeName[..$effectTypeParamsNames]#λ[$resultType] =
           (..$returnItems)"""
      //curried version
      case q"def $methodName[..$mTParams](..$params1)(..$params2): ${_}[$resultType]" =>
        val returnItems = range.map { n =>
          q"${af(n)}.$methodName(..${arguments(params1)})(..${arguments(params2)})"
        }
        q"""def $methodName[..$mTParams](..$params1)(..$params2): $productTypeName[..$effectTypeParamsNames]#λ[$resultType] =
           (..$returnItems)"""
      case st =>
        abort(
          s"autoProductK does not support algebra with such statement: $st"
        )
    }

    //af1: A[F1], af2: A[F2], af3: A[F3]
    val inboundInterpreters: Seq[ValDef] =
      (range zip effectTypeParamsNames).map {
        case (idx, fx) =>
          ValDef(
            Modifiers(Flag.PARAM),
            af(idx),
            typedef.applied(fx),
            EmptyTree
          )
      }

    q"""
        def ${TermName("product" + arity + "K")}[..$effectTypeParams](..$inboundInterpreters): $name[$productTypeName[..$effectTypeParamsNames]#λ] =
          new $name[$productTypeName[..$effectTypeParamsNames]#λ] {
            ..$methods
          }
      """

  }

  def productKInst(annottees: c.Tree*): c.Tree =
    enrich(annottees.toList) { td =>
      val productMethods = (3 to 9).map(productMethod(td))
      Seq(td.defn, addStats(td.companion, productMethods))
    }
}