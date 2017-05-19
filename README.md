[![Build Status](https://travis-ci.org/kailuowang/mainecoon.svg?branch=master)](https://travis-ci.org/kailuowang/mainecoon)
[![codecov](https://codecov.io/gh/kailuowang/mainecoon/branch/master/graph/badge.svg)](https://codecov.io/gh/kailuowang/mainecoon)
[![Join the chat at https://gitter.im/kailuowang/mainecoon](https://badges.gitter.im/kailuowang/mainecoon.svg)](https://gitter.im/kailuowang/mainecoon?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Latest version](https://index.scala-lang.org/kailuowang/mainecoon/mainecoon-core/latest.svg?color=orange)](https://index.scala-lang.org/kailuowang/mainecoon/mainecoon-core)
[![Scala.js](http://scala-js.org/assets/badges/scalajs-0.6.16.svg)](http://scala-js.org)

Mainecoon is a small library built to facilitate transforming and composing tagless final encoded algebras.

Here is a quick example of how mainecoon can help:

Say we have a typical tagless encoded algebra `ExpressionAlg[F[_]]`, with an interpreter implemented using `Try`

```scala
import mainecoon._
import util.Try

@finalAlg @autoFunctorK
trait ExpressionAlg[F[_]] {
  def num(i: String): F[Float]
  def divide(dividend: Float, divisor: Float): F[Float]
}

implicit object tryExpression extends ExpressionAlg[Try] {
  def num(i: String) = Try(i.toFloat)
  def divide(dividend: Float, divisor: Float) = Try(dividend / divisor)
}
```
With the `@autoFunctorK` annotation you can map
 an `ExpressionAlg[F]` to a `ExpressionAlg[G]`, using a `FunctionK[F, G]`, a.k.a. `F ~> G`.
```scala
import mainecoon.implicits._
import cats.~>

val fk : Try ~> Option = λ[Try ~> Option](_.toOption)

tryExpression.mapK(fk)
// res3: ExpressionAlg[Option] = ExpressionAlg$$anon$4@14dd7f4
```

Mainecoon also provides auto instance derivation for `InvariantK` and `CartesianK`.

#### For documentation/FAQ/guides, go to [kailuowang.com/mainecoon](http://kailuowang.com/mainecoon).

### Community

Discussion around mainecoon is encouraged in the
[Gitter channel](https://gitter.im/kailuowang/mainecoon) as well as on Github issue and PR pages.

We adopted the
[Typelevel Code of Conduct](http://typelevel.org/conduct.html). People are expected to follow it when
discussing mainecoon on the Github page, Gitter channel, or other venues.

### Copyright

Copyright (C) 2017 Kailuo Wang [http://kailuowang.com](http://kailuowang.com)
