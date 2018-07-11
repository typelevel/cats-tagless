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
      case q"type $t  >: $lowBound <: $upBound" =>
        q"type $t = $definedAt.$t"
      case q"type $t[..$tparams] >: $lowBound <: $upBound" =>
        q"type $t[..$tparams] = $definedAt.$t[..${tparams.map(tp => Type.Name(tp.name.value))}]"
    }
  }

  lazy val newTypeMemberFullyRefined: Seq[Defn.Type] = {
    abstractTypeMembers.map { td =>
      val typeDefBody =
        if(td.tparams.nonEmpty)
          t"${Type.Name(td.name.value + "0")}[..${td.tparams.map(tp => Type.Name(tp.name.value))}]"
        else
          t"${Type.Name(td.name.value + "0")}"
      Defn.Type(td.mods, td.name, td.tparams, typeDefBody)
    }
  }

  def fromExistingStats[T <: Tree](pf: PartialFunction[Stat, T]): Seq[T] =
    cls.templ.stats.toList.flatMap(_.collect(pf))


  def fullyRefinedTypeSig(newEffectTypeName: String = effectTypeName): Type =
    refinedTypeSig(newEffectTypeName, newTypeMemberFullyRefined)

  def dependentRefinedTypeSig(newEffectTypeName: String = effectTypeName, dependent: Term.Name): Type =
    refinedTypeSig(newEffectTypeName, newTypeMember(dependent))

  def refinedTypeSig(newEffectTypeName: String, refinedTypes: Seq[Defn.Type]): Type = {
    if(abstractTypeMembers.isEmpty)
      newTypeSig(newEffectTypeName)
    else
      t"${cls.name}[..${tArgs(newEffectTypeName)}] { ..$refinedTypes } "
  }

  /**
   * new type signature with a different effect type name
   * @param newEffectTypeName
   */
  def newTypeSig(newEffectTypeName: String = effectTypeName): Type =
    t"${cls.name}[..${tArgs(newEffectTypeName)}]"

  lazy val extraTParams: Seq[Type.Param] = cls.tparams.filterNot(Set(effectType))

  lazy val fullyRefinedTParams: Seq[Type.Param] = extraTParams ++ refinedTParams

  lazy val refinedTParams: Seq[Type.Param] =
    newTypeMemberFullyRefined.map { defn =>
      val (n, s) = if(defn.tparams.nonEmpty) {
        val t"${Type.Name(name)}[..$tparams]" = defn.body
        (name, tparams.size)
      } else {
        val t"${Type.Name(name)}" = defn.body
        (name, 0)
      }

      Util.typeParam(n, s)
    }

  lazy val effectTypeArg: Type.Name = Type.Name(effectType.name.value)

  lazy val effectTypeName: String = effectType.name.value

  def tArgs(effTpeName: Type = effectTypeArg): Seq[Type] = cls.tparams.map {
    case `effectType` => effTpeName
    case tp =>  Type.Name(tp.name.value)
  }

  def tArgs(effTpeName: String): Seq[Type] = tArgs(Type.Name(effTpeName))

  lazy val typeLambdaVaryingHigherKindedEffect = t"({type λ[Ƒ[_]] = ${newTypeSig("Ƒ")}})#λ"
  lazy val typeLambdaVaryingHigherKindedEffectFullyRefined = t"({type λ[Ƒ[_]] = ${fullyRefinedTypeSig("Ƒ")}})#λ"

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

