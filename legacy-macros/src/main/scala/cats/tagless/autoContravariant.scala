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
 * auto generates an instance of `cats.Contravariant`
 */
@compileTimeOnly("Cannot expand @autoContravariant")
class autoContravariant extends StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    enrichAlgebra(defn, higherKinded = false)(autoContravariant.contravariantInst)
  }
}

object autoContravariant {
  private[tagless] def contravariantInst(ad: AlgDefn): TypeDefinition = {
    import ad._
    import cls._

    val instanceDef = Seq(q"""
      implicit def ${Term.Name("contravariantFor" + name.value)}[..$extraTParams]: _root_.cats.Contravariant[$typeLambdaVaryingEffect] =
        _root_.cats.tagless.Derive.contravariant[$typeLambdaVaryingEffect]
    """)

    cls.copy(companion = cls.companion.addStats(instanceDef))

  }
}
