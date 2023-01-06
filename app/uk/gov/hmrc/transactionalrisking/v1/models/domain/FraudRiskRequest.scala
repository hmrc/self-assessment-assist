/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.transactionalrisking.v1.models.domain

import uk.gov.hmrc.auth.core.Nino
import uk.gov.hmrc.transactionalrisking.v1.models.domain.FraudRiskRequest.FraudRiskHeaders

abstract class AtLeastOneNonEmptyOption[T] { self: T with Product =>

  def nonEmpty: Boolean = this.productIterator.exists {
    case Some(_) => true
    case _ => false
  }

}

// This is still being determined; please see TRDT-85.//TODO revisit me later
case class FraudRiskRequest(
                             nino: Option[String],
                             taxYear: Option[String],
                             utr:Option[UTR],
                             deviceId:Option[String],
                             userId:Option[UserId],
                             ipAddress:Option[String],
                             bankAccountSortCode:Option[BankAccountSortCode],
                             bankAccountNumber:Option[BankAccountNumber],
                             email:Option[String],
                             submissionId:Option[String],
                             fraudRiskHeaders: FraudRiskHeaders){



}

object FraudRiskRequest {
  type FraudRiskHeaders = Map[String, String]
}

final case class UTR private(value:String){
  private def copy: Unit = ()
}

object UTR{
  private def apply(value: String): Either[String, UTR] =
    Either.cond(
      value.length == 10 && value.startsWith("0"),
      new UTR(value),
      "Invalid value should be 10 characters long and start with 0"
    )

  def fromString(value: String): Either[String, UTR] = apply(value)
}

final case class UserId private(value:String){
  private def copy: Unit = ()
}


object UserId{
  private def apply(value: String): Either[String, UserId] =
    Either.cond(
      value.length == 16 && value.startsWith("0"),
      new UserId(value),
      "Invalid value should be 16 characters long and start with 0"
    )

  def fromString(value: String): Either[String, UserId] = apply(value)
}


final case class BankAccountSortCode private(value:String){
  private def copy: Unit = ()
}


object BankAccountSortCode{
  private def apply(value: String): Either[String, BankAccountSortCode] =
    Either.cond(
      value.length == 6 && value.startsWith("0"),
      new BankAccountSortCode(value),
      "Invalid value should be 6 characters long and start with 0"
    )

  def fromString(value: String): Either[String, BankAccountSortCode] = apply(value)
}


final case class BankAccountNumber private(value:String){
  private def copy: Unit = ()
}


object BankAccountNumber{
  private def apply(value: String): Either[String, BankAccountNumber] =
    Either.cond(
      value.length == 8 && value.startsWith("0"),
      new BankAccountNumber(value),
      "Invalid value should be 6 characters long and start with 0"
    )

  def fromString(value: String): Either[String, BankAccountNumber] = apply(value)
}