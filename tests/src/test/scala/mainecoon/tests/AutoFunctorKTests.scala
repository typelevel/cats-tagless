package mainecoon
package tests


import scala.util.Try
import cats.~>
import cats.laws.discipline.SerializableTests
import mainecoon.laws.discipline.FunctorKTests


class AutoFunctorKTests extends MainecoonTestSuite {
  test("simple mapK") {

    val fk : Try ~> Option = λ[Try ~> Option](_.toOption)

    val optionParse: SafeAlg[Option] = Interpreters.tryInterpreter.mapK(fk)

    optionParse.parseInt("3") should be(Some(3))
    optionParse.parseInt("sd") should be(None)
    optionParse.divide(3f, 3f) should be(Some(1f))

  }

  checkAll("ParseAlg[Option]", FunctorKTests[SafeAlg].functorK[Try, Option, List, Int])
  checkAll("FunctorK[ParseAlg]", SerializableTests.serializable(FunctorK[SafeAlg]))
}

