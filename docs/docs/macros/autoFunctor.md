---
layout: page
title: @autoFunctor
section: macros
position: 11
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
#### Before Annotation
```scala
import cats.tagless._

trait FunctorAlg[T] {
    def abstractEffect(a: String): T
    def concreteEffect(a: String): T = abstractEffect(a + " concreteEffect")
    def abstractOther(a: String): String
    def concreteOther(a: String): String = a + " concreteOther"
    def withoutParams: T
    def toList(xs: List[Int]): List[T]
    def fromFunction(f: T => String): T
}
```

#### After applying the macro 
```scala
import cats.tagless._

@autoFunctor
trait FunctorAlg[T] {
    def abstractEffect(a: String): T
    def concreteEffect(a: String): T = abstractEffect(a + " concreteEffect")
    def abstractOther(a: String): String
    def concreteOther(a: String): String = a + " concreteOther"
    def withoutParams: T
    def toList(xs: List[Int]): List[T]
    def fromFunction(f: T => String): T
}
```

This expands to
```scala
object FunctorAlg {
  implicit def functorForFunctorAlg: Functor[FunctorAlg] =
      Derive.functor[FunctorAlg]
}
```

#### Extra type Parameters

```scala
@autoFunctor
  trait FunctorAlgWithExtraTypeParam[T1, T] {
    def foo(a: T1): T
  }
```

#### Generic and vararg methods
```scala
@autoFunctor
  trait FunctorAlgWithGenericMethod[T] {
    def plusOne[A](i: A): T
  }

  @autoFunctor
  trait FunctorAlgWithVarArgsParameter[T] {
    def sum(xs: Int*): Int
    def generic[A](as: A*): T
  }
```


### Dependency and Limitation
- Works only on Scala 2; not supported in Scala 3.
In Scala 3 use native `derives Functor` syntax instead.
- The annotation does not need to be combined with other annotations.
- It can handle: 
  - Type parameters in the traits
  - Generic methods
  - Varargs
  - Curried method
- It ignores methods that do not involve the contravariant type parameter.

## Test Behavior (Verified)
The generated instance for `FunctorAlg[T]` satisfies all `Functor` laws including:

- `mapOrKeepToMap` equivalence
- Convariant identity and composition 
- Invariant composition and identity
- Serialization