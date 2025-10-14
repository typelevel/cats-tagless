---
layout: page
title: @autoInvariantK
section: macros
position: 15
---


## @autoInvariantK

### Intent
Generates an instance of `cats.InvariantK` in the companion object of the annotated trait.

### Scope
The annotation can be applied to traits:
- With a single higher-kinded type parameter (e.g., `[F[_]]`)
- Where methods consume or return values of that type parameter (e.g., `F[Int]`)
- That are generic methods, curried methods and varargs 
- That may contain non-effect methods.
- That define extra type parameters beyond the effect one
- That may provide default method implementations

### Examples
#### Before Annotation
```scala
import cats.tagless._

trait InvariantKAlg[F[_]] {
  def parseInt(str: String): F[Int]
  def divide(dividend: Float, divisor: Float): F[Float]
}
```

#### After applying the macro 
```scala
import cats.tagless._

@autoInvariantK
trait InvariantKAlg[F[_]] {
    def parseInt(str: String): F[Int]
    def divide(dividend: Float, divisor: Float): F[Float]
  
}
```

This expands to
```scala
object InvariantKAlg {
  implicit def invariantKForInvarianKAlg: InvariantK[InvariantKAlg] =
      Derive.invariantK[InvariantKAlg]
}
```

#### Extra type Parameters

```scala
@autoInvariantK
  @autoInvariantK @finalAlg
  trait InvariantKWithExtraTpeParam[F[_], T] {
    def a(i: F[T]): F[T]
    def b(i: F[Int]): F[T]
  }
```

### Dependency and Limitation
- Works only on Scala 2; not supported in Scala 3.
In Scala 3 use native `derives InvariantK` syntax instead.
- May require `@finalAlg`.
- It can handle: 
  - Type parameters in the traits
  - Generic methods
  - Varargs
  - Curried method
  - Default implementations
- It ignores methods that do not involve the invariant type parameter.

## Test Behavior (Verified)
The generated instance for `InvariantKAlg[F[_]]` satisfies all `InvariantK` laws including:  
- Invariant composition
- Invariant identity
- Serialization