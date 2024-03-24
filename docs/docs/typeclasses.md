---
layout: page
title:  Type classes
section: typeclasses
position: 4
---


## Type classes


Currently, there are five type classes defined in Cats-tagless: [FunctorK](#functork), [ContravariantK](#contravariantk), [InvariantK](#invariantk), [SemigroupalK](#semigroupalk), and [ApplyK](#applyk). They can be deemed as somewhat higher kinded versions of the corresponding type classes in cats.



### FunctorK
```
  def mapK[F[_], G[_]](af: A[F])(fk: F ~> G): A[G]
```

For tagless final algebras whose effect `F` appears only in the covariant position, instance of `FunctorK` can be auto generated through the `autoFunctorK` annotation.

### ContravariantK
```
  def contramapK[F[_], G[_]](af: A[F])(fk: G ~> F): A[G]
```

For tagless final algebras whose effect `F` appears only in the contravariant position, instance of `ContravariantK` can be auto generated through the `autoContravariantK` annotation.

### InvariantK
```
  def imapK[F[_], G[_]](af: A[F])(fk: F ~> G)(gK: G ~> F): A[G]
```

For tagless final algebras whose effect `F` appears in both the covariant positions and contravariant positions, instance of `InvariantK` can be auto generated through the `autoInvariantK` annotation.

### SemigroupalK
```
 def productK[F[_], G[_]](af: A[F], ag: A[G]): A[Tuple2K[F, G, *]]
```

For tagless final algebras that
1. has no extra type parameters or abstract type members, and
2. whose effect `F` appears only in the covariant position for **all members**,

instance of `SemigroupalK` can be auto generated through `autoSemigroupalK` annotation.


### ApplyK
```
 def map2K[F[_], G[_], H[_]](af: A[F], ag: A[G])(f: Tuple2K[F, G, *] ~> H): A[H]
```

`ApplyK` extends both `SemigroupalK` and `FunctorK` just like their lower kinded counterparts.

For tagless final algebras that
1. has no extra type parameters or abstract type members, and
2. whose effect `F` appears only in the covariant position for **all members**,

instance of `ApplyK` can be auto generated through `autoApplyK` annotation.



Their laws are defined in `cats.tagless.laws`. To test your instance (if you decide to roll your own) against these laws please follow the examples in `cats.tagless.tests`, especially the ones that test against `SafeAlg`.
