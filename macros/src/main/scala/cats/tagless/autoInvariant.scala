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
 * auto generates an instance of `cats.Functor`
 */
@compileTimeOnly("Cannot expand @autoFunctor")
class autoInvariant extends StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    enrichAlgebra(defn, higherKinded = false)(autoInvariant.invariantInst)
  }
}

object autoInvariant {
  private[tagless] def invariantInst(ad: AlgDefn): TypeDefinition = {
    import ad._
    import cls._

    class ParamParser(params: Seq[Term.Param]) {
      lazy val effParams: Seq[Term.Param] =
        params.collect {
          case p @ Term.Param(_, _, Some(Type.Name(`effectTypeName`)), _) => p
        }

      lazy val newArgs: Seq[Term] =
        params.map {
          case p if effParams.contains(p) => q"mapFunctionFrom(${Term.Name(p.name.value)})"
          case p => Term.Name(p.name.value)
        }

      lazy val newParams: Seq[Term.Param] =
        params.map { p =>
          effParams.find(_ == p).fold(p) { effP =>
            effP.copy(decltpe = Some(Type.Name("TTarget")))
          }
        }
    }

    val methods = templ.stats.toList.flatMap(_.collect {
      //abstract method with return type being effect type
      case q"def $methodName[..$mTParams](...$params): ${Type.Name(`effectTypeName`)}" =>
        val pps = params.map(new ParamParser(_))
        q"""def $methodName[..$mTParams](...${pps.map(_.newParams)}): TTarget =
           mapFunction(delegatee_.$methodName(...${pps.map(_.newArgs)}))"""

      //abstract method with other return type
      case q"def $methodName[..$mTParams](...$params): $targetType" =>
        val pps = params.map(new ParamParser(_))
        q"""def $methodName[..$mTParams](...${pps.map(_.newParams)}): $targetType =
           delegatee_.$methodName(...${pps.map(_.newArgs)})"""
    })

    val instanceDef = Seq(q"""
      implicit def ${Term.Name("invariantFor" + name.value)}[..$extraTParams]: _root_.cats.Invariant[$typeLambdaVaryingEffect] =
        new _root_.cats.Invariant[$typeLambdaVaryingEffect] {
          def imap[T, TTarget](delegatee_ : $name[..${tArgs("T")}])(mapFunction: T => TTarget)(mapFunctionFrom: TTarget => T): $name[..${tArgs("TTarget")}] =
            new ${Ctor.Ref.Name(name.value)}[..${tArgs("TTarget")}] {
              ..$methods
            }
        }""")

    cls.copy(companion = cls.companion.addStats(instanceDef))

  }
}
