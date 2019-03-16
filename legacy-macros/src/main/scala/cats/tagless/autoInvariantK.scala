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


class InvariantKInstanceGenerator(algDefn: AlgDefn, autoDerivation: Boolean) {
  import algDefn._
  import cls._

  lazy val instanceDef: Seq[Defn] = {
    val from = Term.Name("af")
    val fullyRefinedAlgebra = dependentRefinedTypeSig("G", from)
    //create a mapK method in the companion object with more precise refined type signature

    Seq(
      q"""
        def imapK[F[_], G[_], ..$extraTParams]($from: ${newTypeSig("F")})(fk: _root_.cats.~>[F, G])(gk: _root_.cats.~>[G, F]): $fullyRefinedAlgebra =
          _root_.cats.tagless.InvariantK[$typeLambdaVaryingHigherKindedEffect].imapK($from)(fk)(gk).asInstanceOf[$fullyRefinedAlgebra]
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
