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

import cats.tagless._
import cats.tagless.InvariantK.ops._
import cats.tagless.ContravariantK.ops._
import cats.tagless.FunctorK.ops._

trait Iso2Derivable[TC[_[_]]] {
  def derive[F[_], G[_]](i: F <~> G): TC[F] => TC[G]
}

object Iso2Derivable extends Iso2DerivableInstances {
  def apply[TC[_[_]]](implicit dev: Iso2Derivable[TC]): Iso2Derivable[TC] = dev
}

trait Iso2DerivableInstances extends Iso2DerivableInstances1 {

}

trait Iso2DerivableInstances1 extends Iso2DerivableInstances2 {
}
trait Iso2DerivableInstances2 extends Iso2DerivableInstances3 {
  // optional, just for a shortcutted instantiation,
  //   if deleted it's purpose would be served by the iso2DeriveInvariant below function anyway
  implicit def iso2DeriveContravariant[TC[_[_]]: ContravariantK]: Iso2Derivable[TC] = new Iso2Derivable[TC] {
    def derive[F[_], G[_]](i: F <~> G): TC[F] => TC[G] = _.contramapK(i.from)
  }
  // optional, just for a shortcutted instantiation,
  //   if deleted it's purpose would be served by the iso2DeriveInvariant below function anyway
  implicit def iso2DeriveCovariant[TC[_[_]]: FunctorK]: Iso2Derivable[TC] = new Iso2Derivable[TC] {
    def derive[F[_], G[_]](i: <~>[F, G]): TC[F] => TC[G] = _.mapK(i.to)
  }
}
trait Iso2DerivableInstances3 {
  implicit def iso2DeriveInvariant[TC[_[_]]: InvariantK]: Iso2Derivable[TC] = new Iso2Derivable[TC] {
    def derive[F[_], G[_]](i: <~>[F, G]): TC[F] => TC[G] = _.imapK(i.to)(i.from)
  }
}
