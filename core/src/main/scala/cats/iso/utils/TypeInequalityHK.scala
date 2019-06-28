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

package cats.iso.utils

object TypeInequalityHK {

  // HK Type inequality
  type =:!:=[F[_], G[_]] = NotEqualHK1[F, G]
  type =:!!:=[F[_,_], G[_,_]] = NotEqualHK2[F, G]

  trait NotEqualHK1[F[_], G[_]]

  object NotEqualHK1 {
    implicit def neq[F[_], G[_]] : F NotEqualHK1 G = new NotEqualHK1[F, G] {}
    implicit def neqAmbig1[F[_]] : F NotEqualHK1 F = sys.error("Unexpected invocation")
    implicit def neqAmbig2[F[_]] : F NotEqualHK1 F = sys.error("Unexpected invocation")
  }

  trait NotEqualHK2[F[_,_], G[_,_]]

  object NotEqualHK2 {
    implicit def neq[F[_,_], G[_,_]] : F NotEqualHK2 G = new NotEqualHK2[F, G] {}
    implicit def neqAmbig1[F[_,_]] : F NotEqualHK2 F = sys.error("Unexpected invocation")
    implicit def neqAmbig2[F[_,_]] : F NotEqualHK2 F = sys.error("Unexpected invocation")
  }

}

