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

/** Auto generates an instance of [[FunctorK]]. */
@compileTimeOnly("Cannot expand @autoFunctorK")
class autoFunctorK(autoDerivation: Boolean = true) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro autoFunctorKMacros.newDef
}

private[tagless] class autoFunctorKMacros(override val c: whitebox.Context)
    extends MacroUtils
    with CovariantKMethodsGenerator {
  import c.universe._

  private def generateFunctorKFor(algebraName: String)(algebraType: Tree, typeParams: Seq[TypeDef]) =
    typeClassInstance(
      TermName("functorKFor" + algebraName),
      typeParams,
      tq"_root_.cats.tagless.FunctorK[$algebraType]",
      q"_root_.cats.tagless.Derive.functorK[$algebraType]"
    )

  def instanceDef(algebra: AlgDefn.UnaryAlg): Tree =
    algebra.forVaryingEffectType(generateFunctorKFor(algebra.name))

  def instanceDefFullyRefined(algebra: AlgDefn.UnaryAlg): Tree =
    algebra.forVaryingEffectTypeFullyRefined { (algebraType, typeParams) =>
      val algebraName = "FullyRefined" + algebra.name
      val methods = List(
        generateFunctorKFor(algebraName)(algebraType, typeParams),
        generateAutoDerive(algebra.fullyRefinedTypeSig, algebraName)(algebraType, typeParams)
      )
      q"object fullyRefined { ..$methods }"
    }

  def newDef(annottees: c.Tree*): c.Tree =
    enrichAlgebra(annottees.toList) { algebra =>
      List(
        instanceDef(algebra),
        companionMapKDef(algebra),
        instanceDefFullyRefined(algebra),
        autoDerivationDef(algebra)
      )
    }
}

private[tagless] trait CovariantKMethodsGenerator { self: MacroUtils =>
  import c.universe._

  def companionMapKDef(algebra: AlgDefn.UnaryAlg) = {
    val af = c.freshName(TermName("af"))
    val fk = c.freshName(TermName("fk"))
    val F = createFreshTypeParam("F", 1)
    val G = createFreshTypeParam("G", 1)
    val algebraF = algebra.newTypeSig(F)
    val fullyRefinedAlgebraG = algebra.dependentRefinedTypeSig(G, af)

    algebra.forVaryingEffectType((algebraType, typeParams) => q"""
      def mapK[$F, $G, ..$typeParams]($af: $algebraF)($fk: _root_.cats.~>[..${tArgs(F, G)}]): $fullyRefinedAlgebraG =
        _root_.cats.tagless.FunctorK[$algebraType].mapK($af)($fk).asInstanceOf[$fullyRefinedAlgebraG]
    """)
  }

  def generateAutoDerive(
      algebraTypeOf: TypeDef => TypTree,
      algebraName: String
  )(algebraType: Tree, typeParams: Seq[TypeDef]) = {
    val _ = algebraType
    val af = c.freshName(TermName("af"))
    val fk = c.freshName(TermName("fk"))
    val F = createFreshTypeParam("F", 1)
    val G = createFreshTypeParam("G", 1)
    val algebraF = algebraTypeOf(F)
    val algebraG = algebraTypeOf(G)

    q"""object autoDerive {
      @SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
      implicit def ${TermName("fromFunctorK" + algebraName)}[$F, $G, ..$typeParams](
        implicit $fk: _root_.cats.~>[..${tArgs(F, G)}], $af: $algebraF
      ): $algebraG = mapK($af)($fk)
    }"""
  }

  def autoDerivationDef(algebra: AlgDefn.UnaryAlg) =
    if (autoDerive) algebra.forVaryingEffectType(generateAutoDerive(algebra.newTypeSig, algebra.name)) else EmptyTree
}
