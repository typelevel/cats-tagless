---
layout: home
title:  Home
section: home
position: 1
---


Mainecoon is a small library built to facilitate composing tagless final encoded algebras.

## Installation

Mainecoon is developed using [scalameta](http://scalameta.org/), so there are a few dependencies to add in your `build.sbt`.

```scala
resolvers += Resolver.bintrayRepo("kailuowang", "maven")

addCompilerPlugin(
  ("org.scalameta" % "paradise" % "3.0.0-M8").cross(CrossVersion.full)
)

libraryDependencies ++= Seq(
  "org.scalameta" %% "scalameta" % "1.7.0" % Provided,
  "com.kailuowang" %% "mainecoon-macros" % "0.0.3"
)
```

## Transforming Interpreters

Say we have a typical tagless encoded algebra `ExpressionAlg[F[_]]`

```tut:silent
import mainecoon._

@finalAlg @autoFunctorK @autoCartesianK
trait ExpressionAlg[F[_]] {
  def num(i: String): F[Float]
  def divide(dividend: Float, divisor: Float): F[Float]
}
```
with an interpreter implemented using `Try`

```tut:silent
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
 an `ExpressionAlg[F]` to a `ExpressionAlg[G]`, if you have a `FunctionK[F, G]`, a.k.a. `F ~> G`.
```tut:silent
import mainecoon.implicits._
import cats.implicits._
import cats._
```
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

```tut:silent
trait StringCalculatorAlg[F[_]] {
  def calc(i: String): F[Float]
}
```

When writing interpreter for this one, we can call for an interpreter for `ExpressionAlg`.

```tut:silent
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

Note that the `ExpressionAlg` interpreter needed here is a `ExpressionAlg[Option]`, while we only defined a `ExpressionAlg[Try]`. However since we have a `fk: Try ~> Option` in scope, we automatically have `ExpressionAlg[Option]` in scope. We can just write

```tut:book
new StringCalculatorOption
```

## Horizontal composition

You can use `CartesianK` to create a new interpreter that runs two interpreters simultaneously and return the result as a `cats.Prod`. The `@autoCartesianK` attribute add an instance of `CartesianK` to the companion object. Example:
```tut:book
val prod = ExpressionAlg[Option].productK(ExpressionAlg[Try])
prod.num("2")
```




