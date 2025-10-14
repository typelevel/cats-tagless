---
layout: page
title: @autoInstrument
section: macros
position: 13
---


## @autoInstrument

### Intent
Generates an instance of `cats.Instrument` in the companion object of the annotated trait.

### Scope
- With a single higher-kinded type parameter (e.g., `F[_]`)
- Where methods return values wrapped in that effect type
- That may contain non-effect methods.

### Examples
#### Before Annotation
```scala
import cats.tagless._

trait InstrumentAlg[F[_]] {
   def ->(id: String): F[Option[Long]]
}
```

#### After applying the macro 
```scala
import cats.tagless._

@autoInstrument
@finalAlg
trait InstrumentAlg[F[_]] {
    def ->(id: String): F[Option[Long]]
  }
```

This expands to
```scala
object InstrumentAlg {
  implicit def instrumentForInstrumentAlg: Instrument[InstrumentAlg] =
      Derive.instrument[InstrumentAlg]
}
```

### Dependency and Limitation
- Works only on Scala 2; not supported in Scala 3.
In Scala 3 use native `derives Instrument` syntax instead.
- Requires `@finalAlg` for proper derivation in most cases.
- It can handle: 
  - Non effect methods
  - Default implementations
- It ignores methods that do not involve the effect type parameter.

## Test Behavior (Verified)
The generated instance for `InstrumentAlg[F[_]]` satisfies all `Instrument` laws including:
- Convariant identity and composition 
- Invariant composition and identity
- Preserving semantics of instrumented methods
- Serialization