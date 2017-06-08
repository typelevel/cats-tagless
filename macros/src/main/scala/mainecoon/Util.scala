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

  /**
   *
   * @param name e.g. "F" gives a `tparam"F[_]"`
   * @return
   */
  def highKindedTypeParam(name: String): Type.Param =
    Type.Param(Nil, Type.Name(name), Seq(Type.Param(Nil, Name.Anonymous(), Nil, Type.Bounds(None, None), Nil, Nil)), Type.Bounds(None, None), Nil, Nil)

  def enrichCompanion(defn: Any)(f: TypeDefinition => TypeDefinition) : Block = {
    defn match {
      case TypeDefinition.FromAny(td) =>
        val enriched = f(td)
        Block(Seq(enriched.defn, enriched.companion))
      case t =>
        abort(s"$defn is not a class or trait.")
    }
  }

  def enrichAlgebra(defn: Any, higherKinded: Boolean = true)(f: AlgDefn => TypeDefinition): Block = {
    enrichCompanion(defn){ cls: TypeDefinition =>
      val ad = AlgDefn.from(cls, higherKinded).getOrElse(abort(s"${cls.name} does not have an higher-kinded type parameter, e.g. F[_]"))
      f(ad)
    }
  }

  implicit class CompanionExtension(val self: Defn.Object) extends AnyVal {
    import self._
    def addStats(stats: Seq[Stat]): Defn.Object =
      copy(templ = templ.copy(stats = Some(templ.stats.getOrElse(Nil) ++ stats)))

  }


  case class AlgDefn(cls: TypeDefinition, effectType: Type.Param){

    val extraTParams = cls.tparams.filterNot(Set(effectType))

    val effectTypeArg: Type.Name = Type.Name(effectType.name.value)

    val effectTypeName: String = effectType.name.value

    def tArgs(effTpeName: Type.Name = effectTypeArg): Seq[Type.Name] = cls.tparams.map {
      case `effectType` => effTpeName
      case tp =>  Type.Name(tp.name.value)
    }

    def tArgs(effTpeName: String): Seq[Type.Name] = tArgs(Type.Name(effTpeName))

    lazy val typeLambdaVaryingHigherKindedEffect = t"({type λ[Ƒ[_]] = ${cls.name}[..${tArgs("Ƒ")}]})#λ"
    lazy val typeLambdaVaryingEffect = t"({type λ[T] = ${cls.name}[..${tArgs("T")}]})#λ"
  }

  object AlgDefn {
    def from(cls: TypeDefinition, higherKinded: Boolean = true): Option[AlgDefn] = {
      { if (higherKinded)
          cls.tparams.collectFirst {
            case tp: Type.Param if tp.tparams.nonEmpty => tp
          }
        else
          cls.tparams.lastOption
      }.map(
        AlgDefn(cls, _)
      )
    }
  }

  case class TypeDefinition(name: Type.Name, templ: Template, tparams: Seq[Type.Param], companion: Defn.Object, defn: Defn)

  object TypeDefinition {

    object FromAny {
      def unapply(defn: Any): Option[TypeDefinition] = {
        def createCompanion(name: Type.Name): Defn.Object = q"object ${Term.Name(name.value)} { }"
        defn match {

          case Term.Block(Seq(t: Defn.Class, companion: Defn.Object)) =>
              Some(TypeDefinition(t.name, t.templ, t.tparams, companion, t))

          case Term.Block(Seq(t: Defn.Trait, companion: Defn.Object)) =>
            Some(TypeDefinition(t.name, t.templ, t.tparams, companion, t))

          case t: Defn.Class =>
            Some(TypeDefinition(t.name, t.templ, t.tparams, createCompanion(t.name), t))

          case t: Defn.Trait =>
            Some(TypeDefinition(t.name, t.templ, t.tparams, createCompanion(t.name), t))
        }

      }
    }
  }

  def arguments(params: Seq[Term.Param]) =
     params.map(p => Term.Name(p.name.value))
}
