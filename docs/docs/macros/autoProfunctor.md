---
layout: page
title: @autoProfunctor
section: macros
position: 18
---


## @autoProfunctor

### Intent
Generates an instance of `cats.Profunctor` in the companion object of the annotated trait.

### Scope
The annotation can be applied to traits:
- With two type parameters (e.g., `A, B`)
- Where methods consume values of the first type parameter (`A`) and/or return values of the second type parameter (`B`)
- That are include generic methods, curried methods and varargs 
- That define extra type parameters beyond the two Profunctor ones
- That may provide default method implementations

### Examples
#### Before Annotation
```scala
import cats.tagless._

trait ProfunctorAlg[A, B] {
    def abstractCovariant(str: String): B
    def concreteCovariant(str: String): B = abstractCovariant(str + " concreteCovariant")
    def abstractContravariant(a: A): String
    def concreteContravariant(a: A): String = abstractContravariant(a) + " concreteContravariant"
    def abstractMixed(a: A): B
    def concreteMixed(a: A): B = abstractMixed(a)
    def abstractOther(str: String): String
    def concreteOther(str: String): String = str + " concreteOther"
    def withoutParams: B
    def fromList(as: List[A]): List[B]
}
```

#### After applying the macro 
```scala
import cats.tagless._

@autoProfunctor
trait ProfunctorAlg[A, B] {
    def abstractCovariant(str: String): B
    def concreteCovariant(str: String): B = abstractCovariant(str + " concreteCovariant")
    def abstractContravariant(a: A): String
    def concreteContravariant(a: A): String = abstractContravariant(a) + " concreteContravariant"
    def abstractMixed(a: A): B
    def concreteMixed(a: A): B = abstractMixed(a)
    def abstractOther(str: String): String
    def concreteOther(str: String): String = str + " concreteOther"
    def withoutParams: B
    def fromList(as: List[A]): List[B]
}
```

This expands to
```scala
object ProfunctorAlg {
  implicit def profunctorForProfunctorAlg: Profunctor[ProfunctorAlg] =
      Derive.profunctor[ProfunctorAlg]
}
```

#### Extra type Parameters

```scala
@autoProfunctor
  trait ProfunctorAlgWithExtraTypeParam[T, A, B] {
   def foo(t: T, a: A): B
  }
```

### Dependency and Limitation
- Works only on Scala 2; not supported in Scala 3.
In Scala 3 use native `derives Profunctor` syntax instead.
- The annotation does not need to be combined with other annotations.
- It can handle: 
  - Extra type parameters in the traits
  - Generic methods
  - Varargs
  - Curried method
  - Defaualt implementations
- It ignores methods that do not involve the Profunctor type parameter.

## Test Behavior (Verified)
The generated instance for `ProfunctorAlg[A, B]` satisfies all `Profunctor` laws including:
- Profunctor composition and identity
- `lmap` composition and identity
- `rmap` composition and identity
- Serialization