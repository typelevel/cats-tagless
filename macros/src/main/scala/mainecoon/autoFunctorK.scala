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
import autoFunctorK._
import Util._
import collection.immutable.Seq

/**
 * auto generates an instance of [[FunctorK]]
 */
class autoFunctorK extends StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    enrichCompanion(defn, functorKInst)
  }
}

object autoFunctorK {
  def functorKInst(cls: ClassOrTrait): Seq[Defn] = {
    import cls._
    val methods = templ.stats.map(_.collect {
      case q"def $methodName(..$params): $f[$resultType]" =>
        q"""def $methodName(..$params): G[$resultType] = fk(af.$methodName(..${arguments(params)}))"""

      case q"def $methodName(..$params): $resultType" =>
        q"""def $methodName(..$params): $resultType = af.$methodName(..${arguments(params)})"""

      case q"type $t" =>
        q"type $t = ${Type.Select(Term.Name("af"), Type.Name(t.value))}"

    }).getOrElse(Nil)

    val typeMember = methods.collect{ case tm: Defn.Type => tm }

    val typeSignature = if(typeMember.isEmpty) t"$name[G]" else t"$name[G] { ..$typeMember }"

    //create a mapK method in the companion object with more precise refined type signature
    Seq(q"""
      def mapK[F[_], G[_]](af: $name[F])(fk: _root_.cats.~>[F, G]): $typeSignature = new ${Ctor.Ref.Name(name.value)}[G] {
        ..$methods
      }""",
      q"""
      implicit def ${Term.Name("functorKFor" + name.value)}: _root_.mainecoon.FunctorK[$name] = new _root_.mainecoon.FunctorK[$name] {
        def mapK[F[_], G[_]](af: $name[F])(fk: _root_.cats.~>[F, G]): $name[G] = ${Term.Name(name.value)}.mapK(af)(fk)
      }
   """)
  }
}



