---
layout: home
title:  Home
section: home
position: 1
---

Cats-tagless is a small library built to facilitate composing tagless final encoded algebras.

[![Typelevel library](https://img.shields.io/badge/typelevel-library-green.svg)](https://typelevel.org/projects#cats)
[![Build status](https://github.com/typelevel/cats-tagless/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/typelevel/cats-tagless/actions)
[![Gitter channel](https://badges.gitter.im/typelevel/cats-tagless.svg)](https://gitter.im/typelevel/cats-tagless)
[![Scala.js](http://scala-js.org/assets/badges/scalajs-1.5.0.svg)](http://scala-js.org)
[![Latest version](https://index.scala-lang.org/typelevel/cats-tagless/cats-tagless-core/latest.svg?color=orange)](https://index.scala-lang.org/typelevel/cats-tagless/cats-tagless-core)
[![Cats friendly](https://typelevel.org/cats/img/cats-badge-tiny.png)](https://typelevel.org/cats)


## Installation

Cats-tagless is available on scala 2.12, 2.13 and Scala.js.
Add the following dependency in `build.sbt`

```scala
// latest version indicated in the badge above
libraryDependencies += "org.typelevel" %% "cats-tagless-macros" % latestVersion
  
// For Scala 2.13, enable macro annotations
scalacOptions += "-Ymacro-annotations"

// For Scala 2.12, scalamacros paradise is needed as well.
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
```

## <a id="auto-transform" href="#auto-transform"></a>Auto-transforming interpreters

Say we have a typical tagless encoded algebra `ExpressionAlg[F[_]]`

```scala mdoc:silent
import cats.tagless._

@finalAlg
@autoFunctorK
@autoSemigroupalK
@autoProductNK
trait ExpressionAlg[F[_]] {
  def num(i: String): F[Float]
  def divide(dividend: Float, divisor: Float): F[Float]
}
```

With an interpreter implemented using `Try`

```scala mdoc:silent
import util.Try

implicit object tryExpression extends ExpressionAlg[Try] {
  def num(i: String) = Try(i.toFloat)
  def divide(dividend: Float, divisor: Float) = Try(dividend / divisor)
}
```

Similar to [simulacrum](https://github.com/mpilquist/simulacrum), `@finalAlg` adds an `apply` method in the companion object so that you can do implicit calling.

```scala mdoc
ExpressionAlg[Try]
```

Cats-tagless provides a [FunctorK](typeclasses.html#functork) type class to map over algebras using [cats](http://typelevel.org/cats)' [FunctionK](http://typelevel.org/cats/datatypes/functionk.html).
More specifically With an instance of `FunctorK[ExpressionAlg]`, you can transform an `ExpressionAlg[F]` to a `ExpressionAlg[G]` using a `FunctionK[F, G]`, a.k.a. `F ~> G`.

The `@autoFunctorK` annotation adds the following line (among some other code) in the companion object.

```scala
object ExpressionAlg {
  implicit def functorKForExpressionAlg: FunctorK[ExpressionAlg] =
    Derive.functorK[ExpressionAlg]
}
```

This `functorKForExpressionAlg` is a `FunctorK` instance for `ExpressionAlg` generated using `cats.tagless.Derive.functorK`. Note that the usage of `@autoFunctorK`, like all other `@autoXXXX` annotations provided by cats-tagless, is optional, you can manually add this instance yourself.

With this implicit instance in scope, you can call the syntax `.mapK` method to perform the transformation.

```scala mdoc:silent
import cats.tagless.implicits._
import cats.implicits._
import cats._
```

```scala mdoc
implicit val fk : Try ~> Option = λ[Try ~> Option](_.toOption)

tryExpression.mapK(fk)
```

Note that the `Try ~> Option` is implemented using [kind projector's polymorphic lambda syntax](https://github.com/non/kind-projector#polymorphic-lambda-values).
`@autoFunctorK` also add an auto derivation, so that if you have an implicit  `ExpressionAlg[F]` and an implicit
`F ~> G`, you automatically have a `ExpressionAlg[G]`.

Obviously [FunctorK](typeclasses.html#functork) instance is only possible when the effect type `F[_]` appears only in the
covariant position (i.e. the return types). For algebras with effect type also appearing in the contravariant position (i.e. argument types), Cats-tagless provides a [InvariantK](typeclasses.html#invariantk) type class and an `autoInvariantK` annotation to automatically generate instances.

```scala mdoc
import ExpressionAlg.autoDerive._
ExpressionAlg[Option]
```

This auto derivation can be turned off using an annotation argument: `@autoFunctorK(autoDerivation = false)`.

## <a id="stack-safe" href="#stack-safe"></a>Example: stack safety with `Free`
Another quick win with a `FunctorK` instance is to lift your algebra interpreters to use `Free` to achieve stack safety.

For example, say you have an interpreter using `Try`

```scala mdoc:silent
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

```scala mdoc:compile-only
program[Try](0)
// java.lang.StackOverflowError
```

Now lets use auto derivation to lift the interpreter with `Try` into an interpreter with `Free`

```scala mdoc:silent
import cats.free.Free
import cats.arrow.FunctionK
import Increment.autoDerive._

implicit def toFree[F[_]]: F ~> Free[F, *] = λ[F ~> Free[F, *]](t => Free.liftF(t))
```

```scala mdoc
program[Free[Try, *]](0).foldMap(FunctionK.id)
```

Again the magic here is that Cats-tagless auto derive an `Increment[Free[Try, *]]` when there is an implicit `Try ~> Free[Try, *]` and a `Increment[Try]` in scope. This auto derivation can be turned off using an annotation argument: `@autoFunctorK(autoDerivation = false)`.

## <a id="vertical-comp" href="#vertical-comp"></a>Vertical composition

Say you have another algebra that could use the `ExpressionAlg`.

```scala mdoc:silent
trait StringCalculatorAlg[F[_]] {
  def calc(i: String): F[Float]
}
```

When writing interpreter for this one, we can call for an interpreter for `ExpressionAlg`.

```scala mdoc:silent
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

```scala mdoc
new StringCalculatorOption
```

## <a id="horizontal-comp" href="#horizontal-comp"></a>Horizontal composition

You can use the [SemigroupalK](typeclasses.html#semigroupalk) type class to create a new interpreter that runs two interpreters simultaneously and return the result as a `cats.Tuple2K`. The `@autoSemigroupalK` attribute add an instance of `SemigroupalK` to the companion object. Example:

```scala mdoc
val prod = ExpressionAlg[Option].productK(ExpressionAlg[Try])
prod.num("2")
```

If you want to combine more than 2 interpreters, the `@autoProductNK` attribute add a series of `product{n}K (n = 3..9)` methods to the companion object.

For example.

```scala mdoc:silent
val listInterpreter = ExpressionAlg[Option].mapK(λ[Option ~> List](_.toList))
val vectorInterpreter = listInterpreter.mapK(λ[List ~> Vector](_.toVector))
```

```scala mdoc
val prod4 = ExpressionAlg.product4K(ExpressionAlg[Try], ExpressionAlg[Option], listInterpreter, vectorInterpreter)

prod4.num("3")

prod4.num("invalid")
```

Unlike `productK` living in the `SemigroupalK` type class, currently we don't have a type class for these `product{n}K` operations yet.

## `@autoFunctor` and `@autoInvariant`

Cats-tagless also provides three derivations that can generate `cats.Functor`, `cats.FlatMap` and `cats.Invariant` instance for your trait.

### `@autoFunctor`

```scala mdoc:silent
@finalAlg @autoFunctor
trait SimpleAlg[T] {
  def foo(a: String): T
  def bar(d: Double): Double
}

implicit object SimpleAlgInt extends SimpleAlg[Int] {
  def foo(a: String): Int = a.length
  def bar(d: Double): Double = 2 * d
}
```

```scala mdoc
SimpleAlg[Int].map(_ + 1).foo("blah")
```

Methods which return not the effect type are unaffected by the `map` function.

```scala mdoc
SimpleAlg[Int].map(_ + 1).bar(2)
```
### `@autoFlatMap`

```scala mdoc:silent
@autoFlatMap
trait StringAlg[T] {
  def foo(a: String): T
}

object LengthAlg extends StringAlg[Int] {
  def foo(a: String): Int = a.length
}

object HeadAlg extends StringAlg[Char] {
  def foo(a: String): Char = a.headOption.getOrElse(' ')
}

val hintAlg = for {
  length <- LengthAlg
  head <- HeadAlg
} yield head.toString ++ "*" * (length - 1)
```

```scala mdoc
hintAlg.foo("Password")
```

### `@autoInvariant`

```scala mdoc:silent
@finalAlg @autoInvariant
trait SimpleInvAlg[T] {
  def foo(a: T): T
}

implicit object SimpleInvAlgString extends SimpleInvAlg[String] {
  def foo(a: String): String = a.reverse
}
```

```scala mdoc
SimpleInvAlg[String].imap(_.toInt)(_.toString).foo(12)
```

### `@autoContravariant`

```scala mdoc:silent
@finalAlg @autoContravariant
trait SimpleContraAlg[T] {
  def foo(a: T): String
}

implicit object SimpleContraAlgString extends SimpleContraAlg[String] {
  def foo(a: String): String = a.reverse
}
```

```scala mdoc
SimpleContraAlg[String].contramap[Int](_.toString).foo(12)
```

Note that if there are multiple type parameters on the trait, `@autoFunctor`, `@autoInvariant`, `@autoContravariant` will treat the last one as the target `T`.
