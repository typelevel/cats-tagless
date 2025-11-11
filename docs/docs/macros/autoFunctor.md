---
layout: page
title: @autoFunctor
section: macros
position: 6
---


## @autoFunctor

### Intent
Generates an instance of `cats.Functor` in the companion object of the annotated trait.

### Scope
The annotation can be applied to traits:
- With a single type parameter (e.g., `T`)
- Where methods consume values of that type parameter (e.g., `def foo(t: T): String`)
- That are generic methods, curried methods and varargs 
- That may contain non-effect methods.
- That define extra type parameters beyond the contravariant one

### Examples
```scala
import cats.tagless._
import cats.syntax.functor._

@autoFunctor
trait FunctorAlg[T] {
    def sum(x: Int, y: Int): T
    def negate(x: Int): T
}
```
#### Implementation 

```scala
val functorOption: FunctorAlg[Option] = new FunctorAlg[Option] {
    def sum(x: Int, y: Int): Option[Int] = Some(x + y)
    def negate(x: Int): Option[Int] = Some(-x)
}
```

#### After applying the macro 
```scala
// Mapping is now possible with @autoFunctor
val mapped: FunctorAlg[Option] = functorOption.map(n => n * 10)

// Example results
val sumResult = mapped.sum(4,4)  //Some(80)
val negateResult = mapped.negate(8)  //Some(-80)


```

The macro expansion
```scala
object FunctorAlg {
  implicit def functorForFunctorAlg: Functor[FunctorAlg] =
      Derive.functor[FunctorAlg]
}
```

### Dependencies and Limitations
- Works only on Scala 2; not supported in Scala 3.
In Scala 3 use native `derives Functor` syntax instead.
- The annotation does not need to be combined with other annotations.
- It can handle: 
  - Type parameters in the traits
  - Generic methods
  - Varargs
  - Curried methods
- It ignores methods that do not return the type parameter.

## Test Behavior (Verified)
The generated instance for `FunctorAlg[T]` satisfies functor `Functor` laws including:

- `mapOrKeepToMap` equivalence
- Identity 
- Composition 
- Serialization