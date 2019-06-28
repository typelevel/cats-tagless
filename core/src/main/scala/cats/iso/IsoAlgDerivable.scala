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

import cats.hkinds._

trait IsoAlgDerivable[TC[_[_[_]]]] {
  def derive[A1[_[_]], A2[_[_]]](i: A1 <≈> A2): TC[A1] => TC[A2]
}

object IsoAlgDerivable extends IsoAlgDerivableInstances {
  def apply[TC[_[_[_]]]](implicit dev: IsoAlgDerivable[TC]): IsoAlgDerivable[TC] = dev
}

trait IsoAlgDerivableInstances extends IsoAlgDerivableInstances1 {

}

trait IsoAlgDerivableInstances1 {
  def isoAlgDeriveGeneral[TC[_[_[_]]]: InvariantHK]: IsoAlgDerivable[TC] = new IsoAlgDerivable[TC] {
    def derive[A1[_[_]], A2[_[_]]](i: <≈>[A1, A2]): TC[A1] => TC[A2] =
      tc => InvariantHK[TC].imapHK(tc)(i.to)(i.from)
  }


}
