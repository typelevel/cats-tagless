---
layout: page
title: @autoApplyK
section: macros
position: 6
---


## @autoApplyK

### Intent
Generates an instance of `cats.ApplyK` in the companion object of the annotated trait.

### Scope
The annotation can be applied to traits:
- With single higher‑kinded type parameter (e.g., `F[_]`)
- Where methods return the type value of the parameter (e.g., `F[Int]`, `EitherT[F, E, A]`)
- That are generic method, curried method and varargs parameters
- With extra type parameters.

### Examples
#### Before Annotation
```scala
import cats.tagless._

trait ApplyKAlg[F[_]] {
  def parseInt(str: String): F[Int]
  def parseDouble(str: String): EitherT[F, String, Double]
  def divide(dividend: Float, divisor: Float): F[Float]
}
```

#### After applying the macro 
```scala
import cats.tagless._

@autoApplyK
trait ApplyKAlg[F[_]] {
  def parseInt(str: String): F[Int]
  def parseDouble(str: String): EitherT[F, String, Double]
  def divide(dividend: Float, divisor: Float): F[Float]
}
```

This expands to
```scala
object ApplyKAlg {
  implicit def applyKForApplyKAlg: ApplyK[ApplyKAlg] =
      Derive.applyK[ApplyKAlg]
}
```
#### VarArgs Parameter
```scala
@autoApplyK
  trait ApplyKAlgWithVarArgsParameter[F[_]] {
    def sum(xs: Int*): Int
    def fSum(xs: Int*): F[Int]
  }
```

### Dependency and Limitation
- Works only on Scala 2; not supported in Scala 3.
In Scala 3 use native `derives ApplyK` syntax instead.
- The annotation does not need to be combined with other annotations.
- It can handle: 
  - Type parameters in the traits
  - Generic methods
  - Varargs
  - Curried method
- It ignores methods that return non-effect types (e.g. `String`, `Int`).


## Test Behavour
The generated instance for `ApplyKAlg[F[_]]` satisfies all `ApplyK` laws including:
- Composition (covariant and invariant)
- Identity (covariant and invariant)
- Semigroupal associativity
- Associativity
- Serialization