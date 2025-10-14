---
layout: page
title: @autoSemigroupK
section: macros
position: 21
---


## @autoSemigroupK

### Intent
Generates an instance of `cats.SemigroupK` in the companion object of the annotated trait.

### Scope
The annotation can be applied to traits:
- With a single type parameter (e.g., `T`)
- Where methods return values of that type parameter wrapped in a `SemigroupK`-compatible effect (e.g., `Validated[Int, T]`)
- That may contain non-effect methods.
- That may provide default method implementations

### Examples
#### Before Annotation
```scala
import cats.tagless._

trait SemigroupKAlg[T] {
    def abstractEffect(a: String): Validated[Int, T]
    def concreteEffect(a: String): Validated[Int, T] = abstractEffect(a + " concreteEffect")
    def abstractOther(t: T): String
    def concreteOther(a: String): String = a + " concreteOther"
    def withoutParams: Comparison
    def headOption(ts: List[T]): Option[T]
}
```

#### After applying the macro 
```scala
import cats.tagless._

@autoSemigroupK
trait SemigroupKAlg[T] {
    def abstractEffect(a: String): Validated[Int, T]
    def concreteEffect(a: String): Validated[Int, T] = abstractEffect(a + " concreteEffect")
    def abstractOther(t: T): String
    def concreteOther(a: String): String = a + " concreteOther"
    def withoutParams: Comparison
    def headOption(ts: List[T]): Option[T]
}
```

This expands to
```scala
object SemigroupKAlg {
  implicit def semigroupKForSemigroupKAlg: SemigroupK[SemigroupKAlg] =
      Derive.semigroupK[SemigroupKAlg]
}
```

### Dependency and Limitation
- Works only on Scala 2; not supported in Scala 3.
In Scala 3 use native `derives SemigroupK` syntax instead.
- The annotation does not need to be combined with other annotations.
- It can handle: 
  - Extra type parameters in the traits
  - Non-effect methods
  - Defaualt implementations
- It ignores methods that do not involve the SemigroupK type parameter.

## Test Behavior (Verified)
The generated instance for `SemigroupKAlg[T]` satisfies all `SemigroupK` laws including:
- Associability
- Serialization