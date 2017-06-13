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

import scala.collection.immutable.Seq
import scala.meta.{Defn, Stat, Template, Term, Tree, Type}
import scala.meta._

case class AlgDefn(cls: TypeDefinition, effectType: Type.Param){
  lazy val abstractTypeMembers: Seq[Decl.Type] = {
    fromExistingStats {
      case dt : Decl.Type => dt
    }
  }

  def newTypeMember(definedAt: Term.Name): Seq[Defn.Type] = {
    abstractTypeMembers.map {
      case q"type $t" =>
        q"type $t = $definedAt.$t"
      case q"type $t[..$tparams]" =>
        q"type $t[..$tparams] = $definedAt.$t[..${tparams.map(tp => Type.Name(tp.name.value))}]"
    }
  }

  def fromExistingStats[T <: Tree](pf: PartialFunction[Stat, T]): Seq[T] =
    cls.templ.stats.toList.flatMap(_.collect(pf))

  def refinedFullTypeSig(newEffectTypeName: String, fromInstance: Term.Name): Type = {
    import cls.name
    if(abstractTypeMembers.isEmpty)
      t"$name[..${tArgs(newEffectTypeName)}]"
    else
      t"$name[..${tArgs(newEffectTypeName)}] { ..${newTypeMember(fromInstance)} }"
  }

  lazy val extraTParams: Seq[Type.Param] = cls.tparams.filterNot(Set(effectType))

  lazy val refinedTParams: Seq[Type.Param] = abstractTypeMembers.map(Util.typeParam)

  lazy val effectTypeArg: Type.Name = Type.Name(effectType.name.value)

  lazy val effectTypeName: String = effectType.name.value

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

case class TypeDefinition(name: Type.Name, templ: Template, tparams: Seq[Type.Param], companion: Defn.Object, defn: Defn) {
  def toDefn: Defn = defn match {
    case t: Defn.Class => t.copy(templ = templ)
    case t: Defn.Trait => t.copy(templ = templ)
  }
}

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

