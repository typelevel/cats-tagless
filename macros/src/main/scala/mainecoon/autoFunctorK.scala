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

package mainecoon

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

abstract class FunctorKInstanceGenerator(ad: AlgDefn) {
  import ad._

  def covariantTransform(resultType: Type, originImpl: Term): (Type, Term) = {
    resultType match {
      case t"${Type.Name(`effectTypeName`)}[$resultTpeArg]" =>
        (t"G[$resultTpeArg]", q"fk($originImpl)")
      case _ => (resultType, originImpl)
    }
  }

  lazy val defWithoutParams: Seq[Stat] =
    fromExistingStats {
      case q"def $methodName[..$mTParams]: $resultType" =>
        val (newResultType, newImpl) = covariantTransform(resultType, q"af.$methodName" )
        q"""def $methodName[..$mTParams]: $newResultType = $newImpl"""
    }
}

class CovariantKInstanceGenerator(algDefn: AlgDefn, autoDerivation: Boolean) extends CovariantKMethodsGenerator(algDefn) {
  import algDefn._
  import cls._
  lazy val instanceDef: Seq[Defn] = {
    Seq(
      companionMapKDef,
      q"""
        implicit def ${Term.Name("functorKFor" + name.value)}[..$extraTParams]: _root_.mainecoon.FunctorK[$typeLambdaVaryingHigherKindedEffect] =
          new _root_.mainecoon.FunctorK[$typeLambdaVaryingHigherKindedEffect] {
            $instanceMapKDef
          }
      """
    )
  }

  lazy val instanceDefFullyRefined: Seq[Defn] = {
    Seq(
      q"""
       object fullyRefined {
         implicit def ${Term.Name("functorKForFullyRefined" + name.value)}[..${fullyRefinedTParams}]: _root_.mainecoon.FunctorK[$typeLambdaVaryingHigherKindedEffectFullyRefined] =
           new _root_.mainecoon.FunctorK[$typeLambdaVaryingHigherKindedEffectFullyRefined] {
             def mapK[F[_], G[_]]($from: ${fullyRefinedTypeSig("F")})(fk: _root_.cats.~>[F, G]):${fullyRefinedTypeSig("G")} =
                ${newInstance(newTypeMemberFullyRefined)}
         }

         object autoDerive {
           @SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
           implicit def fromFunctorK[${effectType}, G[_], ..${fullyRefinedTParams}](
             implicit fk: _root_.cats.~>[${effectTypeArg}, G],
             FK: _root_.mainecoon.FunctorK[$typeLambdaVaryingHigherKindedEffectFullyRefined],
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


class CovariantKMethodsGenerator(algDefn: AlgDefn) extends FunctorKInstanceGenerator(algDefn) {
  import algDefn._
  import cls._

  def autoDerivationDef: Defn =
    q"""
      object autoDerive {
        @SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
        implicit def fromFunctorK[${effectType}, G[_], ..${extraTParams}](
          implicit fk: _root_.cats.~>[${effectTypeArg}, G],
          FK: _root_.mainecoon.FunctorK[$typeLambdaVaryingHigherKindedEffect],
          af: ${newTypeSig("F")})
          : ${newTypeSig("G")} = FK.mapK(af)(fk)
      }"""

  def covariantKMethods(from: Term.Name): Seq[Stat] =
    fromExistingStats {
      case q"def $methodName[..$mTParams](..$params): $resultType" =>
        val (newResultType, newImpl) = covariantTransform(resultType, q"$from.$methodName(..${arguments(params)})" )
        q"""def $methodName[..$mTParams](..$params): $newResultType = $newImpl"""
      case q"def $methodName[..$mTParams](..$params)(..$params2): $resultType" =>
        val (newResultType, newImpl) = covariantTransform(resultType, q"$from.$methodName(..${arguments(params)})(..${arguments(params2)})" )
        q"""def $methodName[..$mTParams](..$params)(..$params2): $newResultType = $newImpl"""
    } ++ defWithoutParams

  val from = Term.Name("af")

  def  newInstance(typeMembers: Seq[Defn.Type]): Term.New =
    q"""new ${Ctor.Ref.Name(name.value)}[..${tArgs("G")}] {
          ..$typeMembers
          ..${covariantKMethods(from)}
     }"""

  lazy val companionMapKDef: Defn = {
      q"""
        def mapK[F[_], G[_], ..$extraTParams]($from: ${newTypeSig("F")})(fk: _root_.cats.~>[F, G]): ${dependentRefinedTypeSig("G", from)} =
          ${newInstance(newTypeMember(from))}
      """
  }

  lazy val instanceMapKDef: Defn = {
      q"""
        def mapK[F[_], G[_]]($from: ${newTypeSig("F")})(fk: _root_.cats.~>[F, G]): ${newTypeSig("G")} =
          ${newInstance(newTypeMember(from))}
      """
  }

}




