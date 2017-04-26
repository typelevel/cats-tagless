package mainecoon

import scala.annotation.StaticAnnotation
import scala.meta._
import scala.collection.immutable.Seq
import autoTransform._

class autoTransform extends StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    defn match {
      case Term.Block(
        Seq(t @ ClassOrTrait(templ, name), companion: Defn.Object)) =>
        val newMethod = tranformKInst(templ, name)
        val templateStats: Seq[Stat] =
          newMethod +: companion.templ.stats.getOrElse(Nil)
        val newCompanion = companion.copy(
          templ = companion.templ.copy(stats = Some(templateStats)))
        Term.Block(Seq(t, newCompanion))
      case t @ ClassOrTrait(templ, name) =>
        val newMethod = tranformKInst(templ, name)
        val companion = q"object ${Term.Name(name.value)} { $newMethod }"
        Term.Block(Seq(t, companion))
      case t =>
        abort("@algebra must annotate a class or a trait/class.")
    }

  }
}

object autoTransform {
  def tranformKInst(templ: Template, name: Type.Name): Defn = {

    val methods = templ.stats.map(_.collect {
      case q"def $methodName(..$params): $f[$resultType]" =>
        q"""def $methodName(..$params): G[$resultType] = fk(af.$methodName(..${params.map(p => Term.Name(p.name.value))}))"""
    }).getOrElse(Nil)

    q"""
      implicit def ${Term.Name("transformKFor" + name.value)}: _root_.mainecoon.TransformK[$name] = new _root_.mainecoon.TransformK[$name] {
        def mapK[F[_], G[_]](af: $name[F])(fk: _root_.cats.~>[F, G]): $name[G] = new ${Ctor.Ref.Name(name.value)}[G] {
          ..$methods
        }
      }
   """
  }
}

object ClassOrTrait {
  def unapply(any: Defn): Option[(Template, Type.Name)] = any match {
    case t: Defn.Class => Some((t.templ, t.name))
    case t: Defn.Trait => Some((t.templ, t.name))
    case _             => None
  }
}

