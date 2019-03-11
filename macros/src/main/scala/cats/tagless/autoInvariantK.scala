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
 * auto generates an instance of [[InvariantK]]
 */
@compileTimeOnly("Cannot expand @autoInvariantK")
class autoInvariantK(autoDerivation: Boolean) extends StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    val autoDerivation: Boolean = this match {
      case q"new $_(${Lit.Boolean(arg)})" => arg
      case q"new $_(autoDerivation = ${Lit.Boolean(arg)})" => arg
      case _  => true
    } //todo: code dup with autoFunctorK
    enrichAlgebra(defn)(a => new InvariantKInstanceGenerator(a, autoDerivation).newTypeDef)
  }
}


class InvariantKInstanceGenerator(algDefn: AlgDefn, autoDerivation: Boolean) extends FunctorKInstanceGenerator(algDefn) {
  import algDefn._
  import cls._

  case class EffParam(p: Term.Param, tpeArg: Type)

  class ParamParser(params: Seq[Term.Param]) {
    lazy val effParams: Seq[EffParam] =
      params.collect {
        case p @ Term.Param(_, _, Some(Type.Apply(Type.Name(`effectTypeName`), Seq(tpeArg))), _) =>
          EffParam(p, tpeArg)
      }

    lazy val newArgs: Seq[Term] =
      params.map {
        case p if effParams.exists(_.p == p) => q"gk(${Term.Name(p.name.value)})"
        case p => Term.Name(p.name.value)
      }

    lazy val newParams: Seq[Term.Param] =
      params.map { p =>
        effParams.find(_.p == p).fold(p) { effP =>
          effP.p.copy(decltpe = Some(Type.Apply(Type.Name("G"), Seq(effP.tpeArg))))
        }
      }
  }

  lazy val instanceDef: Seq[Defn] = {
    def newMethod(methodName: Term.Name,
                  params: Seq[Term.Param],
                  resultType: Type,
                  mTParams: Seq[Type.Param]): Defn.Def = {
      val pp = new ParamParser(params)

      if(pp.effParams.isEmpty) {
        val (newResultType, newImpl) = covariantTransform(resultType, q"af.$methodName(..${arguments(params)})" )

        q"""override def $methodName[..$mTParams](..$params): $newResultType = $newImpl"""
      } else {
        val (newResultType, newImpl) = covariantTransform(resultType, q"af.$methodName(..${pp.newArgs})" )
        q"""override def $methodName[..$mTParams](..${pp.newParams}): $newResultType = $newImpl """
      }
    }

    //todo: duplicated with newMethod
    def newMethodCurry(methodName: Term.Name,
                  params: Seq[Term.Param],
                  params2: Seq[Term.Param],
                  resultType: Type,
                  mTParams: Seq[Type.Param]): Defn.Def = {
      val pp = new ParamParser(params)
      val pp2= new ParamParser(params2)

      if(pp.effParams.isEmpty) {
        val (newResultType, newImpl) = covariantTransform(resultType, q"af.$methodName(..${arguments(params)})(..${arguments(params2)})" )

        q"""override def $methodName[..$mTParams](..$params)(..$params2): $newResultType = $newImpl"""
      } else {
        val (newResultType, newImpl) = covariantTransform(resultType, q"af.$methodName(..${pp.newArgs})(..${pp2.newArgs})" )
        q"""override def $methodName[..$mTParams](..${pp.newParams})(..${pp2.newParams}): $newResultType = $newImpl """
      }
    }


    val methods = fromExistingStats {
      case q"def $methodName[..$mTParams](..$params): $resultType" =>
        newMethod(methodName, params, resultType, mTParams)
      case st @ q"def $methodName[..$mTParams](..$params) = $impl" =>
        abort(s"cats.tagless.autoInvariantK does not support method without declared return type. as in $st ")
      case q"def $methodName[..$mTParams](..$params): $resultType = $impl"  =>
        newMethod(methodName, params, resultType.get, mTParams)

      case q"def $methodName[..$mTParams](..$params)(..$params2): $resultType" =>
        newMethodCurry(methodName, params, params2, resultType, mTParams)
      case st @ q"def $methodName[..$mTParams](..$params)(..$params2) = $impl" =>
        abort(s"cats.tagless.autoInvariantK does not support method without declared return type. as in $st ")
      case q"def $methodName[..$mTParams](..$params)(..$params2): $resultType = $impl"  =>
        newMethodCurry(methodName, params, params2, resultType.get, mTParams)

    } ++ defWithoutParams

    val from = Term.Name("af")
    //create a mapK method in the companion object with more precise refined type signature

    def newInstance(newTypeMembers: Seq[Defn.Type]): Term.New =
      q"""
         new ${Ctor.Ref.Name(name.value)}[..${tArgs("G")}] {
                  ..$newTypeMembers
                   ..$methods
                 }
       """
    Seq(
      q"""
        def imapK[F[_], G[_], ..$extraTParams]($from: ${newTypeSig("F")})(fk: _root_.cats.~>[F, G])(gk: _root_.cats.~>[G, F]): ${dependentRefinedTypeSig("G", from)} =
          ${newInstance(newTypeMember(from))}
      """,
      q"""
        implicit def ${Term.Name("invariantKFor" + name.value)}[..$extraTParams]: _root_.cats.tagless.InvariantK[$typeLambdaVaryingHigherKindedEffect] =
          _root_.cats.tagless.Derive.invariantK[$typeLambdaVaryingHigherKindedEffect]
       """,
      q"""
       object fullyRefined {
         implicit def ${Term.Name("invariantKForFullyRefined" + name.value)}[..$fullyRefinedTParams]: _root_.cats.tagless.InvariantK[$typeLambdaVaryingHigherKindedEffectFullyRefined] =
           _root_.cats.tagless.Derive.invariantK[$typeLambdaVaryingHigherKindedEffectFullyRefined]
         object autoDerive {
           @SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
           implicit def fromInvariantK[${effectType}, G[_], ..${fullyRefinedTParams}](
             implicit af: ${fullyRefinedTypeSig()},
             IK: _root_.cats.tagless.InvariantK[$typeLambdaVaryingHigherKindedEffectFullyRefined],
             fk: _root_.cats.~>[F, G],
             gk: _root_.cats.~>[G, F])
               : ${fullyRefinedTypeSig("G")}= IK.imapK(af)(fk)(gk)
         }
       }
      """)
  }

  lazy val autoDerivationDef  = if(autoDerivation)
      Seq(q"""
          object autoDerive {
            @SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
            implicit def autoDeriveFromInvariantK[${effectType}, G[_], ..${extraTParams}](
              implicit af: $name[..${tArgs()}],
              IK: _root_.cats.tagless.InvariantK[$typeLambdaVaryingHigherKindedEffect],
              fk: _root_.cats.~>[F, G],
              gk: _root_.cats.~>[G, F])
                : $name[..${tArgs("G")}] = IK.imapK(af)(fk)(gk)
          }
        """)
    else Nil

  lazy val newTypeDef = cls.copy(companion = companion.addStats(instanceDef ++ autoDerivationDef))
}
