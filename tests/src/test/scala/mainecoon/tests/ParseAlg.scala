package mainecoon
package tests

import cats.kernel.Eq
import org.scalacheck.{Arbitrary, Cogen}

import scala.util.{Success, Try}
import cats.laws.discipline.eq._

@autoFunctorK
trait ParseAlg[F[_]] {
  def parseInt(i: String): F[Int]
  def floatToString(f: Float, fmt: String): F[String]
}

object ParseAlg {

  implicit def eqForParseAlg[F[_]](implicit eqF: Eq[F[Int]]): Eq[ParseAlg[F]] =
    Eq.by[ParseAlg[F], String => F[Int]](p => (s: String) => p.parseInt(s))

  implicit def arbitraryParseAlg[F[_]](implicit cS: Cogen[String], gF: Cogen[Float], FI: Arbitrary[F[Int]], FB: Arbitrary[F[String]]): Arbitrary[ParseAlg[F]] =
    Arbitrary {
      for {
        f1 <- Arbitrary.arbitrary[String => F[Int]]
        f2 <- Arbitrary.arbitrary[(Float, String) => F[String]]
      } yield new ParseAlg[F] {
        def parseInt(i: String): F[Int] = f1(i)
        def floatToString(f: Float, fmt: String): F[String] = f2(f, fmt)
      }
    }
}

object Interpreters {
  type F[T] = Try[T]

  object tryParse extends ParseAlg[F] {
    def parseInt(s: String) = Try(s.toInt)
    def floatToString(f: Float, fmt: String): F[String] = Success(fmt + f)
  }

}
