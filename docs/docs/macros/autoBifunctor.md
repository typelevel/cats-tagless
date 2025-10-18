---
layout: page
title: @autoBifunctor
section: macros
position: 7
---


## @autoBifunctor

### Intent
Generates an instance of `cats.Bifunctor` in the companion object of the annotated trait.

### Scope
The annotation can be applied to traits:
- With two type parameters (e.g., `A, B`) 
- Where methods return or consume those type parameters
- That are generic method, curried method and varargs parameters
- With extra type parameters.

### Examples
#### Before Annotation
```scala
import cats.tagless._

trait BifunctorAlg[A, B] {
    def left: A
    def right: B
    def fromInt(i: Int): A
    def fromString(s: String): B
}
```

#### After applying the macro 
```scala
import cats.tagless._

@autoBifunctor
trait BifunctorAlg[A, B] {
    def left: A
    def right: B
    def fromInt(i: Int): A
    def fromString(s: String): B
}
```

This expands to
```scala
object BifunctorAlg {
  implicit def bifunctorAlgForAutoApplyKAlg: BifunctorAlg[BifunctorAlg] =
      Derive.bifunctor[BifunctorAlg]
}
```

### Dependency and Limitation
- Works only on Scala 2; not supported in Scala 3.
In Scala 3 use native `derives Bifunctor` syntax instead.
- The annotation does not need to be combined with other annotations.
- It can handle: 
  - Type parameters in the traits
  - Generic methods
  - Varargs
  - Curried method
- It ignores methods that do not involve the last two type parameters.


## Test Behavior
The generated instance for `BifunctorAlg[A, B]` satisfies all `Bifunctor` laws including:
- Identity 
- Associativity 
- `leftMap` identity
- `leftMap` associativity
- Serialization