---
layout: page
title: @autoFlatMap
section: macros
position: 10
---


## @autoFlatMap

### Intent
Generates an instance of `cats.FlatMap` in the companion object of the annotated trait.

### Scope
The annotation can be applied to traits:
- With a single type parameter (e.g., `T`)
- Where methods consume values of that type parameter (e.g., `def foo(t: T): String`)
- That are generic methods, curried methods and varargs 
- That may contain non-effect methods.
- That define extra type parameters beyond the contravariant one

### Examples
#### Before Annotation
```scala
import cats.tagless._

trait FlatMapAlg[T] {
    def abstractEffect(a: String): T
    def concreteEffect(a: String): T = abstractEffect(a + " concreteEffect")
    def abstractOther(a: String): String
    def concreteOther(a: String): String = a + " concreteOther"
    def withoutParams: T
}
```

#### After applying the macro 
```scala
import cats.tagless._

@autoFlatMap
trait FlatMapAlg[T] {
    def abstractEffect(a: String): T
    def concreteEffect(a: String): T = abstractEffect(a + " concreteEffect")
    def abstractOther(a: String): String
    def concreteOther(a: String): String = a + " concreteOther"
    def withoutParams: T
}
```

This expands to
```scala
object FlatMapAlg {
  implicit def flatMapForFlatMapAlg: FlatMap[FlatMapAlg] =
      Derive.flatMap[FlatMapAlg]
}
```

#### Extra type Parameters

```scala
@autoFlatMap
  trait FlatMapAlgWithExtraTypeParam[T1, T] {
    def foo(a: T1, b: T): Int
  }
```

#### Generic and vararg methods
```scala
@autoFlatMap
  trait FlatMapAlgWithGenericMethod[T] {
    def plusOne[A](i: A): T
  }

  @autoFlatMap
  trait FlatMapAlgWithVarArgsParameter[T] {
    def sum(xs: Int*): Int
    def generic[A](as: A*): T
  }
```


### Dependency and Limitation
- Works only on Scala 2; not supported in Scala 3.
In Scala 3 use native `derives FlatMap` syntax instead.
- The annotation does not need to be combined with other annotations.
- It can handle: 
  - Type parameters in the traits
  - Generic methods
  - Varargs
  - Curried method
- It ignores methods that do not involve the contravariant type parameter.

## Test Behavior (Verified)
The generated instance for `FlatMapAlg[T]` satisfies all `FlatMap` laws including:

- `apply` composition
- FlatMap associativity
- Consistency with `apply`, `tailRecM`, and `map`
- `mproduct`, `productL`, `productR` consistency
- `map2`, `map2Eval`, and `product` consistency
- Semigroupal associativity
- Convariant identity and composition 
- Invariant composition and identity
- Serialization