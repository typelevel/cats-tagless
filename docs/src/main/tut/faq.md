---
layout: page
title:  FAQ
section: faq
position: 2
---


## FAQs

### Does mainecoon support algebras with extra type parameters?

Yes. e.g.

```tut:silent
import mainecoon._
import cats.~>
import util.Try

@autoFunctorK @finalAlg
trait Foo[F[_], T] {
  def a(i: Int): F[T]
}

implicit val tryFoo: Foo[Try, String] = new Foo[Try, String] {
  def a(i: Int) = Try(i.toString)
}

implicit val fk: Try ~> Option = λ[Try ~> Option](_.toOption)
```
```tut:book

Foo[Option, String].a(3)
```

### Does mainecoon support algebras with abstract type member?

Yes but with some caveats.
The `FunctorK` instance it generates does not refine to the type member. E.g.

```tut:silent
 @autoFunctorK @finalAlg
  trait Bar[F[_]] {
    type T
    def a(i: Int): F[T]
  }
  implicit val tryInt = new Bar[Try] {
     type T = String
     def a(i: Int): Try[String] = Try(i.toString)
  }
```

If you try to map this tryInt to a `Bar[Option]`, the `type T` of the `Bar[Option]` isn't refined.  That is, you can do

```tut:book
Bar[Option].a(3)
```
But you can't create a `Bar[Option]{ type T = String }` from the `tryInt` using `FunctorK`.

```tut:fail
val barOption: Bar[Option] { type T = String } = tryInt.mapK(fk)
```

However, there is also `mapK` function added to the companion object of the algebra which gives you more precise type.

```tut:book
val barOption: Bar[Option] { type T = String } = Bar.mapK(tryInt)(fk)
```

### I am seeing diverging implicit expansion for type MyAlgebra[F]

If you see error likes the following when you try to summon a specific instance of `MyAlgebra`

> diverging implicit expansion for type MyAlgebra[F]
>
> [error] starting with method autoDeriveFromFunctorK in object MyAlgebra

It probably means that necessary implicit `MyAlgebra` instance and/or the corresponding `FunctionK` is missing in scope. 
