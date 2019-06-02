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

/** Auto generates an instance of [[FunctorK]]. */
@compileTimeOnly("Cannot expand @autoFunctorK")
class autoFunctorK(autoDerivation: Boolean = true) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro autoFunctorKMacros.newDef
}

private [tagless] class autoFunctorKMacros(override val c: whitebox.Context) extends MacroUtils with CovariantKMethodsGenerator {
  import c.universe._

  private def generateFunctorKFor(algebraName: String)(algebraType: Tree, typeParams: Seq[TypeDef]) =
    typeClassInstance(
      TermName("functorKFor" + algebraName),
      typeParams,
      tq"_root_.cats.tagless.FunctorK[$algebraType]",
      q"_root_.cats.tagless.Derive.functorK[$algebraType]"
    )

  def instanceDef(algebra: AlgDefn): AlgDefn =
    algebra.forVaryingHigherKindedEffectType(generateFunctorKFor(algebra.name))

  def instanceDefFullyRefined(algDefn: AlgDefn): AlgDefn = {
    algDefn.forVaryingHigherKindedEffectTypeFullyRefined {
      (algebraType, tparams) =>
        val impl = Seq(
          generateFunctorKFor("FullyRefined" + algDefn.name)(
            algebraType,
            tparams
          ),
          generateAutoDerive(algDefn.fullyRefinedTypeSig)(
            algebraType,
            tparams
          )
        )
        q"object fullyRefined { ..$impl }"
    }
  }

  def newDef(annottees: c.Tree*): c.Tree =
    enrichAlgebra(annottees.toList)(instanceDef _ andThen companionMapKDef andThen instanceDefFullyRefined andThen autoDerivationDef)
}

private [tagless] trait CovariantKMethodsGenerator { self: MacroUtils =>
  import c.universe._

  def companionMapKDef(algDefn: AlgDefn) = {
    val from = TermName("af")
    val F = createFreshTypeParam("F", 1)
    val G = createFreshTypeParam("G", 1)
    val algebraF = algDefn.newTypeSig(F)
    val fullyRefinedAlgebraG = algDefn.dependentRefinedTypeSig(G, from)

    algDefn.forVaryingHigherKindedEffectType((algebraType, tparams) => q"""
      def mapK[$F, $G, ..$tparams]($from: $algebraF)(fk: _root_.cats.~>[..${tArgs(F, G)}]): $fullyRefinedAlgebraG =
        _root_.cats.tagless.FunctorK[$algebraType].mapK($from)(fk).asInstanceOf[$fullyRefinedAlgebraG]
    """)
  }

  def generateAutoDerive(newTypeSig: TypeDef => TypTree)(algebraType: Tree, tparams: Seq[TypeDef]) = {
    val F = createFreshTypeParam("F", 1)
    val G = createFreshTypeParam("G", 1)
    val algebraF = newTypeSig(F)
    val algebraG = newTypeSig(G)

    q"""
      object autoDerive {
        @SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
        implicit def fromFunctorK[$F, $G, ..$tparams](
          implicit fk: _root_.cats.~>[..${tArgs(F, G)}],
          FK: _root_.cats.tagless.FunctorK[$algebraType],
          af: $algebraF)
          : $algebraG = FK.mapK(af)(fk)
      }"""
  }

  def autoDerivationDef(algDefn: AlgDefn) =
    if(autoDerive) algDefn.forVaryingHigherKindedEffectType(generateAutoDerive(algDefn.newTypeSig)) else algDefn

}
