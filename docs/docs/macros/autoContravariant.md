---
layout: page
title: @autoContravariant
section: macros
position: 8
---


## @autoContravariant

### Intent
Generates an instance of `cats.Contravariant` in the companion object of the annotated trait.

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

trait ContravariantAlg[T] {
    def foo(t: T): String
    def bar(opt: Option[T]): String
}
```

#### After applying the macro 
```scala
import cats.tagless._

@autoContravariant
trait ContravariantAlg[T] {
    def foo(t: T): String
    def bar(opt: Option[T]): String
  
}
```

This expands to
```scala
object ContravariantAlg {
  implicit def contravariantForContravariantAlg: Contravariant[ContravariantAlg] =
      Derive.contravariant[ContravariantAlg]
}
```

#### Extra type Parameters

```scala
@autoContravariant
  trait ContravariantAlgWithExtraTypeParam[T1, T] {
    def foo(a: T1, b: T): Int
  }
```

### Dependency and Limitation
- Works only on Scala 2; not supported in Scala 3.
In Scala 3 use native `derives Contravariant` syntax instead.
- The annotation does not need to be combined with other annotations.
- It can handle: 
  - Type parameters in the traits
  - Generic methods
  - Varargs
  - Curried method
- It ignores methods that do not involve the contravariant type parameter.

## Test Behavior (Verified)
The generated instance for `ContravariantAlg[T]` satisfies all `Contravariant` laws including:
- Contravariant composition
- Contravariant identity  
- Invariant composition
- Invariant identity
- Serialization