[![Typelevel library](https://img.shields.io/badge/typelevel-library-green.svg)](https://typelevel.org/projects#cats)
[![Build status](https://github.com/typelevel/cats-tagless/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/typelevel/cats-tagless/actions)
[![Gitter channel](https://badges.gitter.im/typelevel/cats-tagless.svg)](https://gitter.im/typelevel/cats-tagless)
[![Scala.js](http://scala-js.org/assets/badges/scalajs-1.5.0.svg)](http://scala-js.org)
[![Latest version](https://index.scala-lang.org/typelevel/cats-tagless/cats-tagless-core/latest.svg?color=orange)](https://index.scala-lang.org/typelevel/cats-tagless/cats-tagless-core)
[![Cats friendly](https://typelevel.org/cats/img/cats-badge-tiny.png)](https://typelevel.org/cats)


Cats-tagless is a small library built to facilitate transforming and composing tagless final encoded algebras.


## Installation

Cats-tagless is currently available for Scala 2.12 and 2.13 and Scala.js.

Add the following settings in `build.sbt`

```scala
libraryDependencies += 
  "org.typelevel" %% "cats-tagless-macros" % latestVersion  //latest version indicated in the badge above

Compile / scalacOptions ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, n)) if n >= 13 => "-Ymacro-annotations" :: Nil
    case _ => Nil
  }
}

libraryDependencies ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, n)) if n >= 13 => Nil
    case _ => compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full) :: Nil
  }
}
```

## <a id="auto-transform" href="#auto-transform"></a>Auto-transforming tagless final interpreters

Say we have a typical tagless encoded algebra `ExpressionAlg[F[_]]`

```scala
import cats.tagless._

@autoFunctorK
trait ExpressionAlg[F[_]] {
  def num(i: String): F[Float]
  def divide(dividend: Float, divisor: Float): F[Float]
}
```

With Cats-tagless you can transform this interpreter using [Cats](http://typelevel.org/cats)' [`FunctionK`](http://typelevel.org/cats/datatypes/functionk.html), i.e, you can transform an `ExpressionAlg[F]` to an `ExpressionAlg[G]` using a `FunctionK[F, G]`, a.k.a. `F ~> G`. Cats-tagless generates a `FunctorK` instance for your algebra.

The `@autoFunctorK` annotation adds the following line (among some other code) in the companion object.  
```scala
object ExpressionAlg {
  implicit def functorKForExpressionAlg: FunctorK[ExpressionAlg] =
      Derive.functorK[ExpressionAlg]
}
```
This `functorKForExpressionAlg` is a `FunctorK` instance for `ExpressionAlg` which can map a `ExpressionAlg[F]` to a `ExpressionAlg[G]` using a `FunctionK[F, G]`.


Note that the usage of `@autoFunctorK`, like all other `@autoXXXX` annotations provided by cats-tagless, is optional, you can manually add this instance yourself.

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
import cats.tagless.implicits._
import cats.implicits._
import cats._

val fk : Try ~> Option = λ[Try ~> Option](_.toOption)

tryExpression.mapK(fk)
// res0: ExpressionAlg[Option]
```
Note that the `Try ~> Option` is implemented using [kind projector's polymorphic lambda syntax](https://github.com/non/kind-projector#polymorphic-lambda-values).   


Obviously, [`FunctorK`](typeclasses.html#functorK) instance is only possible when the effect type `F[_]` appears only in the
covariant position (i.e. the return types). For algebras with effect type also appearing in the contravariant position (i.e. argument types), Cats-tagless provides a [`InvariantK`](typeclasses.html#invariantK) type class and an `autoInvariantK` annotation to automatically generate instances.

`@autoFunctorK` also add an auto implicit derivation, so that if you have an implicit  `ExpressionAlg[F]` and an implicit
`F ~> G`, you can automatically have a `ExpressionAlg[G]`.
It works like this 
```scala
import ExpressionAlg.autoDerive._

implicitly[ExpressionAlg[Option]]  //implicitly derived from a `ExpressionAlg[Try]` and a `Try ~> Option`
```
This auto derivation can be turned off using an annotation argument: `@autoFunctorK(autoDerivation = false)`.

## <a id="stack-safe" href="#stack-safe"></a>Quick example: make stack safe with `Free`
With Cats-tagless, you can lift your algebra interpreters to use `Free` to achieve stack safety.

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
Now, let's use auto derivation to lift the interpreter with `Try` into an interpreter with `Free`

```scala
import cats.free.Free
import cats.arrow.FunctionK
import Increment.autoDerive._

implicit def toFree[F[_]]: F ~> Free[F, *] = λ[F ~> Free[F, *]](t => Free.liftF(t))

program[Free[Try, *]](0).foldMap(FunctionK.id)
// res9: scala.util.Try[Int] = Success(10000)
```

Again, the magic here is that Cats-tagless auto derive an `Increment[Free[Try, *]]` when there is an implicit `Try ~> Free[Try, *]` and a `Increment[Try]` in scope. This auto derivation can be turned off using an annotation argument: `@autoFunctorK(autoDerivation = false)`.


## <a id="horizontal-comp" href="#horizontal-comp"></a>Horizontal composition with `@autoSemigroupalK`

You can use the [`SemigroupalK`](typeclasses.html#semigroupalK) type class to create a new interpreter that runs both interpreters and return the result as a `cats.Tuple2K`. The `@autoSemigroupalK` attribute adds an instance of `SemigroupalK` to the companion object. Example:

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

If you want to combine more than 2 interpreters, the `@autoProductNK` attribute adds a series of `product{n}K (n = 3..9)` methods to the companion object. Unlike `productK` living in the `SemigroupalK` type class, currently we don't have a type class for these `product{n}K` operations yet.


## `@autoFunctor`, `@autoInvariant` and `@autoContravariant`

Cats-tagless also provides three annotations that can generate `cats.Functor`, `cats.Invariant` `cats.Contravariant` instance for traits.

#### For documentation/FAQ/guides, go to [typelevel.github.io/cats-tagless](https://typelevel.github.io/cats-tagless).

### Community
Any contribution is more than welcome. Also feel free to report bugs, request features using github issues or gitter. 

Discussion around Cats-tagless is encouraged in the
[Gitter channel](https://gitter.im/typelevel/cats-tagless) as well as on Github issue and PR pages.

We adopted the
[Scala Code of Conduct](https://www.scala-lang.org/conduct/). People are expected to follow it when
discussing Cats-tagless on the Github page, Gitter channel, or other venues.

### Maintainers
 
* [Georgi Krastev](https://github.com/joroKr21)
* [Kailuo Wang](https://github.com/kailuowang)
* [Luka Jacobowitz](https://github.com/LukaJCB)

### Copyright

Copyright (C) 2019 Maintainers of Cats-tagless

### License 

Cats-tagless is licensed under the Apache License 2.0
