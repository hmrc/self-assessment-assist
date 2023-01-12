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
}

case class UserId(value:String)
object UserId{
  implicit val format: OFormat[UserId] = Json.format[UserId]
}


case class BankAccountSortCode private(value:String)
object BankAccountSortCode{
  implicit val format: OFormat[BankAccountSortCode] = Json.format[BankAccountSortCode]
}


case class BankAccountNumber private(value:String)
object BankAccountNumber{
  implicit val format: OFormat[BankAccountNumber] = Json.format[BankAccountNumber]
}