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

import cats.iso.Iso3Instances._
import cats.iso.utils.TypeInequalityHK.=:!!:=

class Iso3Instances extends Iso3Instances1 {
  implicit def hasIsoRefl[F[_,_]]: HasIso[F, F] = HasIso(Iso3.reflexive[F])
  implicit def hasIso1[F[_,_], G[_,_]](implicit ev: F =:!!:= G, i: F <~~> G): HasIso[F, G] = HasIso(i)
  implicit def hasIso2[F[_,_], G[_,_]](implicit ev: F =:!!:= G, i: F <~~> G): HasIso[G, F] = HasIso(i.flip)
}

class Iso3Instances1 extends Iso3Instances2 {
}
class Iso3Instances2 {
}

object Iso3Instances {

  case class HasIso[F[_,_], G[_,_]](iso: F <~~> G)

}
