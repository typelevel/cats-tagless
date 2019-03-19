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

import cats.tagless.Util._

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.collection.immutable.Seq
import scala.meta._

/**
 * auto generates an instance of `cats.FlatMap`
 */
@compileTimeOnly("Cannot expand @autoFlatMap")
class autoFlatMap extends StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    enrichAlgebra(defn, higherKinded = false)(autoFlatMap.flatMapInst)
  }
}


object autoFlatMap {
  private[tagless] def flatMapInst(ad: AlgDefn): TypeDefinition = {
    import ad._
    import cls._

    val flatMapMethods = templ.stats.toList.flatMap(_.collect {
      // abstract method with return type being effect type
      case q"def $methodName[..$mTParams](...$params): ${Type.Name(`effectTypeName`)}" =>
        q"""def $methodName[..$mTParams](...$params): TTarget =
           flatMapFunction(delegatee_.$methodName(...${argumentss(params)})).$methodName(...${argumentss(params)})"""
      // concrete method with return type being effect type
      case q"def $methodName[..$mTParams](...$params): ${Some(targetType)} = $_" if targetType.toString == effectTypeName =>
        q"""override def $methodName[..$mTParams](...$params): TTarget =
           flatMapFunction(delegatee_.$methodName(...${argumentss(params)})).$methodName(...${argumentss(params)})"""
      // abstract method with other return type
      case q"def $methodName[..$mTParams](...$params): $targetType" =>
        q"""def $methodName[..$mTParams](...$params): $targetType =
           delegatee_.$methodName(...${argumentss(params)})"""
      // concrete method with other return type
      case q"def $methodName[..$mTParams](...$params): $targetType = $_" =>
        q"""override def $methodName[..$mTParams](...$params): $targetType =
           delegatee_.$methodName(...${argumentss(params)})"""
    })

    val tailRecMMethods = templ.stats.toList.flatMap(_.collect{
      // abstract method with return type being effect type
      case q"def $methodName[..$mTParams](...$params): ${Type.Name(`effectTypeName`)}" =>
        q"""def $methodName[..$mTParams](...$params): TTarget = {
              @_root_.scala.annotation.tailrec
              def step(current: TInit): TTarget =
                fn(current).$methodName(...${argumentss(params)}) match {
                  case Right(target) => target
                  case Left(next) => step(next)
                }

              step(init)
            }"""
      // concrete method with return type being effect type
      case q"def $methodName[..$mTParams](...$params): ${Some(targetType)} = $_" if targetType.toString == effectTypeName =>
        q"""override def $methodName[..$mTParams](...$params): TTarget = {
              @_root_.scala.annotation.tailrec
              def step(current: TInit): TTarget =
                fn(current).$methodName(...${argumentss(params)}) match {
                  case Right(target) => target
                  case Left(next) => step(next)
                }

              step(init)
            }"""
      // abstract method with other return type
      case q"def $methodName[..$mTParams](..$params): $targetType" =>
        q"""def $methodName[..$mTParams](..$params): $targetType =
           fn(init).$methodName(..${arguments(params)})"""
      // concrete method with other return type
      case q"def $methodName[..$mTParams](...$params): $targetType = $_" =>
        q"""override def $methodName[..$mTParams](...$params): $targetType =
           fn(init).$methodName(...${argumentss(params)})"""
    })

    val instanceDef = Seq(q"""
      implicit def ${Term.Name("monadFor" + name.value)}[..$extraTParams]: _root_.cats.FlatMap[$typeLambdaVaryingEffect] =
        _root_.cats.tagless.Derive.flatMap[$typeLambdaVaryingEffect]
    """)

    cls.copy(companion = cls.companion.addStats(instanceDef))

  }

  private[tagless] def mapMethods(template: Template, effectTypeName: String): List[Defn.Def] = {
    template.stats.toList.flatMap(_.collect {
      // abstract method with return type being effect type
      case q"def $methodName[..$mTParams](...$params): ${Type.Name(`effectTypeName`)}" =>
        q"""def $methodName[..$mTParams](...$params): TTarget =
           mapFunction(delegatee_.$methodName(...${argumentss(params)}))"""
      // concrete method with return type being effect type
      case q"def $methodName[..$mTParams](...$params): ${Some(targetType)} = $_" if targetType.toString == effectTypeName =>
        q"""override def $methodName[..$mTParams](...$params): TTarget =
           mapFunction(delegatee_.$methodName(...${argumentss(params)}))"""
      // abstract method with other return type
      case q"def $methodName[..$mTParams](...$params): $targetType" =>
        q"""def $methodName[..$mTParams](...$params): $targetType =
           delegatee_.$methodName(...${argumentss(params)})"""
      // concrete method with other return type
      case q"def $methodName[..$mTParams](...$params): $targetType = $_" =>
        q"""override def $methodName[..$mTParams](...$params): $targetType =
           delegatee_.$methodName(...${argumentss(params)})"""
    })
  }
}



