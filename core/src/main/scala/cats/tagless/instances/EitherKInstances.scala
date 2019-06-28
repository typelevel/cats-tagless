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

package cats.tagless.instances

import cats.data.{EitherK, Tuple2K}
import cats.tagless.ApplyK
import cats.~>

trait EitherKInstances {

  implicit def catsTaglessApplyKForEitherK[F[_], A]: ApplyK[EitherK[F, ?[_], A]] =
    eitherKInstance.asInstanceOf[ApplyK[EitherK[F, ?[_], A]]]

  private[this] val eitherKInstance: ApplyK[EitherK[Any, ?[_], Any]] = new ApplyK[EitherK[Any, ?[_], Any]] {
    def mapK[F[_], G[_]](af: EitherK[Any, F, Any])(fk: F ~> G) = af.mapK(fk)
    def productK[F[_], G[_]](af: EitherK[Any, F, Any], ag: EitherK[Any, G, Any]) =
      (af.run, ag.run) match {
        case (Left(x), _) => EitherK.leftc[Any, Tuple2K[F, G, ?], Any](x)
        case (_, Left(y)) => EitherK.leftc[Any, Tuple2K[F, G, ?], Any](y)
        case (Right(fa), Right(ga)) => EitherK.rightc[Any, Tuple2K[F, G, ?], Any](Tuple2K(fa, ga))
      }
  }
}
