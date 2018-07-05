[![Typelevel incubator](https://img.shields.io/badge/typelevel-incubator-F51C2B.svg)](http://typelevel.org)
[![Build Status](https://travis-ci.org/kailuowang/mainecoon.svg?branch=master)](https://travis-ci.org/kailuowang/mainecoon)
[![codecov](https://codecov.io/gh/kailuowang/mainecoon/branch/master/graph/badge.svg)](https://codecov.io/gh/kailuowang/mainecoon)
[![Join the chat at https://gitter.im/kailuowang/mainecoon](https://badges.gitter.im/kailuowang/mainecoon.svg)](https://gitter.im/kailuowang/mainecoon?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Scala.js](http://scala-js.org/assets/badges/scalajs-0.6.15.svg)](http://scala-js.org)
[![Latest version](https://index.scala-lang.org/kailuowang/mainecoon/mainecoon-core/latest.svg?color=orange)](https://index.scala-lang.org/kailuowang/mainecoon/mainecoon-core)


Mainecoon is a small library built to facilitate transforming and composing tagless final encoded algebras.


## Installation

Mainecoon is available on scala 2.11, 2.12, and scalajs. The macro annotations are developed using [scalameta](http://scalameta.org/), so there are a few dependencies to add in your `build.sbt`.

```scala
addCompilerPlugin(
  ("org.scalameta" % "paradise" % "3.0.0-M11").cross(CrossVersion.full))

libraryDependencies += 
  "com.kailuowang" %% "mainecoon-macros" % latestVersion  //latest version indicated in the badge above
```
Note that `org.scalameta.paradise` is a fork of `org.scalamacros.paradise`. So if you already have the
`org.scalamacros.paradise` dependency, you might need to replace it.


## <a id="auto-transform" href="#auto-transform"></a>Auto-transforming tagless final interpreters

Say we have a typical tagless encoded algebra `ExpressionAlg[F[_]]`

```scala
import mainecoon._

@autoFunctorK
trait ExpressionAlg[F[_]] {
  def num(i: String): F[Float]
  def divide(dividend: Float, divisor: Float): F[Float]
}
```
With Mainecoon you can transform this interpreter using [Cats](http://typelevel.org/cats)' [`FunctionK`](http://typelevel.org/cats/datatypes/functionk.html). I.e, you can transform an `ExpressionAlg[F]` to a `ExpressionAlg[G]` using a `FunctionK[F, G]`, a.k.a. `F ~> G`.

For example, if you have an interpreter of `ExpressionAlg[Try]`

```scala
import util.Try

object tryExpression extends ExpressionAlg[Try] {
  def num(i: String) = Try(i.toFloat)
  def divide(dividend: Float, divisor: Float) = Try(dividend / divisor)
}
```
You can transform it to an interpreter of `ExpressionAlg[Option]` 
```scala
import mainecoon.implicits._
import cats.implicits._
import cats._

val fk : Try ~> Option = λ[Try ~> Option](_.toOption)

tryExpression.mapK(fk)
// res0: ExpressionAlg[Option]
```
Note that the `Try ~> Option` is implemented using [kind projector's polymorphic lambda syntax](https://github.com/non/kind-projector#polymorphic-lambda-values).   


Obviously [`FunctorK`](typeclasses.html#functorK) instance is only possible when the effect type `F[_]` appears only in the
covariant position (i.e. the return types). For algebras with effect type also appearing in the contravariant position (i.e. argument types), mainecoon provides a [`InvariantK`](typeclasses.html#invariantK) type class and an `autoInvariantK` annotation to automatically generate instances.

`@autoFunctorK` also add an auto implicit derivation, so that if you have an implicit  `ExpressionAlg[F]` and an implicit
`F ~> G`, you can automatically have a `ExpressionAlg[G]`.
It works like this 
```scala
import ExpressionAlg.autoDerive._

implicitly[ExpressionAlg[Option]]  //implicitly derived from a `ExpressionAlg[Try]` and a `Try ~> Option`
```
This auto derivation can be turned off using an annotation argument: `@autoFunctorK(autoDerivation = false)`.

## <a id="stack-safe" href="#stack-safe"></a>Quick example: make stack safe with `Free`
With mainecoon, you can lift your algebra interpreters to use `Free` to achieve stack safety.

 For example, say you have an interpreter using `Try`

```scala
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

```scala
import cats.free.Free
import cats.arrow.FunctionK
import Increment.autoDerive._

implicit def toFree[F[_]]: F ~> Free[F, ?] = λ[F ~> Free[F, ?]](t => Free.liftF(t))

program[Free[Try, ?]](0).foldMap(FunctionK.id)
// res9: scala.util.Try[Int] = Success(10000)
```

Again the magic here is that mainecoon auto derive an `Increment[Free[Try, ?]]` when there is an implicit `Try ~> Free[Try, ?]` and a `Increment[Try]` in scope. This auto derivation can be turned off using an annotation argument: `@autoFunctorK(autoDerivation = false)`.


## <a id="horizontal-comp" href="#horizontal-comp"></a>Horizontal composition with `@autoSemigroupalK`

You can use the [`SemigroupalK`](typeclasses.html#semigroupalK) type class to create a new interpreter that runs both interpreters and return the result as a `cats.Tuple2K`. The `@autoSemigroupalK` attribute add an instance of `SemigroupalK` to the companion object. Example:

```scala
@autoSemigroupalK
trait ExpressionAlg[F[_]] {
  def num(i: String): F[Float]
  def divide(dividend: Float, divisor: Float): F[Float]
}


val prod = tryExpression.productK(optionExpression)
prod.num("2")
// res11: cats.data.Tuple2K[Option,scala.util.Try,Float] = Tuple2K(Some(2.0),Success(2.0))
```

If you want to combine more than 2 interpreters, the `@autoProductNK` attribute add a series of `product{n}K (n = 3..9)` methods to the companion object. Unlike `productK` living in the `SemigroupalK` type class, currently we don't have a type class for these `product{n}K` operations yet.


## `@autoFunctor` and `@autoInvariant`

Mainecoon also provides two annotations that can generate `cats.Functor` and `cats.Invariant` instance for your trait.

#### For documentation/FAQ/guides, go to [kailuowang.com/mainecoon](http://kailuowang.com/mainecoon).

### Community
Any contribution is more than welcome. Also feel free to report bugs, request features using github issues or gitter. 

Discussion around mainecoon is encouraged in the
[Gitter channel](https://gitter.im/kailuowang/mainecoon) as well as on Github issue and PR pages.

We adopted the
[Typelevel Code of Conduct](http://typelevel.org/conduct.html). People are expected to follow it when
discussing mainecoon on the Github page, Gitter channel, or other venues.

### Copyright

Copyright (C) 2017 Kailuo Wang [http://kailuowang.com](http://kailuowang.com)

### License 

Mainecoon is licensed under the Apache License 2.0
