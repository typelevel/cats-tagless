---
layout: page
title: @autoProductNK
section: macros
position: 17
---


## @autoProductNK

### Intent
Generates methods in companion object to compose multiple interpreters into an interpreter of a `TupleNK` effects

### Scope
The annotation can be applied to traits:
- With a single higher-kinded type parameter (e.g., `[F[_]]`)
- Where methods consume or return values of that effect type
- That are generic methods, curried methods and varargs 
- That may contain multiple parameter lists

### Examples
#### Before Annotation
```scala
import cats.tagless._

trait ProductNKAlgWithGenericType[F[_]] {
    def a[T](a: T): F[Unit]
}
```

#### After applying the macro 
```scala
import cats.tagless._

@autoProductNK
trait ProductNKAlgWithGenericType[F[_]] {
    def a[T](a: T): F[Unit]
}
```

This expands to
```scala
object ProductNKAlgWithGenericType {
  def product3K[F1[_], F2[_], F3[_]](
    af1: algWithGenericType[F1],
    af2: algWithGenericType[F2],
    af3: algWithGenericType[F3]
  ): algWithGenericType[Tuple3K[F1, F2, F3]#λ] =
    new algWithGenericType[Tuple3K[F1, F2, F3]#λ] {
      def a[T](t: T): Tuple3K[F1, F2, F3]#λ[Unit] =
        (af1.a(t), af2.a(t), af3.a(t))
    }
}
```

### Dependency and Limitation
- Works only on Scala 2; not supported in Scala 3.
In Scala 3 use native `derives ProductNK` syntax instead.
- Does not generate a type class instance, but instead companion methods.
- It can handle: 
  - Type parameters in the traits
  - Generic methods
  - Varargs
  - Curried method
  - Multiple parameter lists