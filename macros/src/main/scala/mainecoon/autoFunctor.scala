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
 * auto generates an instance of `cats.Functor`
 */
@compileTimeOnly("Cannot expand @autoFunctor")
class autoFunctor extends StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    enrichAlgebra(defn, higherKinded = false)(autoFunctor.functorInst)
  }
}

object autoFunctor {
  private[mainecoon] def functorInst(ad: AlgDefn): TypeDefinition = {
    import ad._
    import cls._

    val methods = templ.stats.map(_.collect {
      //abstract method with return type being effect type
      case q"def $methodName(..$params): ${Type.Name(`effectTypeName`)}" =>
        q"""def $methodName(..$params): TTarget =
           mapFunction(delegatee_.$methodName(..${arguments(params)}))"""
      //abstract method with other return type
      case q"def $methodName(..$params): $targetType" =>
        q"""def $methodName(..$params): $targetType =
           delegatee_.$methodName(..${arguments(params)})"""

    }).getOrElse(Nil)

    val instanceDef = Seq(q"""
      implicit def ${Term.Name("functorFor" + name.value)}[..$extraTParams]: _root_.cats.Functor[$typeLambdaVaryingEffect] =
        new _root_.cats.Functor[$typeLambdaVaryingEffect] {
          def map[T, TTarget](delegatee_ : $name[..${tArgs("T")}])(mapFunction: T => TTarget): $name[..${tArgs("TTarget")}] =
            new ${Ctor.Ref.Name(name.value)}[..${tArgs("TTarget")}] {
              ..$methods
            }
        }""")

    cls.copy(companion = cls.companion.addStats(instanceDef))

  }
}
