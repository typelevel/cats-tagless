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
import cats.tagless.{Derive, autoInvariant, autoSemigroupal}

object ValidationExample extends App {

  @autoInvariant
  @autoSemigroupal
  trait Validator[A] {
    def validate(entity: A): ValidatedNec[String, A]
  }

  type ErrorOr[A] = ValidatedNec[String, A]
  implicit val allValid: SemigroupK[ErrorOr] = new SemigroupK[ErrorOr] {
    override def combineK[A](x: ErrorOr[A], y: ErrorOr[A]) = {
      implicit val last: Semigroup[A] = Semigroup.last
      x.combine(y)
    }
  }

  case class Money(amount: Int)
  case class Items(price: Money, quantity: Int)

  implicit class PrintHelper[A](x: => A) {
    def showValidationResult(message: => String): Unit = println(s"[$message] $x")
  }

  //////////// BASIC VALIDATORS ///////
  val positiveValidator: Validator[Int] = entity =>
    if (entity >= 0) Valid(entity)
    else Invalid(NonEmptyChain.one(s"[$entity]: Value cannot be negative"))

  def upperBoundValidator(upperBound: Int = 1000000000): Validator[Int] = entity =>
    if (entity <= upperBound) Valid(entity)
    else Invalid(NonEmptyChain.one(s"[$entity]: Value cannot be greater than $upperBound"))
  ////////////////////////////////////

  // magic is here: the instances are auto generated
  val invariant: Invariant[Validator] = Invariant[Validator]
  val semigroupal: Semigroupal[Validator] = Semigroupal[Validator]
  val semigroupK: SemigroupK[Validator] = Derive.semigroupK

  ////// DERIVED VALIDATORS //////////////
  lazy val moneyValidator: Validator[Money] = invariant.imap[Int, Money](positiveValidator)(Money)(_.amount)
  lazy val rangeValidator: Validator[Int] = semigroupK.combineK(positiveValidator, upperBoundValidator(100))
  lazy val tupleValidator: Validator[(Money, Int)] = semigroupal.product(moneyValidator, rangeValidator)
  lazy val itemsValidator: Validator[Items] = invariant.imap(tupleValidator)(Items.tupled)(i => i.price -> i.quantity)

  moneyValidator.validate(Money(-1)).showValidationResult("money validator")
  rangeValidator.validate(101).showValidationResult("range validator")
  rangeValidator.validate(-10).showValidationResult("range validator")
  itemsValidator.validate(Items(Money(-100), 102)).showValidationResult("items validator")
}
