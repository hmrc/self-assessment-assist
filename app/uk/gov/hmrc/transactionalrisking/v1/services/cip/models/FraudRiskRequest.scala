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

package uk.gov.hmrc.transactionalrisking.v1.services.cip.models

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.transactionalrisking.v1.services.cip.models.FraudRiskRequest.FraudRiskHeaders

// This is still being determined; please see TRDT-85.//TODO revisit me later
case class FraudRiskRequest(
                             nino: Option[String]=None,
                             taxYear: Option[String]=None,
                             utr:Option[UTR]=None,
                             deviceId:Option[String]=None,
                             userId:Option[UserId]=None,
                             ipAddress:Option[String]=None,
                             bankAccountSortCode:Option[BankAccountSortCode]=None,
                             bankAccountNumber:Option[BankAccountNumber]=None,
                             email:Option[String]=None,
                             submissionId:Option[String]=None,
                             fraudRiskHeaders: FraudRiskHeaders)

object FraudRiskRequest {
  type FraudRiskHeaders = Map[String, String]
  implicit val utrFormat: OFormat[UTR] = UTR.format
  implicit val bascFormat: OFormat[BankAccountSortCode] = BankAccountSortCode.format
  implicit val banFormat: OFormat[BankAccountNumber] = BankAccountNumber.format
  implicit val format: OFormat[FraudRiskRequest] = Json.format[FraudRiskRequest]
}

case class UTR private(value:String)

object UTR{
  implicit val format: OFormat[UTR] = Json.format[UTR]
/*  private def apply(value: String): Either[InitializationError, UTR] =
    Either.cond(
      value.length == 10 && value.startsWith("0"),
      new UTR(value),
      InvalidValueOrFormat("Invalid value should be 10 characters long and start with 0")
    )

  def fromString(value: String): Either[InitializationError, UTR] = apply(value)*/
}

case class UserId(value:String)
/*{
  private def copy: Unit = ()
}*/


object UserId{
  implicit val format: OFormat[UserId] = Json.format[UserId]
/*  private def apply(value: String): Either[InitializationError, UserId] =
    Either.cond(
      value.length == 16 && value.startsWith("0"),
      new UserId(value),
      InvalidValueOrFormat("Invalid value should be 16 characters long and start with 0")
    )

  def fromString(value: String): Either[String, UserId] = apply(value)*/
}


case class BankAccountSortCode private(value:String)



object BankAccountSortCode{
  implicit val format: OFormat[BankAccountSortCode] = Json.format[BankAccountSortCode]
/*  private def apply(value: String): Either[InitializationError, BankAccountSortCode] =
    Either.cond(
      value.length == 6 && value.startsWith("0"),
      new BankAccountSortCode(value),
      InvalidValueOrFormat("Invalid value should be 6 characters long and start with 0")
    )

  def fromString(value: String): Either[InitializationError, BankAccountSortCode] = apply(value)*/
}


case class BankAccountNumber private(value:String)


object BankAccountNumber{
  implicit val format: OFormat[BankAccountNumber] = Json.format[BankAccountNumber]
/*  private def apply(value: String): Either[InitializationError, BankAccountNumber] =
    Either.cond(
      value.length == 8 && value.startsWith("0"),
      new BankAccountNumber(value),
      InvalidValueOrFormat("Invalid value should be 6 characters long and start with 0")
    )

  def fromString(value: String): Either[InitializationError, BankAccountNumber] = apply(value)*/
}