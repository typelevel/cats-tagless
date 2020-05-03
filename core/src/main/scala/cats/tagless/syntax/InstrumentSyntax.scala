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

trait InstrumentSyntax {
  import InstrumentSyntax.InstrumentOps

  implicit def toInstrumentOps[Alg[_[_]], F[_]](af: Alg[F]): InstrumentOps[Alg, F] =
    new InstrumentOps(af)
}

object InstrumentSyntax {
  class InstrumentOps[Alg[_[_]], F[_]](private val af: Alg[F]) extends AnyVal {

    def instrument(implicit instrument: Instrument[Alg]): Alg[Instrumentation[F, *]] =
      instrument.instrument(af)

    def instrumentWith[Arg[_], Ret[_]](
      implicit instrument: Instrument.With[Alg, Arg, Ret]
    ): Alg[Instrumentation.With[F, Arg, Ret, *]] = instrument.instrumentWith(af)

    def instrumentWithBoth[G[_]](
      implicit instrument: Instrument.WithBoth[Alg, G]
    ): Alg[Instrumentation.WithBoth[F, G, *]] = instrument.instrumentWith(af)

    def instrumentWithArgs[G[_]](
      implicit instrument: Instrument.WithArgs[Alg, G]
    ): Alg[Instrumentation.WithArgs[F, G, *]] = instrument.instrumentWith(af)

    def instrumentWithRet[G[_]](
      implicit instrument: Instrument.WithRet[Alg, G]
    ): Alg[Instrumentation.WithRet[F, G, *]] = instrument.instrumentWith(af)
  }
}
