---
layout: page
title: @autoMonoidK
section: macros
position: 16
---


## @autoMonoidK

### Intent
Generates an instance of `cats.MonoidK` in the companion object of the annotated trait.

### Scope
The annotation can be applied to traits:
- With a single type parameter (e.g., `T`)
- Where methods return values of that type parameter wrapped in a collection-like structure (e.g., `Map[String, T]`)
- That may contain non-effect methods.
- That may provide default method implementations

### Examples
#### Before Annotation
```scala
import cats.tagless._

trait MonoidKAlg[T] {
    def abstractEffect(a: String): Map[String, T]
    def concreteEffect(a: String): Map[String, T] = abstractEffect(a + " concreteEffect")
    def abstractOther(t: T): String
    def concreteOther(a: String): String = a + " concreteOther"
    def withoutParams: Int
    def headOption(ts: List[T]): Option[T]
}
```

#### After applying the macro 
```scala
import cats.tagless._

@autoMonoidK
trait MonoidKAlg[T] {
    def abstractEffect(a: String): Map[String, T]
    def concreteEffect(a: String): Map[String, T] = abstractEffect(a + " concreteEffect")
    def abstractOther(t: T): String
    def concreteOther(a: String): String = a + " concreteOther"
    def withoutParams: Int
    def headOption(ts: List[T]): Option[T]
}
```

This expands to
```scala
object MonoidKAlg {
  implicit def monoidKForMonoidKAlg: MonoidK[MonoidKAlg] =
      Derive.monoidK[MonoidKAlg]
}
```

### Dependency and Limitation
- Works only on Scala 2; not supported in Scala 3.
In Scala 3 use native `derives MonoidK` syntax instead.
- The annotation does not need to be combined with other annotations.
- It can handle: 
  - Extra type parameters in the traits
  - Non-effect methods
  - Defaualt implementations
- It ignores methods that do not involve type parameter.

## Test Behavior (Verified)
The generated instance for `MonoidKAlg[T]` satisfies all `MonoidK` laws including:
- Left identity
- Right identity
- Associativity
- Serialization