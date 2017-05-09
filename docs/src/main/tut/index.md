---
layout: home
title:  "Home"
section: "home"
---


Mainecoon is a small library built to facilitate composing final tagless endcoded algebras.


## Vertical composition

```tut:silent
import mainecoon._
```

```tut:book

@finalAlg @autoFunctorK
trait SafeAlg[F[_]] {
  def num(i: String): F[Float]
  def divide(dividend: Float, divisor: Float): F[Float]
}


trait Calculator[F[_]] {
  def calc(i: String): F[Float]
}


```
## Type classes

There are three type classes that are higher kinded version of the corresponding type class in cats.

1. `InvariantK`

2. `FunctorK`

3. `CartesianK`



