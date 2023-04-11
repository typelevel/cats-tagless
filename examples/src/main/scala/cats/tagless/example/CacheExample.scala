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

package cats.tagless.example

import cats.Invariant
import cats.tagless.{autoInvariant, finalAlg}

object CacheExample extends App {

  @finalAlg
  @autoInvariant
  trait CacheKV[K, V] {
    def get(key: K): Option[V]
    def put(key: K, v: V): Unit
  }

  private type Cache[A] = CacheKV[String, A]

  private val intCache = new Cache[Int] {
    import scala.collection.mutable.Map as MMap
    private val cache: MMap[String, Int] = MMap.empty[String, Int]

    override def get(key: String): Option[Int] = cache.get(key)

    override def put(key: String, v: Int): Unit = {
      cache.put(key, v)
      ()
    }
  }

  // magic is here: Invariant[Cache] instance is generated
  private val cacheI: Invariant[Cache] = Invariant[Cache]

  private def makeStrCache(inv: Invariant[Cache]): Cache[String] = inv.imap[Int, String](intCache)(_.toString)(_.toInt)

  // manual implementation of Invariant[Cache] instance:
  /*
  private val cacheIManual: Invariant[Cache] = new Invariant[Cache] {
    override def imap[A, B](fa: Cache[A])(f: A => B)(g: B => A): Cache[B] = new Cache[B] {
      override def get(key: String): Option[B] = fa.get(key).map(f)

      override def put(key: String, v: B): Unit = fa.put(key, g(v))
    }
  }
   */

  private val strCache: Cache[String] = makeStrCache(cacheI)
  strCache.put("aaa", "42")
  println(strCache.get("aaa"))

}
