---
layout: page
title: @autoSemigroupalK
section: macros
position: 20
---


## @autoSemigroupalK

### Intent
Generates an instance of `cats.SemigroupalK` in the companion object of the annotated trait.

### Scope
The annotation can be applied to traits:
- With a single higher-kinded type parameter (e.g., `F[_]`)
- Where methods return values of that effect type
- That may contain constant return types 
- That are generic methods, curried methods and varargs 

### Examples
#### Before Annotation
```scala
import cats.tagless._

trait SemigroupalKAlgF[F[_]] {
    def sum(xs: Int*): Int
    def effectfulSum(xs: Int*): F[Int]
}
```

#### After applying the macro 
```scala
import cats.tagless._

@autoSemigroupalK
trait SemigroupalKAlg[F[_]] {
    def sum(xs: Int*): Int
    def effectfulSum(xs: Int*): F[Int]
}
```

This expands to
```scala
object SemigroupalKAlg {
  implicit def semigroupalKForSemigroupalKAlg: SemigroupalK[SemigroupalKAlg] =
      Derive.semigroupalK[SemigroupalKAlg]
}
```

### Dependency and Limitation
- Works only on Scala 2; not supported in Scala 3.
In Scala 3 use native `derives SemigroupalK` syntax instead.
- The annotation does not need to be combined with other annotations.
- It can handle: 
  - Generic methods
  - Varargs
  - Curried method
  - Constant return types
- It ignores methods that do not involve the effect type parameter.

## Test Behavior (Verified)
The generated instance for `SemigroupalKAlg[T]` satisfies all `SemigroupalK` laws including:
- Associativity
- Serialization