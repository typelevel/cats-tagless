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

import scala.annotation.StaticAnnotation
import scala.meta._
import autoCartesianK._
import Util._
import collection.immutable.Seq

/**
 * auto generates an instance of [[CartesianK]]
 */
class autoCartesianK extends StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    enrichCompanion(defn)(cartesianKInst)
  }
}

object autoCartesianK {
  def cartesianKInst(cls: ClassOrTrait): Seq[Defn] = {

    import cls._

    val methods = templ.stats.map(_.map {
      case q"def $methodName(..$params): $f[$resultType]" =>

        q"""def $methodName(..$params): _root_.cats.data.Prod[F, G, $resultType] =
           _root_.cats.data.Prod(af.$methodName(..${arguments(params)}), ag.$methodName(..${arguments(params)}))"""
      case st => abort(s"autoCartesianK does not support algebra with such statement: $st")
    }).getOrElse(Nil)

    Seq(q"""
      implicit def ${Term.Name("cartesianKFor" + name.value)}: _root_.mainecoon.CartesianK[$name] =
        new _root_.mainecoon.CartesianK[$name] {
          def productK[F[_], G[_]](af: $name[F], ag: $name[G]): $name[({type λ[A]=_root_.cats.data.Prod[F, G, A]})#λ] =
            new ${Ctor.Ref.Name(name.value)}[({type λ[A]=_root_.cats.data.Prod[F, G, A]})#λ] {
              ..$methods
            }
      }
   """)
  }
}



