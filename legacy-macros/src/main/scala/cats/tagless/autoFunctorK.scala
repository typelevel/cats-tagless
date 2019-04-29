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
import scala.meta._
import Util._

import collection.immutable.Seq

/**
 * auto generates an instance of [[FunctorK]]
 */
@compileTimeOnly("Cannot expand @autoFunctorK")
class autoFunctorK(autoDerivation: Boolean) extends StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    val autoDerivation: Boolean = this match {
      case q"new $_(${Lit.Boolean(arg)})" => arg
      case q"new $_(autoDerivation = ${Lit.Boolean(arg)})" => arg
      case _  => true
    }
    enrichAlgebra(defn)(a => new CovariantKInstanceGenerator(a, autoDerivation).newDef)
  }
}

class CovariantKInstanceGenerator(algDefn: AlgDefn, autoDerivation: Boolean) extends CovariantKMethodsGenerator(algDefn) {
  import algDefn._
  import cls._
  lazy val instanceDef: Seq[Defn] = {
    Seq(
      companionMapKDef,
      q"""
        implicit def ${Term.Name("functorKFor" + name.value)}[..$extraTParams]: _root_.cats.tagless.FunctorK[$typeLambdaVaryingHigherKindedEffect] =
          _root_.cats.tagless.Derive.functorK[$typeLambdaVaryingHigherKindedEffect]
      """
    )
  }

  lazy val instanceDefFullyRefined: Seq[Defn] = {
    Seq(
      q"""
       object fullyRefined {
         implicit def ${Term.Name("functorKForFullyRefined" + name.value)}[..${fullyRefinedTParams}]: _root_.cats.tagless.FunctorK[$typeLambdaVaryingHigherKindedEffectFullyRefined] =
            _root_.cats.tagless.Derive.functorK[$typeLambdaVaryingHigherKindedEffectFullyRefined]

         object autoDerive {
           @SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
           implicit def fromFunctorK[${effectType}, G[_], ..${fullyRefinedTParams}](
             implicit fk: _root_.cats.~>[${effectTypeArg}, G],
             FK: _root_.cats.tagless.FunctorK[$typeLambdaVaryingHigherKindedEffectFullyRefined],
             af: ${fullyRefinedTypeSig()})
             : ${fullyRefinedTypeSig("G")} = FK.mapK(af)(fk)
           }
       }
      """
    )
  }


  lazy val newDef: TypeDefinition = cls.copy(companion = cls.companion.addStats(instanceDef ++ {
    if(autoDerivation)
      Seq(autoDerivationDef)
    else Nil} ++ instanceDefFullyRefined))
}


class CovariantKMethodsGenerator(algDefn: AlgDefn) {
  import algDefn._

  def autoDerivationDef: Defn =
    q"""
      object autoDerive {
        @SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
        implicit def fromFunctorK[${effectType}, G[_], ..${extraTParams}](
          implicit fk: _root_.cats.~>[${effectTypeArg}, G],
          FK: _root_.cats.tagless.FunctorK[$typeLambdaVaryingHigherKindedEffect],
          af: ${newTypeSig("F")})
          : ${newTypeSig("G")} = FK.mapK(af)(fk)
      }"""

  val from = Term.Name("af")

  lazy val companionMapKDef: Defn = {
    val fullyRefinedAlgebra = dependentRefinedTypeSig("G", from)

    q"""
      def mapK[F[_], G[_], ..$extraTParams]($from: ${newTypeSig("F")})(fk: _root_.cats.~>[F, G]): $fullyRefinedAlgebra =
        _root_.cats.tagless.FunctorK[$typeLambdaVaryingHigherKindedEffect].mapK($from)(fk).asInstanceOf[$fullyRefinedAlgebra]
    """
  }
}




