---
layout: page
title: @autoSemigroupal
section: macros
position: 19
---


## @autoSemigroupal

### Intent
Generates an instance of `cats.Semigroupal` in the companion object of the annotated trait.

### Scope
The annotation can be applied to traits:
- With a single type parameter (e.g., `T`)
- Where methods consume or return values of that type parameter (e.g., `def foo(t: T): String`)
- That are generic methods, curried methods and varargs 
- That may contain constant return types 
- That may provide default method implementations

### Examples
#### Before Annotation
```scala
import cats.tagless._

trait SemigroupalAlg[T] {
    def abstractEffect(a: String): T
    def concreteEffect(a: String): T = abstractEffect(a + " concreteEffect")
    def abstractOther(a: String): String
    def concreteOther(a: String): String = a + " concreteOther"
    def withoutParams: T
    def curried(a: String)(b: Int): T
    def headOption(ts: List[T]): Option[T]
}
```

#### After applying the macro 
```scala
import cats.tagless._

@autoSemigroupal
trait SemigroupalAlg[T] {
    def abstractEffect(a: String): T
    def concreteEffect(a: String): T = abstractEffect(a + " concreteEffect")
    def abstractOther(a: String): String
    def concreteOther(a: String): String = a + " concreteOther"
    def withoutParams: T
    def curried(a: String)(b: Int): T
    def headOption(ts: List[T]): Option[T]
}
```

This expands to
```scala
object SemigroupalAlg {
  implicit def semigroupalForSemigroupalAlg: Semigroupal[SemigroupalAlg] =
      Derive.semigroupal[SemigroupalAlg]
}
```

#### Extra type Parameters

```scala
@autoSemigroupal
  trait SemigroupalAlgWithExtraTypeParam[T1, T] {
    def foo(a: T1): T
  }
```

#### Generic and vararg methods
```scala
@autoSemigroupal
  trait SemigroupalAlgWithGenericMethod[T] {
   def plusOne[A](i: A): T
  }

  @autoSemigroupal
  trait SemigroupalAlgWithVarArgsParameter[T] {
      def sum(xs: Int*): Int
      def product(xs: Int*): T
  }
```

### Dependency and Limitation
- Works only on Scala 2; not supported in Scala 3.
In Scala 3 use native `derives Semigroupal` syntax instead.
- The annotation does not need to be combined with other annotations.
- It can handle: 
  - Extra type parameters in the traits
  - Generic methods
  - Varargs
  - Curried method
  - Constant return types
  - Defaualt implementations
- It ignores methods that do not involve the invariant type parameter.

## Test Behavior (Verified)
The generated instance for `SemigroupalAlg[T]` satisfies all `Semigroupal` laws including:
- Associativity
- Serialization