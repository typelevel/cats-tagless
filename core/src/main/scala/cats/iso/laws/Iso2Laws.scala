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

package cats.iso.laws

//import cats.iso._
//import cats.hkinds._
//
//trait Iso2Laws[F[_], G[_]] {
//  def iso2: F <~> G
//
//  def iso2identityLeft[A](f: F[A]): IsEq[F[A]] =
//    iso2.from(iso2.to(f)) <-> f
//
//  def iso2identityRight[A](g: G[A]): IsEq[G[A]] =
//    iso2.to(iso2.from(g)) <-> g
//}

object Iso2Laws {
//  def apply[F[_], G[_], A](instance: F <~> G): Iso2Laws[F, G] = new Iso2Laws[F, G] {
//    override def iso2: F <~> G = instance
//  }
}
