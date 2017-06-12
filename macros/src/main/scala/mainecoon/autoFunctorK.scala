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
  import cls._

  lazy val newTypeMember: Seq[Defn.Type] =
    fromExistingMethods {
      case q"type $t" =>
        q"type $t = ${Type.Select(Term.Name("af"), Type.Name(t.value))}"
    }

  def fromExistingMethods[T <: Stat](pf: PartialFunction[Stat, T]): Seq[T] =
    templ.stats.toList.flatMap(_.collect(pf))

  lazy val typeSignature: Type = {
    if(newTypeMember.isEmpty) t"$name[..${tArgs("G")}]" else t"$name[..${tArgs("G")}] { ..$newTypeMember }"
  }

  def covariantTransform(resultType: Type, originImpl: Term): (Type, Term) = {
    resultType match {
      case t"${Type.Name(`effectTypeName`)}[$resultTpeArg]" =>
        (t"G[$resultTpeArg]", q"fk($originImpl)")
      case _ => (resultType, originImpl)
    }
  }

  lazy val defWithoutParams: Seq[Stat] =
    fromExistingMethods {
      case q"def $methodName[..$mTParams]: $resultType" =>
        val (newResultType, newImpl) = covariantTransform(resultType, q"af.$methodName" )
        q"""def $methodName[..$mTParams]: $newResultType = $newImpl"""
    }
}

class CovariantKInstanceGenerator(algDefn: AlgDefn, autoDerivation: Boolean) extends FunctorKInstanceGenerator(algDefn) {
  import algDefn._
  import cls._

  lazy val covariantKMethods: Seq[Stat] =
    fromExistingMethods {
      case q"def $methodName[..$mTParams](..$params): $resultType" =>
        val (newResultType, newImpl) = covariantTransform(resultType, q"af.$methodName(..${arguments(params)})" )
        q"""def $methodName[..$mTParams](..$params): $newResultType = $newImpl"""
      case q"def $methodName[..$mTParams](..$params)(..$params2): $resultType" =>
        val (newResultType, newImpl) = covariantTransform(resultType, q"af.$methodName(..${arguments(params)})(..${arguments(params2)})" )
        q"""def $methodName[..$mTParams](..$params)(..$params2): $newResultType = $newImpl"""
    } ++ defWithoutParams

  lazy val instanceDef: Seq[Defn] = {

    //create a mapK method in the companion object with more precise refined type signature
    Seq(q"""
      def mapK[F[_], G[_], ..$extraTParams](af: $name[..${tArgs()}])(fk: _root_.cats.~>[F, G]): ${typeSignature} =
        new ${Ctor.Ref.Name(name.value)}[..${tArgs("G")}] {
          ..${covariantKMethods ++ newTypeMember}
        }""",
      q"""
        implicit def ${Term.Name("functorKFor" + name.value)}[..$extraTParams]: _root_.mainecoon.FunctorK[$typeLambdaVaryingHigherKindedEffect] =
          new _root_.mainecoon.FunctorK[$typeLambdaVaryingHigherKindedEffect] {
            def mapK[F[_], G[_]](af: $name[..${tArgs("F")}])(fk: _root_.cats.~>[F, G]): $name[..${tArgs("G")}] =
              ${Term.Name(name.value)}.mapK(af)(fk)
          }
      """
    )
  }

  lazy val autoDerivationDef: Seq[Defn] = if(autoDerivation)
      Seq(q"""
           object autoDerive {
             implicit def autoDeriveFromFunctorK[${effectType}, G[_], ..${extraTParams}](
               implicit fk: _root_.cats.~>[F, G],
               FK: _root_.mainecoon.FunctorK[$typeLambdaVaryingHigherKindedEffect],
               af: $name[..${tArgs()}])
               : $name[..${tArgs("G")}] = FK.mapK(af)(fk)
           }
          """)
    else Nil

  lazy val newDef: TypeDefinition = cls.copy(companion = cls.companion.addStats(instanceDef ++ autoDerivationDef))
}


