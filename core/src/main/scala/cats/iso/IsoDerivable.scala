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

import cats._
import cats.implicits._

trait IsoDerivable[TC[_]] {
  def derive[A, B](i: A <=> B)(TC: TC[A]): TC[B]
}

object IsoDerivable extends IsoDerivableInstances

trait IsoDerivableInstances extends IsoDerivableInstances1 {

}
trait IsoDerivableInstances1 extends IsoDerivableInstances2 {
}
trait IsoDerivableInstances2 extends IsoDerivableInstances3 {
  // optional, just for a shortcutted instantiation,
  //   if deleted it's purpose would be served by the isoDeriveInvariant below function anyway
  implicit def isoDeriveContravariant[TC[_]: Contravariant]: IsoDerivable[TC] = new IsoDerivable[TC] {
    override def derive[A, B](i: <=>[A, B])(TC: TC[A]): TC[B] = TC.contramap(i.from)
  }
  // optional, just for a shortcutted instantiation,
  //   if deleted it's purpose would be served by the isoDeriveInvariant below function anyway
  implicit def isoDeriveCovariant[TC[_]: Functor]: IsoDerivable[TC] = new IsoDerivable[TC] {
    override def derive[A, B](i: <=>[A, B])(TC: TC[A]): TC[B] = TC.map(i.to)
  }
}
trait IsoDerivableInstances3 {
  implicit def isoDeriveInvariant[TC[_]: Invariant]: IsoDerivable[TC] = new IsoDerivable[TC] {
    override def derive[A, B](i: A <=> B)(TC: TC[A]): TC[B] = TC.imap(i.to)(i.from)
  }
}
