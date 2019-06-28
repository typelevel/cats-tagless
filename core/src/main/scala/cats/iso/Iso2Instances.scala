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

package cats.iso

import cats.iso.Iso2Instances._
import cats.iso.utils.TypeInequalityHK.=:!:=


trait Iso2Instances extends Iso2Instances1 {
  implicit def hasIsoRefl[F[_]]: HasIso[F, F] = HasIso(Iso2.reflexive)
  implicit def hasIso1[F[_], G[_]](implicit ev: F =:!:= G, i: F <~> G): HasIso[F, G] = HasIso(i)
  implicit def hasIso2[F[_], G[_]](implicit ev: F =:!:= G, i: F <~> G): HasIso[G, F] = HasIso(i.flip)
  implicit def hasTC[TC[_[_]], F[_]](implicit TC: TC[F]): HasTypeClass[TC, F] = HasTypeClass(TC)
}
trait Iso2Instances1 {
}

object Iso2Instances {

  case class HasIso[F[_], G[_]](iso: F <~> G)
  case class HasTypeClass[TC[_[_]], F[_]](typeClass: TC[F])

}
