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

private[mainecoon] object Util {

  def enrichCompanion(defn: Any, f: (Template, Type.Name) => Seq[Defn]) : Block = {
    defn match {
      case Term.Block(
      Seq(t@ClassOrTrait(templ, name), companion: Defn.Object)) =>
        val newStat = f(templ, name)
        val templateStats: Seq[Stat] =
          newStat ++ companion.templ.stats.getOrElse(Nil)
        val newCompanion = companion.copy(
          templ = companion.templ.copy(stats = Some(templateStats)))
        Term.Block(Seq(t, newCompanion))
      case t@ClassOrTrait(templ, name) =>
        val newStat = f(templ, name)
        val companion = q"object ${Term.Name(name.value)} { ..$newStat }"
        Term.Block(Seq(t, companion))
      case t =>
        abort("@algebra must annotate a class or a trait/class.")
    }
  }

  object ClassOrTrait {
    def unapply(any: Defn): Option[(Template, Type.Name)] = any match {
      case t: Defn.Class => Some((t.templ, t.name))
      case t: Defn.Trait => Some((t.templ, t.name))
      case _             => None
    }
  }
}
