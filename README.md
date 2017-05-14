[![Build Status](https://travis-ci.org/kailuowang/mainecoon.svg?branch=master)](https://travis-ci.org/kailuowang/mainecoon)
[![codecov](https://codecov.io/gh/kailuowang/mainecoon/branch/master/graph/badge.svg)](https://codecov.io/gh/kailuowang/mainecoon)
[![Join the chat at https://gitter.im/kailuowang/mainecoon](https://badges.gitter.im/kailuowang/mainecoon.svg)](https://gitter.im/kailuowang/mainecoon?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Mainecoon is a small library built to facilitate composing tagless final encoded algebras.

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
 an `ExpressionAlg[F]` to a `ExpressionAlg[G]`, if you have a `FunctionK[F, G]`, a.k.a. `F ~> G`.
```scala
import mainecoon.implicits._
import cats._

implicit val fk : Try ~> Option = λ[Try ~> Option](_.toOption)

tryExpression.mapK(fk)
// res3: ExpressionAlg[Option] = ExpressionAlg$$anon$4@14dd7f4
```

In fact, `@finalAlg` also add implicit calling apply and an auto derivation for you, so that if you have an implicit  `ExpressionAlg[F]` and an implicit
`F ~> G`, you automatically have a `ExpressionAlg[G]`

```scala
ExpressionAlg[Option]
// res3: ExpressionAlg[Option] = ExpressionAlg$$anon$4@14dd7f4
```

### For more detail checkout the website [kailuowang.com/mainecoon](http://kailuowang.com/mainecoon).
