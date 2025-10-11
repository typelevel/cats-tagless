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

import cats.*
import cats.data.*
import cats.syntax.all.*
import cats.tagless.optimize.*
import cats.tagless.optimize.syntax.all.*

object OptimizationExample extends App {

  // Define a simple key-value store algebra
  trait KVStore[F[_]] {
    def get(key: String): F[Option[String]]
    def put(key: String, value: String): F[Unit]
  }

  // Mock interpreter that logs operations
  val mockInterpreter = new KVStore[Eval] {
    def get(key: String) = Eval.later {
      println(s"  → Fetching key: $key")
      Some(s"Value for $key")
    }
    def put(key: String, value: String) = Eval.later {
      println(s"  → Storing key: $key with value: $value")
    }
  }

  // A program that has duplicate operations
  def program[F[_]: Applicative](store: KVStore[F]): F[List[String]] = {
    (store.get("Cats"), store.get("Dogs"), store.get("Cats"), store.get("Birds"))
      .mapN((c, d, c2, b) => List(c, d, c2, b).flatten)
  }

  val programInstance = new Program[KVStore, Applicative, List[String]] {
    def apply[F[_]: Applicative](alg: KVStore[F]): F[List[String]] = program(alg)
  }

  // Create an optimizer that eliminates duplicate gets
  def createOptimizer[F[_]: Monad]: Optimizer[KVStore, F] = new Optimizer[KVStore, F] {
    type M = Set[String]
    
    def monoidM = implicitly[Monoid[Set[String]]]
    def monadF = implicitly[Monad[F]]
    
    def extract = new KVStore[Const[Set[String], *]] {
      def get(key: String) = Const(Set(key))
      def put(key: String, value: String) = Const(Set.empty)
    }
    
    def rebuild(keys: Set[String], interp: KVStore[F]): F[KVStore[F]] = {
      println(s"  → Pre-fetching unique keys: ${keys.mkString(", ")}")
      
      keys.toList.traverse(key => 
        interp.get(key).map(_.map(value => key -> value))
      ).map { results =>
        val cache = results.flatten.toMap
        println(s"  → Built cache with ${cache.size} entries")
        
        new KVStore[F] {
          def get(key: String) = cache.get(key) match {
            case Some(value) => 
              println(s"  → Cache hit for key: $key")
              Option(value).pure[F]
            case None => 
              println(s"  → Cache miss for key: $key")
              interp.get(key)
          }
          def put(key: String, value: String) = interp.put(key, value)
        }
      }
    }
  }

  implicit val optimizer = createOptimizer[Eval]

  println("=== Without Optimization ===")
  val unoptimized = program(mockInterpreter).value
  println(s"Result: $unoptimized")
  
  println("\n=== With Optimization ===")
  val optimized = programInstance.optimize(mockInterpreter).value
  println(s"Result: $optimized")
  
  println("\n=== Put-Get Elimination Example ===")
  
  // More sophisticated optimizer that handles put-get elimination
  case class KVStoreInfo(queries: Set[String], cache: Map[String, String])
  
  def createPutGetEliminator[F[_]: Monad]: Optimizer[KVStore, F] = new Optimizer[KVStore, F] {
    type M = KVStoreInfo
    
    def monoidM = implicitly[Monoid[KVStoreInfo]]
    def monadF = implicitly[Monad[F]]
    
    def extract = new KVStore[Const[KVStoreInfo, *]] {
      def get(key: String) = Const(KVStoreInfo(Set(key), Map.empty))
      def put(key: String, value: String) = Const(KVStoreInfo(Set.empty, Map(key -> value)))
    }
    
    def rebuild(info: KVStoreInfo, interp: KVStore[F]): F[KVStore[F]] = {
      println(s"  → Analysis: queries=${info.queries.mkString(", ")}, cache=${info.cache}")
      
      val uncachedQueries = info.queries.filterNot(info.cache.contains)
      println(s"  → Need to fetch: ${uncachedQueries.mkString(", ")}")
      
      uncachedQueries.toList.traverse(key => 
        interp.get(key).map(_.map(value => key -> value))
      ).map { results =>
        val fetched = results.flatten.toMap
        val allCache = info.cache ++ fetched
        println(s"  → Final cache: ${allCache.mkString(", ")}")
        
        new KVStore[F] {
          def get(key: String) = allCache.get(key) match {
            case Some(value) => 
              println(s"  → Cache hit for key: $key")
              Option(value).pure[F]
            case None => 
              println(s"  → Cache miss for key: $key")
              interp.get(key)
          }
          def put(key: String, value: String) = interp.put(key, value)
        }
      }
    }
  }
  
  def putGetProgram[F[_]: Applicative](store: KVStore[F]): F[List[String]] = {
    (store.put("Cat", "Cat!"), store.get("Cat"), store.get("Dog"))
      .mapN((_, cat, dog) => List(cat, dog).flatten)
  }
  
  val putGetProgramInstance = new Program[KVStore, Applicative, List[String]] {
    def apply[F[_]: Applicative](alg: KVStore[F]): F[List[String]] = putGetProgram(alg)
  }
  
  implicit val putGetOptimizer = createPutGetEliminator[Eval]
  
  println("Without put-get elimination:")
  val unoptimizedPutGet = putGetProgram(mockInterpreter).value
  println(s"Result: $unoptimizedPutGet")
  
  println("\nWith put-get elimination:")
  val optimizedPutGet = putGetProgramInstance.optimize(mockInterpreter).value
  println(s"Result: $optimizedPutGet")
}
