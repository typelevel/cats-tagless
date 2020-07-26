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

/** Auto generates an instance of [[cats.tagless.aop.Instrument]]. */
@compileTimeOnly("Cannot expand @autoInstrument")
class autoInstrument extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro autoInstrumentMacros.instrumentInst
}

private[tagless] class autoInstrumentMacros(override val c: whitebox.Context) extends MacroUtils  {
  import c.universe._

  private def generateInstrumentFor(algebraName: String)(algebraType: Tree, typeParams: Seq[TypeDef]) =
    typeClassInstance(
      TermName("instrumentFor" + algebraName),
      typeParams,
      tq"_root_.cats.tagless.aop.Instrument[$algebraType]",
      q"_root_.cats.tagless.Derive.instrument[$algebraType]"
    )

  def instrumentInst(annottees: c.Tree*): c.Tree =
    enrichAlgebra(annottees.toList, AlgebraResolver.FirstHigherKindedTypeParam) { algebra =>
      algebra.forVaryingEffectType(generateInstrumentFor(algebra.name)) :: Nil
    }
}
