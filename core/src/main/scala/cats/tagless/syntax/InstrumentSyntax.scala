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

import cats.tagless.diagnosis.{Instrument, Instrumentation}

trait InstrumentSyntax extends Instrument.ToInstrumentOps {
  import InstrumentSyntax.InstrumentConstrainedOps

  implicit def toInstrumentConstrainedOps[Alg[_[_]], F[_]](af: Alg[F]): InstrumentConstrainedOps[Alg, F] =
    new InstrumentConstrainedOps(af)
}

object InstrumentSyntax {
  class InstrumentConstrainedOps[Alg[_[_]], F[_]](private val af: Alg[F]) extends AnyVal {
    def instrumentWith[G[_]](
      implicit instrument: Instrument.With[Alg, G]
    ): Alg[Instrumentation.With[F, G, *]] = instrument.instrumentWith(af)
  }
}
