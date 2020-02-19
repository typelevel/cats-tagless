/*
 * Copyright 2020 cats-tagless maintainers
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

package cats.tagless

import simulacrum.typeclass
import alleycats.EmptyK

/**
 * Given an EmptyK, a value-polymorphic but effect-constant constructor, 
 * we can build an instance of the handler that, on every operation, gives us that. 
**/ 
@typeclass
trait NihilistK[A[_[_]]] {
  def exNihiloK[F[_]](emptyK: EmptyK[F]): A[F]
}
