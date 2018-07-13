---
layout: page
title:  Type classes
section: typeclasses
position: 4
---


## Type classes


Currently there are four type classes defined in mainecoon: [FunctorK](#functorK), [InvariantK](#invariantK), [SemigroupalK](#semigroupalK), and [ApplyK](#applyK). They can be deemed as somewhat higher kinded versions of the corresponding type classes in cats.



### <a id="functorK" href="#functorK"></a>`FunctorK` 
```
  def mapK[F[_], G[_]](af: A[F])(fk: F ~> G): A[G]
```

For tagless final algebras whose effect `F` appears only in the covariant position, instance of `FunctorK` can be auto generated through the `autoFunctorK` annotation.

### <a id="invariantK" href="#invariantK"></a>`InvariantK` 
```
  def imapK[F[_], G[_]](af: A[F])(fk: F ~> G)(gK: G ~> F): A[G]
```

For tagless final algebras whose effect `F` appears in both the covariant positions and contravariant positions, instance of `InvariantK` can be auto generated through the `autoInvariantK` annotation.

### <a id="semigroupalK" href="#semigroupalK"></a>`SemigroupalK`
```
 def productK[F[_], G[_]](af: A[F], ag: A[G]): A[Tuple2K[F, G, ?]]
```

For tagless final algebras that
1. has no extra type parameters or abstract type members, and
2. whose effect `F` appears only in the covariant position for **all members**,

instance of `SemigroupalK` can be auto generated through `autoSemigroupalK` annotation.


### <a id="applyK" href="#applyK"></a>`ApplyK`
```
 def map2K[F[_], G[_], H[_]](af: A[F], ag: A[G])(f: Tuple2K[F, G, ?] ~> H): A[H]
```

`ApplyK` extends both `SemigroupalK` and `FunctorK` just like their lower kinded counterparts.

For tagless final algebras that
1. has no extra type parameters or abstract type members, and
2. whose effect `F` appears only in the covariant position for **all members**,

instance of `ApplyK` can be auto generated through `autoApplyK` annotation.



Their laws are defined in `mainecoon.laws`. To test your instance (if you decide to roll your own) against these laws please follow the examples in `mainecoon.tests`, especially the ones that test against `SafeAlg`.


