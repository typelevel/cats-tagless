---
layout: page
title: @autoInvariant
section: macros
position: 14
---


## @autoInvariant

### Intent
Generates an instance of `cats.Invariant` in the companion object of the annotated trait.

### Scope
The annotation can be applied to traits:
- With a single type parameter (e.g., `T`)
- Where methods consume or return values of that type parameter (e.g., `def foo(t: T): String`)
- That are generic methods, curried methods and varargs 
- That may contain non-effect methods.
- That define extra type parameters beyond the invariant one
- That may provide default method implementations

### Examples
#### Before Annotation
```scala
import cats.tagless._

trait InvariantAlg[T] {
    def foo(a: T): T
    def headOption(ts: List[T]): Option[T]
    def map[U](ts: List[T])(f: T => U): List[U] = ts.map(f)
}
```

#### After applying the macro 
```scala
import cats.tagless._

@autoInvariant
trait InvariantAlg[T] {
    def foo(a: T): T
    def headOption(ts: List[T]): Option[T]
    def map[U](ts: List[T])(f: T => U): List[U] = ts.map(f)
}
```

This expands to
```scala
object InvariantAlg {
  implicit def invariantForInvariantAlg: Invariant[InvariantAlg] =
      Derive.invariant[InvariantAlg]
}
```

#### Extra type Parameters

```scala
@autoInvariant
  trait InvariantAlgWithExtraTypeParam[T1, T] {
    def foo(a: T1, b: T): T
  }
```

#### Generic and vararg methods
```scala
@autoInvariant
  trait InvariantAlgWithGenericMethod[T] {
    def foo[A](i: T, a: A): T
  }

  @autoInvariant
  trait InvariantAlgWithVarArgsParameter[T] {
    def sum(xs: Int*): Int
    def covariantSum(xs: Int*): T
    def contravariantSum(xs: T*): Int
    def invariantSum(xs: T*): T
  }
```
#### Non-effect methods

```scala
@autoInvariant
  trait InvariantAlgWithGenericMethod[T] {
    def foo(a: String): Float = a.length.toFloat
    def foo2(a: String): String = a.reverse
    def foo3(a: Float): String = a.toInt.toString
  }
```

### Dependency and Limitation
- Works only on Scala 2; not supported in Scala 3.
In Scala 3 use native `derives Invariant` syntax instead.
- The annotation does not need to be combined with other annotations.
- It can handle: 
  - Extra type parameters in the traits
  - Generic methods
  - Varargs
  - Curried method
  - Defaualt implementations
- It ignores methods that do not involve the invariant type parameter.

## Test Behavior (Verified)
The generated instance for `InvariantAlg[T]` satisfies all `Invariant` laws including:
- Invariant composition and identity
- Serialization