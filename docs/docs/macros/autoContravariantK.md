---
layout: page
title: @autoContravariantK
section: macros
position: 9
---


## @autoContravariantK

### Intent
Generates an instance of `cats.ContravariantK` in the companion object of the annotated trait.

### Scope
The annotation can be applied to traits:
- With a single higher-kinded type parameter (e.g., `[F[_]]`)
- Where methods consume values of that type parameter (e.g., `F[Int]`)
- That are generic methods, curried methods and varargs 
- That may contain non-effect methods.
- That define extra type parameters beyond the contravariant one

### Examples
#### Before Annotation
```scala
import cats.tagless._

trait ContravariantKAlg[F[_]] {
    def sum(xs: F[Int]): Int
    def sumAll(xss: F[Int]*): Int
    def foldSpecialized(init: String)(f: (Int, String) => Int): Cokleisli[F, String, Int]
}
```

#### After applying the macro 
```scala
import cats.tagless._

@autoContravariantK
trait ContravariantKAlg[F[_]] {
    def sum(xs: F[Int]): Int
    def sumAll(xss: F[Int]*): Int
    def foldSpecialized(init: String)(f: (Int, String) => Int): Cokleisli[F, String, Int]
  
}
```

This expands to
```scala
object ContravariantKAlg {
  implicit def contravariantKForContravarianKAlg: ContravariantK[ContravariantKAlg] =
      Derive.contravariantK[ContravariantKAlg]
}
```

#### Extra type Parameters

```scala
@autoContravariantK
  trait ContravariantKAlgWithExtraTypeParam[F[_], A] extends Contravariant[F] {
    def fold[B](init: B)(f: (B, A) => B): Cokleisli[F, A, B]
  }
```

### Dependency and Limitation
- Works only on Scala 2; not supported in Scala 3.
In Scala 3 use native `derives ContravariantK` syntax instead.
- The annotation does not need to be combined with other annotations.
- It can handle: 
  - Type parameters in the traits
  - Generic methods
  - Varargs
  - Curried method
- It ignores methods that do not involve the contravariant type parameter.

## Test Behavior (Verified)
The generated instance for `ContravariantKAlg[F[_]]` satisfies all `ContravariantK` laws including:
- Contravariant composition
- Contravariant identity  
- Invariant composition
- Invariant identity
- Serialization