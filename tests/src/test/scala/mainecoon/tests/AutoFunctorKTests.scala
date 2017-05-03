package mainecoon
package tests


import scala.util.Try
import cats.~>
import cats.laws.discipline.SerializableTests
import mainecoon.laws.discipline.FunctorKTests


class AutoFunctorKTests extends MainecoonTestSuite {
  test("simple mapK") {

    val fk : Try ~> Option = λ[Try ~> Option](_.toOption)

    val optionParse: ParseAlg[Option] = Interpreters.tryParse.mapK(fk)

    optionParse.parseInt("3") should be(Some(3))
    optionParse.parseInt("sd") should be(None)
    optionParse.floatToString(3f, "d") should be(Some("d3.0"))

  }

  checkAll("ParseAlg[Option]", FunctorKTests[ParseAlg].functorK[Option, List, Vector, Int])
  checkAll("FunctorK[ParseAlg]", SerializableTests.serializable(FunctorK[ParseAlg]))
}

