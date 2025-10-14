---
layout: page
title: @autoApply
section: macros
position: 5
---


## @autoApply

### Intent
Generates an instance of `cats.Apply` in the companion object of the annotated trait.

### Scope
The annotation can be applied to traits:
- With a single type parameter (e.g., `T`)
- Where methods return the type value of the parameter 
- That are generic method, curried method and varargs parameters
- With extra type parameter.

### Examples
#### Before Annotation
```scala
import cats.tagless._

trait ApplyAlg[T] {
    def abstractEffect(a: String): T
    def curried(a: String)(b: Int): T
}
```

#### After applying the macro 
```scala
import cats.tagless._

@autoApply
trait ApplyAlg[T] {
  def abstractEffect(a: String): T
    def curried(a: String)(b: Int): T
}
```

This expands to
```scala
object ApplyAlg {
  implicit def applyForApplyAlg: Apply[ApplyAlg] =
      Derive.apply[ApplyAlg]
}
```
#### Extra type parameter
```scala
import cats.tagless._

@autoApply
trait ApplyAlgWithExtraTypeParam[T1, T] {
  def foo(a: T1): T
}
```

### Dependency and Limitation
- Works only on Scala 2; not supported in Scala 3.
In Scala 3 use native `derives Apply` syntax instead.
- The annotation does not need to be combined with other annotations.
- It can handle: 
  - Type parameters in the traits
  - Generic methods
  - Varargs
  - Curried method
- Methods that that do not involve type parameter are ignored.


## Test Behavior(Verified)
The generated instance for `ApplyAlg[T]` satisfies all `Apply` laws including:
- Composition 
- Identity
- Consistency between `map2`, `product`, and `map`
- Associativity
- Serialization