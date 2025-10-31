---
layout: page
title:  Optimization
section: optimization
position: 5
---

# Optimization

The `cats.tagless.optimize` package provides powerful tools for analyzing and optimizing tagless final programs. These optimizations can significantly improve performance by eliminating redundant operations, batching requests, and applying other program transformations based on static analysis.

## Overview

The optimization package is inspired by techniques originally developed in the [Sphynx](https://github.com/typelevel/sphynx) library and is based on the principles described in the blog post ["Optimizing Tagless Final – Saying farewell to Free"](https://typelevel.org/blog/2017/12/27/optimizing-final-tagless.html) by Luka Jacobowitz.

The core idea is to perform static analysis on tagless final programs by interpreting them into structures that can collect information about the operations being performed, then use this information to create optimized versions of the programs.

## Key Concepts

### Program Analysis

The optimization process works in two phases:

1. **Analysis Phase**: The program is interpreted using a special interpreter that collects static information about the operations (e.g., which keys are accessed, what values are stored).

2. **Optimization Phase**: Based on the collected information, an optimized interpreter is created that can eliminate redundant operations, batch requests, or apply other optimizations.

### Type Classes

The optimization package provides three main type classes:

- **`SemigroupalOptimizer`**: The most general optimizer that works with `Apply` constraints
- **`Optimizer`**: Extends `SemigroupalOptimizer` and works with `Applicative` constraints  
- **`MonadOptimizer`**: The most powerful optimizer that works with `Monad` constraints and can handle stateful optimizations

## Basic Usage

Let's start with a simple example using a key-value store algebra:

```scala mdoc:silent
trait KVStore[F[_]] {
  def get(key: String): F[Option[String]]
  def put(key: String, value: String): F[Unit]
}
```

### Creating an Optimizer

To create an optimizer, you need to implement the `Optimizer` trait with three key components:

1. **`M`**: The type that collects static information (e.g., `Set[String]` for unique keys)
2. **`extract`**: An interpreter that collects static information
3. **`rebuild`**: A function that creates an optimized interpreter based on the collected information

```scala mdoc:silent
import cats._
import cats.data._
import cats.syntax.all._
import cats.tagless.optimize._

def createKVStoreOptimizer[F[_]: Monad]: Optimizer[KVStore, F] = new Optimizer[KVStore, F] {
  type M = Set[String]
  
  def monoidM = implicitly[Monoid[Set[String]]]
  def monadF = implicitly[Monad[F]]
  
  def extract = new KVStore[Const[Set[String], *]] {
    def get(key: String) = Const(Set(key))
    def put(key: String, value: String) = Const(Set.empty)
  }
  
  def rebuild(keys: Set[String], interp: KVStore[F]): F[KVStore[F]] = {
    // Pre-fetch all unique keys
    keys.toList.traverse(key => 
      interp.get(key).map(_.map(value => key -> value))
    ).map { results =>
      val cache = results.flatten.toMap
      
      new KVStore[F] {
        def get(key: String) = cache.get(key) match {
          case Some(value) => Option(value).pure[F]
          case None => interp.get(key)
        }
        def put(key: String, value: String) = interp.put(key, value)
      }
    }
  }
}
```

### Using the Optimizer

Now you can use the optimizer to optimize programs:

```scala mdoc:silent
import cats._
import cats.syntax.all._

def program[F[_]: Applicative](store: KVStore[F]): F[List[String]] = {
  (store.get("Cats"), store.get("Dogs"), store.get("Cats"), store.get("Birds"))
    .mapN((c, d, c2, b) => List(c, d, c2, b).flatten)
}

val programInstance = new Program[KVStore, Applicative, List[String]] {
  def apply[F[_]: Applicative](alg: KVStore[F]): F[List[String]] = program(alg)
}
```

```scala mdoc:silent
import cats.Eval

// Example interpreter
val mockInterpreter = new KVStore[Eval] {
  def get(key: String) = Eval.later {
    println(s"Fetching key: $key")
    Some(s"Value for $key")
  }
  def put(key: String, value: String) = Eval.later {
    println(s"Storing key: $key")
  }
}

implicit val optimizer = createKVStoreOptimizer[Eval]
```

```scala mdoc:compile-only
// Without optimization - each get is executed
val unoptimized = program(mockInterpreter).value

// With optimization - duplicate gets are eliminated
val optimized = programInstance.optimize(mockInterpreter).value
```

## Advanced Optimizations

### Put-Get Elimination

A more sophisticated optimization can eliminate redundant put-get pairs:

```scala mdoc:compile-only
import cats._
import cats.data._
import cats.syntax.all._
import cats.tagless.optimize._

case class KVStoreInfo(queries: Set[String], cache: Map[String, String])

// Note: KVStoreInfo needs a Monoid instance to work with Optimizer
implicit val kvStoreInfoMonoid: Monoid[KVStoreInfo] = new Monoid[KVStoreInfo] {
  def empty = KVStoreInfo(Set.empty, Map.empty)
  def combine(x: KVStoreInfo, y: KVStoreInfo) = 
    KVStoreInfo(x.queries |+| y.queries, x.cache |+| y.cache)
}

def createPutGetEliminator[F[_]: Monad]: Optimizer[KVStore, F] = new Optimizer[KVStore, F] {
  type M = KVStoreInfo
  
  def monoidM = implicitly[Monoid[KVStoreInfo]]
  def monadF = implicitly[Monad[F]]
  
  def extract = new KVStore[Const[KVStoreInfo, *]] {
    def get(key: String) = Const(KVStoreInfo(Set(key), Map.empty))
    def put(key: String, value: String) = Const(KVStoreInfo(Set.empty, Map(key -> value)))
  }
  
  def rebuild(info: KVStoreInfo, interp: KVStore[F]): F[KVStore[F]] = {
    val uncachedQueries = info.queries.filterNot(info.cache.contains)
    
    uncachedQueries.toList.traverse(key => 
      interp.get(key).map(_.map(value => key -> value))
    ).map { results =>
      val fetched = results.flatten.toMap
      val allCache = info.cache ++ fetched
      
      new KVStore[F] {
        def get(key: String) = allCache.get(key) match {
          case Some(value) => Option(value).pure[F]
          case None => interp.get(key)
        }
        def put(key: String, value: String) = interp.put(key, value)
      }
    }
  }
}
```

### Monad Optimizer

For more complex optimizations that require state, use `MonadOptimizer`:

```scala mdoc:compile-only
import cats._
import cats.data._
import cats.tagless.ApplyK
import cats.tagless.optimize._

// Note: For this to work, KVStore needs an ApplyK instance
// which can be auto-generated using @autoApplyK annotation

def createMonadOptimizer[F[_]: Monad](implicit applyKInstance: ApplyK[KVStore]): MonadOptimizer[KVStore, F] = new MonadOptimizer[KVStore, F] {
  type M = Map[String, String]
  
  def monoidM = implicitly[Monoid[Map[String, String]]]
  def monadF = implicitly[Monad[F]]
  def applyK = applyKInstance
  
  def rebuild(interp: KVStore[F]): KVStore[Kleisli[F, M, *]] = new KVStore[Kleisli[F, M, *]] {
    def get(key: String): Kleisli[F, M, Option[String]] = Kleisli { cache =>
      cache.get(key) match {
        case Some(value) => Option(value).pure[F]
        case None => interp.get(key)
      }
    }
    
    def put(key: String, value: String): Kleisli[F, M, Unit] = Kleisli { _ =>
      interp.put(key, value)
    }
  }
  
  def extract: KVStore[* => M] = new KVStore[* => M] {
    def get(key: String): Option[String] => M = {
      case Some(value) => Map(key -> value)
      case None => Map.empty
    }
    
    def put(key: String, value: String): Unit => M = _ => Map(key -> value)
  }
}
```

## Syntax Support

The optimization package provides convenient syntax for working with optimizers:

```scala mdoc:silent
import cats.tagless.optimize.syntax.all.*

// Using the syntax extension
val optimizedResult = programInstance.optimize(mockInterpreter)
```

## Best Practices

1. **Choose the Right Optimizer**: Use `SemigroupalOptimizer` for basic optimizations, `Optimizer` for applicative programs, and `MonadOptimizer` for complex stateful optimizations.

2. **Design Your Analysis Type**: The type `M` should efficiently represent the information you need to collect. Use appropriate data structures like `Set`, `Map`, or custom case classes.

3. **Consider Memory Usage**: Be mindful of the memory footprint of your analysis type, especially for large programs.

4. **Test Thoroughly**: Always verify that optimized programs produce the same results as unoptimized ones.

5. **Profile Performance**: Measure the actual performance improvements to ensure optimizations are beneficial.

## Limitations

- Optimizations are based on static analysis, so they work best with programs that have predictable patterns
- The analysis phase requires interpreting the program, which may have overhead for very simple programs
- Some optimizations may not be applicable to all effect types or program structures

## Further Reading

- [Optimizing Tagless Final – Saying farewell to Free](https://typelevel.org/blog/2017/12/27/optimizing-final-tagless.html) - The original blog post by Luka Jacobowitz
- [Sphynx](https://github.com/typelevel/sphynx) - The original library that inspired these optimizations
- [Cats-tagless tests](https://github.com/typelevel/cats-tagless/tree/master/tests/src/test/scala/cats/tagless/tests) - See `OptimizerTests.scala` for more examples
