---
layout: page
title: @autoFunctorK
section: macros
position: 12
---


## @autoFunctorK

### Intent
Generates an instance of `cats.FunctorK` in the companion object of the annotated trait.

### Scope
- With a single higher-kinded type parameter (e.g., `F[_]`)
- Where methods return values of that type parameter (e.g., `F[Int]`)
- That are generic methods, curried methods and varargs 
- That may contain non-effect methods.
- That define extra type parameters beyond the effect type
- That use type members, type bounds, context bounds, or abstract type classes
- That define default parameters, final methods, or by-name parameters
- That follow builder-style algebra patterns

### Examples
#### Before Annotation
```scala
import cats.tagless._

trait FunctorKAlg[F[_]] {
  def parseInt(str: String): F[Int]
  def divide(dividend: Float, divisor: Float): F[Float]
}
```

#### After applying the macro 
```scala
import cats.tagless._

@autoFunctorK
trait FunctorKAlg[F[_]] {
    def parseInt(str: String): F[Int]
  def divide(dividend: Float, divisor: Float): F[Float]
}
```

This expands to
```scala
object FunctorKAlg {
  implicit def functoKrForFunctorKAlg: Functor[FunctorKAlg] =
      Derive.functor[FunctorKAlg]
}
```

#### Extra type Parameters

```scala
@autoFunctorK
  trait FunctorKAlgWithExtraParam[F[_], A] {
  def a(i: Int): F[A]
}
```

### Dependency and Limitation
- Works only on Scala 2; not supported in Scala 3.
In Scala 3 use native `derives FunctorK` syntax instead.
- The annotation does not need to be combined with other annotations.
- It can handle: 
  - Type parameters in the traits
  - Generic methods
  - Varargs
  - Curried method
  - Abstract type classes
  - Default parameters
  - Final methods
  - By-nameparameters
- It ignores methods that do not involve the contravariant type parameter.

## Test Behavior (Verified)
The generated instance for `FunctorKAlg[F[_]]` satisfies all `FunctorK` laws including:
- Convariant identity and composition 
- Invariant composition and identity
- Serialization