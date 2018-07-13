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
import autoSemigroupalK._
import Util._

import collection.immutable.Seq

/**
 * auto generates an instance of [[SemigroupalK]]
 */
@compileTimeOnly("Cannot expand @autoSemigroupalK")
class autoSemigroupalK extends StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    enrich(defn)(semigroupalKInst)
  }
}

object autoSemigroupalK {
  private[mainecoon] def semigroupalKInst(cls: TypeDefinition): TypeDefinition = {
    import cls._

    val instanceDef = Seq(q"""
      implicit def ${Term.Name("semigroupalKFor" + name.value)}: _root_.mainecoon.SemigroupalK[$name] =
        new _root_.mainecoon.SemigroupalK[$name] {
          ${semigroupalKMethods(cls)}
        }""")

    cls.copy(companion = cls.companion.addStats(instanceDef))
  }

  private[mainecoon] def semigroupalKMethods(cls: TypeDefinition): Defn = {
    import cls._

    val methods = templ.stats.map(_.map {
      case q"def $methodName[..$mTParams](..$params): $f[$resultType]" =>

        q"""def $methodName[..$mTParams](..$params): _root_.cats.data.Tuple2K[F, G, $resultType] =
           _root_.cats.data.Tuple2K(af.$methodName(..${arguments(params)}), ag.$methodName(..${arguments(params)}))"""
      case q"def $methodName[..$mTParams](..$params)(..$params2): $f[$resultType]" =>

        q"""def $methodName[..$mTParams](..$params)(..$params2): _root_.cats.data.Tuple2K[F, G, $resultType] =
           _root_.cats.data.Tuple2K(af.$methodName(..${arguments(params)})(..${arguments(params2)}), ag.$methodName(..${arguments(params)})(..${arguments(params2)}))"""
      case st => abort(s"autoSemigroupalK does not support algebra with such statement: $st")
    }).getOrElse(Nil)

    q"""
      def productK[F[_], G[_]](af: $name[F], ag: $name[G]): $name[({type λ[A]=_root_.cats.data.Tuple2K[F, G, A]})#λ] =
        new ${Ctor.Ref.Name(name.value)}[({type λ[A]=_root_.cats.data.Tuple2K[F, G, A]})#λ] {
          ..$methods
        }
    """


  }
}



