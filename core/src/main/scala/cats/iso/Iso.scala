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

import cats.arrow.Compose

trait Iso[=>:[_, _], A, B] { self =>
  def to: A =>: B
  def from: B =>: A
  def flip: Iso[=>:, B, A] = new Iso[=>:, B, A] {
    val to = self.from
    val from = self.to
    override def flip = self
  }

  def %~(f: =>:[B, B])(implicit C: Compose[=>:]): =>:[A, A] =
    C.compose(from, C.compose(f, to))
}

object Iso {
  def apply[A, B](implicit iso: A <=> B): A <=> B = iso

  def reflexive[A]: A <=> A = new (A <=> A) {
    override final val to: A => A = a => a
    override final val from: A => A = a => a
    override final val flip: Iso[Function, A, A] = this
  }

  implicit def isoSetOps[A, B](iso: A <=> B): IsoSetOps[A, B] = IsoSetOps[A, B](iso)
  implicit def toFn  [A, B](iso: A <=> B): A => B = iso.to
  implicit def toOpFn[A, B](iso: A <=> B): B => A = iso.from

}