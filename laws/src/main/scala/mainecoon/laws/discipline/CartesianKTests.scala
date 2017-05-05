package mainecoon
package laws
package discipline


import cats.Eq
import cats.data.Prod
import mainecoon.laws.discipline.CartesianKTests.IsomorphismsK
import org.scalacheck.Prop._
import org.scalacheck.{Arbitrary, Prop}
import org.typelevel.discipline.Laws

trait CartesianKTests[A[_[_]]] extends Laws {
  def laws: CartesianKLaws[A]

  def CartesianK[F[_], G[_], H[_]](implicit
                                               ArbCF: Arbitrary[A[F]],
                                               ArbCG: Arbitrary[A[G]],
                                               ArbCH: Arbitrary[A[H]],
                                               iso: IsomorphismsK[A],
                                               EqFGH: Eq[A[λ[T => (F[T], G[T], H[T])]]]
                                              ): RuleSet = {
    new DefaultRuleSet(
      name = "CartesianK",
      parent = None,
      "cartesian associativity" -> forAll((af: A[F], ag: A[G], ah: A[H]) => iso.associativity(laws.cartesianAssociativity[F, G, H](af, ag, ah))))
  }
}


object CartesianKTests {
  def apply[A[_[_]]: CartesianK]: CartesianKTests[A] =
    new CartesianKTests[A] { def laws: CartesianKLaws[A] = CartesianKLaws[A] }

  trait IsomorphismsK[A[_[_]]] {
    def associativity[F[_], G[_], H[_]](fs: (A[Prod[F, Prod[G, H, ?], ?]], A[Prod[Prod[F, G, ?], H, ?]]))
                                       (implicit EqFGH: Eq[A[λ[T => (F[T], G[T], H[T])]]]): Prop
  }
}
