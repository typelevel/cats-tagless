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

import cats.arrow._
import cats.{Bifoldable, Bifunctor, Bitraverse}
import cats.iso.cats_classes.iso3._

trait Iso3Derivation[TC[_[_,_]]] {
  def get[F[_,_], G[_,_]: TC](i: F <~~> G): TC[F]
}
trait Iso3Derivable[TC[_[_,_]]] {
  def derive[F[_,_], G[_,_]](i: F <~~> G): TC[F] => TC[G]
}
object Iso3Derivable {
  def apply[TC[_[_,_]]](implicit dev: Iso3Derivable[TC]): Iso3Derivable[TC] = dev
}

object Iso3Derivation extends Iso3DerivationInstances

trait Iso3DerivationInstances {
  implicit def iso3Derivable[TC[_[_,_]]](i3d: Iso3Derivation[TC]): Iso3Derivable[TC] = new Iso3Derivable[TC] {
    def derive[F[_, _], G[_, _]](i: <~~>[F, G]): TC[F] => TC[G] = tcf => i3d.get(i.flip)(tcf)
  }

  implicit val iso3DeriveCompose: Iso3Derivation[Compose] = new Iso3Derivation[Compose] {
    final override def get[F[_,_], G[_,_] : Compose](i: <~~>[F, G]): Compose[F] =
      new Iso3Compose[F, G] { val G = Compose[G]; val iso = i }
  }
  implicit val iso3DeriveCategory: Iso3Derivation[Category] = new Iso3Derivation[Category] {
    override def get[F[_,_], G[_,_] : Category](i: <~~>[F, G]): Category[F] =
      new Iso3Category[F, G] { val G = Category[G]; val iso = i }
  }
  implicit val iso3DeriveChoice: Iso3Derivation[Choice] = new Iso3Derivation[Choice] {
    final override def get[F[_,_], G[_,_] : Choice](i: <~~>[F, G]): Choice[F] =
      new Iso3Choice[F, G] { val G = Choice[G]; val iso = i }
  }  
  implicit val iso3DeriveProfunctor: Iso3Derivation[Profunctor] = new Iso3Derivation[Profunctor] {
    final override def get[F[_, _], G[_, _] : Profunctor](i: <~~>[F, G]): Profunctor[F] =
      new Iso3Profunctor[F, G] { val G = Profunctor[G]; val iso = i }
  }
  implicit val iso3DeriveStrong: Iso3Derivation[Strong] = new Iso3Derivation[Strong] {
    final override def get[F[_,_], G[_,_] : Strong](i: <~~>[F, G]): Strong[F] =
      new Iso3Strong[F, G] { val G = Strong[G]; val iso = i }
  }
  implicit val iso3DeriveArrow: Iso3Derivation[Arrow] = new Iso3Derivation[Arrow] {
    final override def get[F[_,_], G[_,_] : Arrow](i: <~~>[F, G]): Arrow[F] =
      new Iso3Arrow[F, G] { val G = Arrow[G]; val iso = i }
  }
  implicit val iso3DeriveArrowChoice: Iso3Derivation[ArrowChoice] = new Iso3Derivation[ArrowChoice] {
    final override def get[F[_,_], G[_,_] : ArrowChoice](i: <~~>[F, G]): ArrowChoice[F] =
      new Iso3ArrowChoice[F, G] { val G = ArrowChoice[G]; val iso = i }
  }
  implicit val iso3DeriveCommutativeArrow: Iso3Derivation[CommutativeArrow] = new Iso3Derivation[CommutativeArrow] {
    final override def get[F[_,_], G[_,_] : CommutativeArrow](i: <~~>[F, G]): CommutativeArrow[F] =
      new Iso3CommutativeArrow[F, G] { val G = CommutativeArrow[G]; val iso = i }
  }
  implicit val iso3DeriveBifoldable: Iso3Derivation[Bifoldable] = new Iso3Derivation[Bifoldable] {
    final override def get[F[_,_], G[_,_] : Bifoldable](i: <~~>[F, G]): Bifoldable[F] =
      new Iso3Bifoldable[F, G] { val G = Bifoldable[G]; val natTrans = i.to }
  }
  implicit val iso3DeriveBifunctor: Iso3Derivation[Bifunctor] = new Iso3Derivation[Bifunctor] {
    final override def get[F[_,_], G[_,_] : Bifunctor](i: <~~>[F, G]): Bifunctor[F] =
      new Iso3Bifunctor[F, G] { val G = Bifunctor[G]; val iso = i }
  }
  implicit val iso3DeriveBitraverse: Iso3Derivation[Bitraverse] = new Iso3Derivation[Bitraverse] {
    final override def get[F[_,_], G[_,_] : Bitraverse](i: <~~>[F, G]): Bitraverse[F] =
      new Iso3Bitraverse[F, G] { val G = Bitraverse[G]; val iso = i }
  }
}
