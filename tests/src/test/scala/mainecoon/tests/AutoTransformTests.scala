package mainecoon
package tests

import org.scalatest.{FunSuite, Matchers}

import scala.util.{Try, Success}
import cats.~>
import TransformK.ops._

@autoTransform
trait ParseAlg[F[_]] {
  def parseInt(i: String): F[Int]
  def floatToString(f: Float, fmt: String): F[String]
}

object Interpreters {
  type F[T] = Try[T]

  object tryParse extends ParseAlg[F] {
    def parseInt(s: String) = Try(s.toInt)
    def floatToString(f: Float, fmt: String): F[String] = Success(fmt + f)
  }
}

class AutoTransformTests extends FunSuite with Matchers {
  test("can transform") {

    val fk : Try ~> Option = λ[Try ~> Option](_.toOption)

    val optionParse: ParseAlg[Option] = Interpreters.tryParse.mapK(fk)

    optionParse.parseInt("3") should be(Some(3))
    optionParse.parseInt("sd") should be(None)
    optionParse.floatToString(3f, "d") should be(Some("d3.0"))
  }
}

