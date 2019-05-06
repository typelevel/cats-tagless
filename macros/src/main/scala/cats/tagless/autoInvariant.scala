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
import scala.reflect.macros.whitebox

/**
  * auto generates an instance of `cats.Invariant`
  */
@compileTimeOnly("Cannot expand @autoInvariant")
class autoInvariant extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro autoInvariantMacros.invariantInst
}

private[tagless] class autoInvariantMacros(override val c: whitebox.Context) extends MacroUtils  {
  import c.universe._

  private def generateInvariantFor(
    algebraName: String
  )(algebraType: Tree, tparams: Seq[TypeDef]) = {
    val name = TermName("invariantFor" + algebraName)
    q"""
      implicit def $name[..$tparams]: _root_.cats.Invariant[$algebraType] =
        _root_.cats.tagless.Derive.invariant[$algebraType]
    """
  }

  def invariantInst(annottees: c.Tree*): c.Tree =
    enrichAlgebra(annottees.toList, higherKinded = false)(
      ad => ad.forVaryingEffectType(generateInvariantFor(ad.name))
    )
}
