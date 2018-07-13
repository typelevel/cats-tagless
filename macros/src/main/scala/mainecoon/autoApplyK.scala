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
 * auto generates an instance of [[ApplyK]]
 */
@compileTimeOnly("Cannot expand @autoApplyK")
class autoApplyK(autoDerivation: Boolean) extends StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    val autoDerivation: Boolean = this match {
      case q"new $_(${Lit.Boolean(arg)})" => arg
      case q"new $_(autoDerivation = ${Lit.Boolean(arg)})" => arg
      case _  => true
    }
    enrichAlgebra(defn)(a => new ApplyKInstanceGenerator(a, autoDerivation).newDef)
  }
}


class ApplyKInstanceGenerator(algDefn: AlgDefn, autoDerivation: Boolean) extends CovariantKMethodsGenerator(algDefn) {
  import algDefn._
  import cls._
  lazy val instanceDef: Seq[Defn] = {
    Seq(
      companionMapKDef,
      q"""
        implicit def ${Term.Name("applyKFor" + name.value)}[..$extraTParams]: _root_.mainecoon.ApplyK[$typeLambdaVaryingHigherKindedEffect] =
          new _root_.mainecoon.ApplyK[$typeLambdaVaryingHigherKindedEffect] {
            $instanceMapKDef
            ${autoSemigroupalK.semigroupalKMethods(cls)}
          }
      """
    )
  }


  lazy val newDef: TypeDefinition = cls.copy(companion = cls.companion.addStats(instanceDef ++  {
    if(autoDerivation)
      Seq(autoDerivationDef)
    else Nil}))
}
