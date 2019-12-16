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

package cats.tagless

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.collection.immutable.Seq
import scala.reflect.macros.whitebox

/** Auto generates an instance of `cats.Semigroupal`. */
@compileTimeOnly("Cannot expand @autoSemigroupal")
class autoSemigroupal extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro autoSemigroupalMacros.semigroupalInst
}


private[tagless] class autoSemigroupalMacros(override val c: whitebox.Context) extends MacroUtils {
  import c.universe._

  private def generateSemigroupalFor(algebraName: String)(algebraType: Tree, typeParams: Seq[TypeDef]) =
    typeClassInstance(
      TermName("semigroupalFor" + algebraName),
      typeParams,
      tq"_root_.cats.Semigroupal[$algebraType]",
      q"_root_.cats.tagless.Derive.semigroupal[$algebraType]"
    )

  def semigroupalInst(annottees: c.Tree*): c.Tree =
    enrichAlgebra(annottees.toList, AlgebraResolver.LastRegularTypeParam) { algebra =>
      algebra.forVaryingEffectType(generateSemigroupalFor(algebra.name)) :: Nil
    }
}
