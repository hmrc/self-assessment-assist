/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.transactionalrisking.models.domain

//import play.api.libs.json.{JsArray, JsNull, JsNumber, JsObject, JsString, Json, OFormat, Reads, Writes}
//import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.transactionalrisking.models.domain.FraudRiskRequest.FraudRiskHeaders
//import uk.gov.hmrc.transactionalrisking.services.nrs.models.request.{Metadata, NrsSubmission}
//import uk.gov.hmrc.transactionalriskingsimulator.services.ris.RdsAssessmentRequestForSelfAssessment.{Input, InputWithInt, InputWithObject, InputWithString}

// This is still being determined; please see TRDT-85.//TODO revisit me later
class FraudRiskRequest(nino: String, taxYear: String, fraudRiskHeaders: FraudRiskHeaders)

object FraudRiskRequest {
  type FraudRiskHeaders = Map[String, String]
//  implicit val mdFormat: OFormat[FraudRiskHeaders] = Json.format[FraudRiskHeaders]
//  implicit val format: OFormat[FraudRiskRequest] = Json.format[FraudRiskRequest]
}
