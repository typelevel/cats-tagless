package mainecoon.tests

import cats.arrow.FunctionK
import cats.instances.AllInstances
import mainecoon.syntax.AllSyntax
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.{FunSuite, Matchers}
import org.typelevel.discipline.scalatest.Discipline

class MainecoonTestSuite extends FunSuite with Matchers with Discipline with TestInstances with AllInstances with AllSyntax with cats.syntax.AllSyntax


trait TestInstances {
  implicit val catsDataArbitraryOptionList: Arbitrary[FunctionK[Option, List]] = Arbitrary(Gen.const(λ[FunctionK[Option, List]](_.toList)))
  implicit val catsDataArbitraryListVector: Arbitrary[FunctionK[List, Vector]] = Arbitrary(Gen.const(λ[FunctionK[List, Vector]](_.toVector)))
}
