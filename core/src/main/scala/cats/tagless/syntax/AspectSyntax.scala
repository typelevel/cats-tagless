/*
 * Copyright 2019 cats-tagless maintainers
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

package cats.tagless.syntax

import cats.tagless.aop.Aspect

trait AspectSyntax {
  implicit def toAspectOps[Alg[_[_]], F[_]](af: Alg[F]): AspectSyntax.AspectOps[Alg, F] =
    new AspectSyntax.AspectOps(af)
}

object AspectSyntax {

  class AspectOps[Alg[_[_]], F[_]](private val af: Alg[F]) extends AnyVal {

    def weave[Dom[_], Cod[_]](implicit aspect: Aspect[Alg, Dom, Cod]): Alg[Aspect.Weave[F, Dom, Cod, *]] =
      aspect.weave(af)

    def weaveDomain[G[_]](implicit aspect: Aspect.Domain[Alg, G]): Alg[Aspect.Weave.Domain[F, G, *]] =
      aspect.weave(af)

    def weaveCodomain[G[_]](implicit aspect: Aspect.Codomain[Alg, G]): Alg[Aspect.Weave.Codomain[F, G, *]] =
      aspect.weave(af)

    def weaveFunction[G[_]](implicit aspect: Aspect.Function[Alg, G]): Alg[Aspect.Weave.Function[F, G, *]] =
      aspect.weave(af)
  }
}
