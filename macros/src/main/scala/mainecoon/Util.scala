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

import mainecoon.Util.ClassOrTrait.FromDefn

import scala.meta.Term.Block
import scala.meta._
import scala.collection.immutable.Seq

private[mainecoon] object Util {

  def enrichCompanion(defn: Any)(f: ClassOrTrait => Seq[Defn]) : Block = {
    defn match {
      case Term.Block(
      Seq(t@FromDefn(cls), companion: Defn.Object)) =>
        val newStat = f(cls)
        val templateStats: Seq[Stat] =
          newStat ++ companion.templ.stats.getOrElse(Nil)
        val newCompanion = companion.copy(
          templ = companion.templ.copy(stats = Some(templateStats)))
        Term.Block(Seq(t, newCompanion))
      case t@FromDefn(cls) =>
        val newStat = f(cls)
        val companion = q"object ${Term.Name(cls.name.value)} { ..$newStat }"
        Term.Block(Seq(t, companion))
      case t =>
        abort("@algebra must annotate a class or a trait/class.")
    }
  }

  def enrichAlg(defn: Any)(f: AlgDefn => Seq[Defn]): Block = {
    enrichCompanion(defn){ cls: ClassOrTrait =>
      val ad = AlgDefn.from(cls).getOrElse(abort(s"${cls.name} does not have an higher-kinded type parameter, e.g. F[_]"))
      f(ad)
    }
  }

  case class ClassOrTrait(name: Type.Name, templ: Template, tparams: Seq[Type.Param])

  case class AlgDefn(cls: ClassOrTrait, effectType: Type.Param, extraTParams: Seq[Type.Param]){
    val tArgs = cls.tparams.map(tp => Type.Name(tp.name.value))
    val extraTArgs = extraTParams.map(tp => Type.Name(tp.name.value))
    val effectTypeArg = Type.Name(effectType.name.value)

    val typeLambdaForFunctorK = t"({type λ[Ƒ[_]] = ${cls.name}[Ƒ, ..${extraTArgs}]})#λ"
  }

  object AlgDefn {
    def from(cls: ClassOrTrait): Option[AlgDefn] = {
      cls.tparams.collectFirst {
        case tp: Type.Param if tp.tparams.nonEmpty => tp
      }.map { effectType =>
        val extraTParams = cls.tparams.filterNot(Set(effectType))
        AlgDefn(cls, effectType, extraTParams)
      }
    }
  }

  object ClassOrTrait {
    object FromDefn {
      def unapply(any: Defn): Option[ClassOrTrait] = any match {
        case t: Defn.Class => Some(ClassOrTrait(t.name, t.templ, t.tparams))
        case t: Defn.Trait => Some(ClassOrTrait(t.name, t.templ, t.tparams))
        case _             => None
      }
    }

  }

  def arguments(params: Seq[Term.Param]) =
     params.map(p => Term.Name(p.name.value))
}
