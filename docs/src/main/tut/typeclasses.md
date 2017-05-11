---
layout: page
title:  Type classes
section: typeclasses
position: 4
---


## Type classes

There are three type classes that are higher kinded version of the corresponding type class in cats.

1. `InvariantK`

2. `FunctorK`

3. `CartesianK`

Their laws are defined in `mainecoon.laws`. To test your instance (if you decide to roll your own) against these laws please follow the examples in `mainecoon.tests`, especially the ones that test against `SafeAlg`.
