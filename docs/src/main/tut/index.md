---
layout: home
title:  "Home"
section: "home"
---


Mainecoon is a small library built to facilitate composing tagless final encoded algebras.
## Transforming Interpreters

Say we have a typical tagless encoded algebra `ExpressionAlg[F[_]]`

```tut:silent
import mainecoon._
import mainecoon.implicits._
import cats.implicits._
import cats._
```

```tut:book

@finalAlg @autoFunctorK
trait ExpressionAlg[F[_]] {
  def num(i: String): F[Float]
  def divide(dividend: Float, divisor: Float): F[Float]
}

```
Now let's write a Try
```tut:book
import util.Try

implicit object tryExpression extends ExpressionAlg[Try] {
  def num(i: String) = Try(i.toFloat)
  def divide(dividend: Float, divisor: Float) = Try(dividend / divisor)
}
```

Similar to [simularcum], `@finalAlg` adds an `apply` method in the companion object so that you can do implicit calling.
```tut:book
ExpressionAlg[Try]
```

The `@autoFunctorK` annotation adds a `FunctorK` instance for `ExpressionAlg` so that you can map
 an `ExpressionAlg[F]` to a `ExpressionAlg[G]`, if you have a `FunctionK[F, G]` (or with symbols `F ~> G`)

```tut:book

implicit val fk : Try ~> Option = λ[Try ~> Option](_.toOption)

tryExpression.mapK(fk)

```
In fact, `@finalAlg` also add an auto derivation, so that if you have an implicit  `ExpressionAlg[F]` and an implicit
`F ~> G`, you automatically have a `ExpressionAlg[G]`

```tut:book
ExpressionAlg[Option]
```


## Vertical composition

Say you have another algebra that could use the `ExpressionAlg`.

```tut:book
trait StringCalculatorAlg[F[_]] {
  def calc(i: String): F[Float]
}
```

When writing interpreter for this one, we can call for an interpreter for `ExpressionAlg`.

```tut:book

class StringCalculatorOption(implicit exp: ExpressionAlg[Option]) extends StringCalculatorAlg[Option] {
  def calc(i: String): Option[Float] = {
    val numbers = i.split("/")
    for {
      s1 <- numbers.headOption
      f1 <- exp.num(s1)
      s2 <- numbers.lift(1)
      f2 <- exp.num(s2)
      r <- exp.divide(f1, f2)
    } yield r
  }
}

```


## Type classes

There are three type classes that are higher kinded version of the corresponding type class in cats.

1. `InvariantK`

2. `FunctorK`

3. `CartesianK`



