---
layout: home
title:  Home
section: home
position: 1
---

Mainecoon is a small library built to facilitate composing tagless final encoded algebras.

[![Typelevel incubator](https://img.shields.io/badge/typelevel-incubator-F51C2B.svg)](http://typelevel.org)
[![Build Status](https://travis-ci.org/kailuowang/mainecoon.svg?branch=master)](https://travis-ci.org/kailuowang/mainecoon)
[![codecov](https://codecov.io/gh/kailuowang/mainecoon/branch/master/graph/badge.svg)](https://codecov.io/gh/kailuowang/mainecoon)
[![Join the chat at https://gitter.im/kailuowang/mainecoon](https://badges.gitter.im/kailuowang/mainecoon.svg)](https://gitter.im/kailuowang/mainecoon?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Scala.js](http://scala-js.org/assets/badges/scalajs-0.6.15.svg)](http://scala-js.org)
[![Latest version](https://index.scala-lang.org/kailuowang/mainecoon/mainecoon-core/latest.svg?color=orange)](https://index.scala-lang.org/kailuowang/mainecoon/mainecoon-core)


## Installation

Mainecoon is available on scala 2.11, 2.12, and scalajs. The macro annotations are developed using [scalameta](http://scalameta.org/), so there are a few dependencies to add in your `build.sbt`.


```scala
addCompilerPlugin(
  ("org.scalameta" % "paradise" % "3.0.0-M9").cross(CrossVersion.full))

libraryDependencies += 
  "com.kailuowang" %% "mainecoon-macros" % latestVersion  //latest version indicated in the badge above
```

Note that `org.scalameta.paradise` is a fork of `org.scalamacros.paradise`. So if you already have the
`org.scalamacros.paradise` dependency, you might need to replace it.

## <a id="auto-transform" href="#auto-transform"></a>Auto-transforming interpreters

Say we have a typical tagless encoded algebra `ExpressionAlg[F[_]]`

```tut:silent
import mainecoon._

@finalAlg
@autoFunctorK
@autoSemigroupalK
@autoProductNK
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

Similar to [simulacrum](https://github.com/mpilquist/simulacrum), `@finalAlg` adds an `apply` method in the companion object so that you can do implicit calling.
```tut:book
ExpressionAlg[Try]
```

Mainecoon provides a [`FunctorK`](typeclasses.html#functorK) type class to map over algebras using [cats](http://typelevel.org/cats)' [`FunctionK`](http://typelevel.org/cats/datatypes/functionk.html).
The `@autoFunctorK` annotation automatically generate an instance of `FunctorK` for `ExpressionAlg` so that you can map
 an `ExpressionAlg[F]` to a `ExpressionAlg[G]` using a `FunctionK[F, G]`, a.k.a. `F ~> G`.

```tut:silent
import mainecoon.implicits._
import cats.implicits._
import cats._
```
```tut:book
implicit val fk : Try ~> Option = λ[Try ~> Option](_.toOption)

tryExpression.mapK(fk)
```
Note that the `Try ~> Option` is implemented using [kind projector's polymorphic lambda syntax](https://github.com/non/kind-projector#polymorphic-lambda-values).   
`@autoFunctorK` also add an auto derivation, so that if you have an implicit  `ExpressionAlg[F]` and an implicit
`F ~> G`, you automatically have a `ExpressionAlg[G]`.

Obviously [`FunctorK`](typeclasses.html#functorK) instance is only possible when the effect type `F[_]` appears only in the
covariant position (i.e. the return types). For algebras with effect type also appearing in the contravariant position (i.e. argument types), mainecoon provides a [`InvariantK`](typeclasses.html#invariantK) type class and an `autoInvariantK` annotation to automatically generate instances.

```tut:book
import ExpressionAlg.autoDerive._

ExpressionAlg[Option]
```
This auto derivation can be turned off using an annotation argument: `@autoFunctorK(autoDerivation = false)`.

## <a id="stack-safe" href="#stack-safe"></a>Make stack safe with `Free`
Another quick win with a `FunctorK` instance is to lift your algebra interpreters to use `Free` to achieve stack safety.

 For example, say you have an interpreter using `Try`

```tut:silent
@finalAlg @autoFunctorK
trait Increment[F[_]] {
  def plusOne(i: Int): F[Int]
}

implicit object incTry extends Increment[Try] {
  def plusOne(i: Int) = Try(i + 1)
}

def program[F[_]: Monad: Increment](i: Int): F[Int] = for {
  j <- Increment[F].plusOne(i)
  z <- if (j < 10000) program[F](j) else Monad[F].pure(j)
} yield z

```
Obviously, this program is not stack safe.
```scala
program[Try](0)
//throws java.lang.StackOverflowError
```
Now lets use auto derivation to lift the interpreter with `Try` into an interpreter with `Free`

```tut:silent
import cats.free.Free
import cats.arrow.FunctionK
import Increment.autoDerive._

implicit def toFree[F[_]]: F ~> Free[F, ?] = λ[F ~> Free[F, ?]](t => Free.liftF(t))
```
```tut:book
program[Free[Try, ?]](0).foldMap(FunctionK.id)
```

Again the magic here is that mainecoon auto derive an `Increment[Free[Try, ?]]` when there is an implicit `Try ~> Free[Try, ?]` and a `Increment[Try]` in scope. This auto derivation can be turned off using an annotation argument: `@autoFunctorK(autoDerivation = false)`.



## <a id="vertical-comp" href="#vertical-comp"></a>Vertical composition

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

Note that the `ExpressionAlg` interpreter needed here is a `ExpressionAlg[Option]`, while we only defined a `ExpressionAlg[Try]`. However since we have a `fk: Try ~> Option` in scope, we can automatically have `ExpressionAlg[Option]` in scope through `autoDerive`. We can just write

```tut:book
import ExpressionAlg.autoDerive._
new StringCalculatorOption
```

## <a id="horizontal-comp" href="#horizontal-comp"></a>Horizontal composition

You can use the [`SemigroupalK`](typeclasses.html#semigroupalK) type class to create a new interpreter that runs two interpreters simultaneously and return the result as a `cats.Tuple2K`. The `@autoSemigroupalK` attribute add an instance of `SemigroupalK` to the companion object. Example:
```tut:book
val prod = ExpressionAlg[Option].productK(ExpressionAlg[Try])
prod.num("2")
```

If you want to combine more than 2 interpreters, the `@autoProductNK` attribute add a series of `product{n}K (n = 3..9)` methods to the companion object.

For example.
```tut:silent

val listInterpreter = ExpressionAlg[Option].mapK(λ[Option ~> List](_.toList))
val vectorInterpreter = listInterpreter.mapK(λ[List ~> Vector](_.toVector))

```
```tut:book
val prod4 = ExpressionAlg.product4K(ExpressionAlg[Try], ExpressionAlg[Option], listInterpreter, vectorInterpreter)

prod4.num("3")

prod4.num("invalid")

```
Unlike `productK` living in the `SemigroupalK` type class, currently we don't have a type class for these `product{n}K` operations yet.


## `@autoFunctor` and `@autoInvariant`

Mainecoon also provides two annotations that can generate `cats.Functor` and `cats.Invariant` instance for your trait.

### `@autoFunctor`
```tut:silent
@finalAlg @autoFunctor
trait SimpleAlg[T] {
  def foo(a: String): T
}

implicit object SimpleAlgInt extends SimpleAlg[Int] {
  def foo(a: String): Int = a.length
}
```
```tut:book
SimpleAlg[Int].map(_ + 1).foo("blah")
```

### `@autoInvariant`
```tut:silent
@finalAlg @autoInvariant
trait SimpleInvAlg[T] {
  def foo(a: T): T
}

implicit object SimpleInvAlgInt extends SimpleInvAlg[String] {
  def foo(a: String): String = a.reverse
}
```
```tut:book
SimpleInvAlg[String].imap(_.toInt)(_.toString).foo(12)
```
Note that if there are multiple type parameters on the trait, `@autoFunctor` and `@autoInvariant` will treat the last one as the target `T`.

