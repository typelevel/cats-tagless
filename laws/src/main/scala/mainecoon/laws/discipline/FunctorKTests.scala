package mainecoon
package laws
package discipline

import org.scalacheck.{Arbitrary, Prop}
import Prop._
import cats.{Eq, ~>}
import org.typelevel.discipline.Laws
import cats.laws.discipline._

trait FunctorKTests[F[_[_]]] extends InvariantKTests[F] {
  def laws: FunctorKLaws[F]

  def functorK[A[_], B[_], C[_], T: Arbitrary](implicit
                                               ArbFA: Arbitrary[F[A]],
                                               ArbitraryG: Arbitrary[A[T]],
                                               ArbitraryH: Arbitrary[B[T]],
                                               ArbitraryI: Arbitrary[C[T]],
                                               ArbitraryFK: Arbitrary[A ~> B],
                                               ArbitraryFK2: Arbitrary[B ~> C],
                                               ArbitraryFK3: Arbitrary[B ~> A],
                                               ArbitraryFK4: Arbitrary[C ~> B],
                                               EqFA: Eq[F[A]],
                                               EqFC: Eq[F[C]]
                                              ): RuleSet = {
    new DefaultRuleSet(
      name = "functorK",
      parent = Some(invariantK[A, B, C]),
      "covariant identity" -> forAll(laws.covariantIdentity[A] _),
      "covariant composition" -> forAll(laws.covariantComposition[A, B, C] _))
  }
}

object FunctorKTests {
  def apply[F[_[_]]: FunctorK]: FunctorKTests[F] =
    new FunctorKTests[F] { def laws: FunctorKLaws[F] = FunctorKLaws[F] }
}
