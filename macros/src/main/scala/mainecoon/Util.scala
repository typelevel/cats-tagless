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

import scala.meta.Term.Block
import scala.meta._
import scala.collection.immutable.Seq
import cats.implicits._

private[mainecoon] object Util {

  /**
   *
   * @param name e.g. "F" gives a `tparam"F[_]"`
   * @return
   */
  def typeParam(name: String, numOfTParams: Int = 1): Type.Param = {
    val tparams = Range(0, numOfTParams).toList.as(Type.Param(Nil, Name.Anonymous(), Nil, Type.Bounds(None, None), Nil, Nil))
    Type.Param(Nil, Type.Name(name), tparams, Type.Bounds(None, None), Nil, Nil)
  }

  def typeParam(typeDecl: Decl.Type): Type.Param =
    typeParam(typeDecl.name.value, typeDecl.tparams.size)


  def enrich(defn: Any)(f: TypeDefinition => TypeDefinition) : Block = {
    defn match {
      case TypeDefinition.FromAny(td) =>
        val enriched = f(td)
        Block(Seq(enriched.toDefn, enriched.companion))
      case t =>
        abort(s"$defn is not a class or trait.")
    }
  }

  def enrichAlgebra(defn: Any, higherKinded: Boolean = true)(f: AlgDefn => TypeDefinition): Block = {
    enrich(defn){ cls: TypeDefinition =>
      val ad = AlgDefn.from(cls, higherKinded).getOrElse(abort(s"${cls.name} does not have an higher-kinded type parameter, e.g. F[_]"))
      f(ad)
    }
  }

  implicit class CompanionExtension(val self: Defn.Object) extends AnyVal {
    import self._
    def addStats(stats: Seq[Stat]): Defn.Object =
      copy(templ = templ.copy(stats = Some(templ.stats.getOrElse(Nil) ++ stats)))

  }


  def arguments(params: Seq[Term.Param]) =
     params.map(p => Term.Name(p.name.value))
}
