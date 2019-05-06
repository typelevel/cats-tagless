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
  * auto generates an instance of [[InvariantK]]
  */
@compileTimeOnly("Cannot expand @autoInvariantK")
class autoInvariantK(autoDerivation: Boolean = true) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro autoInvariantKMacros.newDef
}

private [tagless] class autoInvariantKMacros(override val c: whitebox.Context) extends MacroUtils {
  import c.universe._

  private def generateInvariantKFor(
                                   algebraName: String
                                 )(algebraType: Tree, tparams: Seq[TypeDef]) = {
    val name = TermName("invariantKFor" + algebraName)
    q"""
        implicit def $name[..$tparams]: _root_.cats.tagless.InvariantK[$algebraType] =
          _root_.cats.tagless.Derive.invariantK[$algebraType]
      """
  }

  def instanceDef(algDefn: AlgDefn): AlgDefn =
    algDefn.forVaryingHigherKindedEffectType(
      generateInvariantKFor(algDefn.name)
    )

  def instanceDefFullyRefined(algDefn: AlgDefn): AlgDefn = {
    algDefn.forVaryingHigherKindedEffectTypeFullyRefined {
      (algebraType, tparams) =>
        val impl = Seq(
          generateInvariantKFor("FullyRefined" + algDefn.name)(
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

  def companionIMapKDef(algDefn: AlgDefn) = {
    val from = TermName("af")
    val F = createFreshTypeParam("F", 1)
    val G = createFreshTypeParam("G", 1)
    val algebraF = algDefn.newTypeSig(F)
    val fullyRefinedAlgebraG = algDefn.dependentRefinedTypeSig(G, from)

    algDefn.forVaryingHigherKindedEffectType((algebraType, tparams) => q"""
      def imapK[$F, $G, ..$tparams]($from: $algebraF)(fk: _root_.cats.~>[..${tArgs(F, G)}])(gk: _root_.cats.~>[..${tArgs(G, F)}]): $fullyRefinedAlgebraG =
        _root_.cats.tagless.InvariantK[$algebraType].imapK($from)(fk)(gk).asInstanceOf[$fullyRefinedAlgebraG]
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
          implicit af: $algebraF,
          IK: _root_.cats.tagless.InvariantK[$algebraType],
          fk: _root_.cats.~>[..${tArgs(F, G)}],
          gk: _root_.cats.~>[..${tArgs(G, F)}])
          : $algebraG = IK.imapK(af)(fk)(gk)
      }"""
  }

  def autoDerivationDef(algDefn: AlgDefn) =
    if(autoDerive) algDefn.forVaryingHigherKindedEffectType(generateAutoDerive(algDefn.newTypeSig)) else algDefn

  def newDef(annottees: c.Tree*): c.Tree =
    enrichAlgebra(annottees.toList)(instanceDef _ andThen companionIMapKDef andThen instanceDefFullyRefined andThen autoDerivationDef)
}
