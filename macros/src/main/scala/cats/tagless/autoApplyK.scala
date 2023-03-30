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

/** Auto generates an instance of [[ApplyK]]. */
@compileTimeOnly("Cannot expand @autoApplyK")
class autoApplyK(autoDerivation: Boolean = true) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro autoApplyKMacros.newDef
}

private[tagless] class autoApplyKMacros(override val c: whitebox.Context)
    extends MacroUtils
    with CovariantKMethodsGenerator {
  import c.universe.*

  private def generateApplyKFor(algebraName: String)(algebraType: Tree, typeParams: Seq[TypeDef]) =
    typeClassInstance(
      TermName("applyKFor" + algebraName),
      typeParams,
      tq"_root_.cats.tagless.ApplyK[$algebraType]",
      q"_root_.cats.tagless.Derive.applyK[$algebraType]"
    )

  def instanceDef(algebra: AlgDefn.UnaryAlg): Tree =
    algebra.forVaryingEffectType(generateApplyKFor(algebra.name))

  def newDef(annottees: c.Tree*): c.Tree =
    enrichAlgebra(annottees.toList)(algebra =>
      instanceDef(algebra) :: companionMapKDef(algebra) :: autoDerivationDef(algebra) :: Nil
    )
}
