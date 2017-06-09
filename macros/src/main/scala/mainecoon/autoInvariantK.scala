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
    def newMethod(methodName: Term.Name, params: Seq[Term.Param],  resultType: Type): Defn.Def = {
      val pp = new ParamParser(params)

      if(pp.effParams.isEmpty) {
        val (newResultType, newImpl) = covariantTransform(resultType, q"af.$methodName(..${arguments(params)})" )

        q"""override def $methodName(..$params): $newResultType = $newImpl"""
      } else {
        val (newResultType, newImpl) = covariantTransform(resultType, q"af.$methodName(..${pp.newArgs})" )
        q"""override def $methodName(..${pp.newParams}): $newResultType = $newImpl """
      }
    }


    val methods = fromExistingMethods {
      case q"def $methodName(..$params ): $resultType" =>
        newMethod(methodName, params, resultType)
      case st @ q"def $methodName(..$params ) = $impl" =>
        abort(s"mainecoon.autoInvariantK does not support method without declared return type. as in $st ")
      case q"def $methodName(..$params ): $resultType = $impl"  =>
        newMethod(methodName, params, resultType.get)

    } ++ defWithoutParams

    //create a mapK method in the companion object with more precise refined type signature
    Seq(q"""
      def imapK[F[_], G[_], ..$extraTParams](af: $name[..${tArgs()}])(fk: _root_.cats.~>[F, G])(gk: _root_.cats.~>[G, F]): ${typeSignature} =
        new ${Ctor.Ref.Name(name.value)}[..${tArgs("G")}] {
          ..${methods ++ newTypeMember}
        }""",
      q"""
        implicit def ${Term.Name("invariantKFor" + name.value)}[..$extraTParams]: _root_.mainecoon.InvariantK[$typeLambdaVaryingHigherKindedEffect] =
          new _root_.mainecoon.InvariantK[$typeLambdaVaryingHigherKindedEffect] {
            def imapK[F[_], G[_]](af: $name[..${tArgs("F")}])(fk: _root_.cats.~>[F, G])(gk: _root_.cats.~>[G, F]): $name[..${tArgs("G")}] =
              ${Term.Name(name.value)}.imapK(af)(fk)(gk)
          }
       """)
  }

  lazy val autoDerivationDef  = if(autoDerivation)
      Seq(q"""
          implicit def autoDeriveFromInvariantK[${effectType}, G[_], ..${extraTParams}](
            implicit af: $name[..${tArgs()}],
            IK: _root_.mainecoon.InvariantK[$typeLambdaVaryingHigherKindedEffect],
            fk: _root_.cats.~>[F, G],
            gk: _root_.cats.~>[G, F])
              : $name[..${tArgs("G")}] = IK.imapK(af)(fk)(gk)
        """)
    else Nil

  lazy val newTypeDef = cls.copy(companion = companion.addStats(instanceDef ++ autoDerivationDef))
}
