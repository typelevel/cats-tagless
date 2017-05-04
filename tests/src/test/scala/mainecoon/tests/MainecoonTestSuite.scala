package mainecoon.tests

import cats.arrow.FunctionK
import cats.instances.AllInstances
import cats.kernel.Eq
import mainecoon.syntax.AllSyntax
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.{FunSuite, Matchers}
import org.typelevel.discipline.scalatest.Discipline

import scala.util.Try

class MainecoonTestSuite extends FunSuite with Matchers with Discipline with TestInstances with AllInstances with AllSyntax with cats.syntax.AllSyntax


trait TestInstances {
  implicit val catsDataArbitraryOptionList: Arbitrary[FunctionK[Option, List]] = Arbitrary(Gen.const(λ[FunctionK[Option, List]](_.toList)))
  implicit val catsDataArbitraryTryOption: Arbitrary[FunctionK[Try, Option]] = Arbitrary(Gen.const(λ[FunctionK[Try, Option]](_.toOption)))
  implicit val catsDataArbitraryListVector: Arbitrary[FunctionK[List, Vector]] = Arbitrary(Gen.const(λ[FunctionK[List, Vector]](_.toVector)))

  implicit val eqThrow: Eq[Throwable] = Eq.allEqual
}
