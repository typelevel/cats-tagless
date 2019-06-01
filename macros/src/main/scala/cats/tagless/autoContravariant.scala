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

/** Auto generates an instance of `cats.Invariant`. */
@compileTimeOnly("Cannot expand @autoContravariant")
class autoContravariant extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro autoContravariantMacros.contravariantInst
}

private[tagless] class autoContravariantMacros(override val c: whitebox.Context) extends MacroUtils  {
  import c.universe._

  private def generateContravariantFor(algebraName: String)(algebraType: Tree, typeParams: Seq[TypeDef]) =
    typeClassInstance(
      TermName("contravariantFor" + algebraName),
      typeParams,
      tq"_root_.cats.Contravariant[$algebraType]",
      q"_root_.cats.tagless.Derive.contravariant[$algebraType]"
    )

  def contravariantInst(annottees: c.Tree*): c.Tree =
    enrichAlgebra(annottees.toList, higherKinded = false) { algebra =>
      algebra.forVaryingEffectType(generateContravariantFor(algebra.name))
    }
}
