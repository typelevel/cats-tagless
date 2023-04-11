/*
 * Copyright 2019 cats-tagless maintainers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cats.tagless.example

import cats.*
import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyChain, ValidatedNec}
import cats.syntax.all.*
import cats.tagless.autoInvariant
object ValidationExample extends App {

  @autoInvariant
  trait Validator[A] {
    def validate(entity: A): ValidatedNec[String, A]
  }

  type ErrorOr[A] = ValidatedNec[String, A]

  case class Money(amount: Int)
  case class Items(price: Money, quantity: Int)

  implicit class PrintHelper[A](x: => A) {
    def showValidationResult(message: => String) = println(s"[$message] $x")
  }

  //////////// BASIC VALIDATORS ///////
  val positiveValidator: Validator[Int] = new Validator[Int] {
    override def validate(entity: Int): ValidatedNec[String, Int] =
      if (entity >= 0) Valid(entity)
      else Invalid(NonEmptyChain.one(s"[$entity]: Value cannot be negative"))
  }

  def upperBoundValidator(upperBound: Int = 1000000000): Validator[Int] = new Validator[Int] {
    override def validate(entity: Int): ValidatedNec[String, Int] =
      if (entity <= upperBound) Valid(entity)
      else Invalid(NonEmptyChain.one(s"[$entity]: Value cannot be greater than $upperBound"))
  }
  ////////////////////////////////////

  // magic is here: Invariant[Validator] instance is auto generated
  val invariant: Invariant[Validator] = Invariant[Validator]

  val semigroupal: Semigroupal[Validator] = new Semigroupal[Validator] {
    override def product[A, B](fa: Validator[A], fb: Validator[B]): Validator[(A, B)] = new Validator[(A, B)] {
      override def validate(entity: (A, B)): ErrorOr[(A, B)] = {
        val (a, b) = entity
        Semigroupal[ErrorOr].product(fa.validate(a), fb.validate(b))
      }
    }
  }

  implicit val semigroupKValidated: SemigroupK[ErrorOr] = new SemigroupK[ErrorOr] {
    override def combineK[A](x: ErrorOr[A], y: ErrorOr[A]): ErrorOr[A] = x match {
      case errorA @ Invalid(eA) =>
        y match {
          case Valid(_) => errorA
          case Invalid(eB) => Invalid(eA.combine(eB))
        }
      case valid @ Valid(_) =>
        y match {
          case Valid(_) => valid
          case errorB @ Invalid(_) => errorB
        }
    }
  }

  val semigroupK: SemigroupK[Validator] = new SemigroupK[Validator] {
    override def combineK[A](x: Validator[A], y: Validator[A]): Validator[A] = new Validator[A] {
      override def validate(entity: A): ValidatedNec[String, A] =
        SemigroupK[ErrorOr].combineK(x.validate(entity), y.validate(entity))
    }

  }

  ////// DERIVED VALIDATORS //////////////
  lazy val moneyValidator: Validator[Money] = invariant.imap[Int, Money](positiveValidator)(Money)(_.amount)
  lazy val rangeValidator: Validator[Int] = semigroupK.combineK(positiveValidator, upperBoundValidator(100))
  lazy val tupleValidator: Validator[(Money, Int)] = semigroupal.product(moneyValidator, rangeValidator)
  lazy val itemsValidator: Validator[Items] =
    invariant.imap(tupleValidator)(x => Items(x._1, x._2))(x => x.price -> x.quantity)

  moneyValidator.validate(Money(-1)).showValidationResult("money validator")
  rangeValidator.validate(101).showValidationResult("range validator")
  rangeValidator.validate(-10).showValidationResult("range validator")
  itemsValidator.validate(Items(Money(-100), 102)).showValidationResult("items validator")

}
