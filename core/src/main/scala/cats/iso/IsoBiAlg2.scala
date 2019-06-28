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

trait IsoBiAlg2[|=>[_[_[_,_],_,_], _[_[_,_],_,_]], Alg1[_[_,_],_,_], Alg2[_[_,_],_,_]] { self =>
  def to: Alg1 |=> Alg2
  def from: Alg2 |=> Alg1
  def flip: IsoBiAlg2[|=>, Alg2, Alg1] = new IsoBiAlg2[|=>, Alg2, Alg1] {
    val to = self.from
    val from = self.to
    override def flip: IsoBiAlg2[|=>, Alg1, Alg2] = self
  }


}

object IsoBiAlg2 {
}
