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

package cats.tagless.tests

import cats.{Applicative, Apply, Monad}
import cats.syntax.all._
import cats.tagless.optimize.Program

object Programs {

  private def putGetProgramF[F[_]: Applicative](F: KVStore[F]): F[List[String]] =
    List("Cat" -> "Cat!", "Dog" -> "Dog!").traverse(t => F.put(t._1, t._2)) *>
      List("Dog", "Bird", "Mouse", "Bird").traverse(F.get).map(_.flatten)

  val putGetProgram = new Program[KVStore, Applicative, List[String]] {
    def apply[F[_]: Applicative](alg: KVStore[F]): F[List[String]] = putGetProgramF(alg)
  }

  private def applicativeProgramF[F[_]: Apply](F: KVStore[F]): F[List[String]] =
    (F.get("Cats"), F.get("Dogs"), F.put("Mice", "mouse"), F.get("Cats"))
      .mapN((f, s, _, t) => List(f, s, t).flatten)

  val applicativeProgram = new Program[KVStore, Apply, List[String]] {
    def apply[F[_]: Apply](alg: KVStore[F]): F[List[String]] = applicativeProgramF(alg)
  }

  private def monadProgramF[F[_]: Monad](F: KVStore[F]): F[List[String]] = for {
    _ <- F.put("dog", "Daawwwwgg")
    _ <- F.get("dog")
    list <- putGetProgramF(F)
  } yield list

  val monadProgram = new Program[KVStore, Monad, List[String]] {
    def apply[F[_]: Monad](alg: KVStore[F]): F[List[String]] = monadProgramF(alg)
  }

}
