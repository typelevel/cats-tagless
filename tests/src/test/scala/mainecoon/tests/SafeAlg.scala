package mainecoon
package tests

import cats.kernel.Eq
import org.scalacheck.{Arbitrary, Cogen}

import scala.util.Try
import cats.laws.discipline.eq._

@autoFunctorK
trait SafeAlg[F[_]] {
  def parseInt(i: String): F[Int]
  def divide(dividend: Float, divisor: Float): F[Float]
}

object SafeAlg {

  implicit def eqForParseAlg[F[_]](implicit eqF: Eq[F[Int]]): Eq[SafeAlg[F]] =
    Eq.by[SafeAlg[F], String => F[Int]](p => (s: String) => p.parseInt(s))

  implicit def arbitraryParseAlg[F[_]](implicit cS: Cogen[String],
                                       gF: Cogen[Float],
                                       FI: Arbitrary[F[Int]],
                                       FB: Arbitrary[F[Float]]): Arbitrary[SafeAlg[F]] =
    Arbitrary {
      for {
        f1 <- Arbitrary.arbitrary[String => F[Int]]
        f2 <- Arbitrary.arbitrary[(Float, Float) => F[Float]]
      } yield new SafeAlg[F] {
        def parseInt(i: String): F[Int] = f1(i)
        def divide(dividend: Float, divisor: Float): F[Float] = f2(dividend, divisor)
      }
    }
}

object Interpreters {
  type F[T] = Try[T]

  object tryInterpreter extends SafeAlg[F] {
    def parseInt(s: String) = Try(s.toInt)
    def divide(dividend: Float, divisor: Float): F[Float] = Try(dividend / divisor)
  }

}
