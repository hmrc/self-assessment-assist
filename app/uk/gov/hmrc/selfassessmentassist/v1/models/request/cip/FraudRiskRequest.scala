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

package uk.gov.hmrc.selfassessmentassist.v1.models.request.cip

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.selfassessmentassist.v1.models.request.cip.FraudRiskRequest.FraudRiskHeaders

case class FraudRiskRequest(nino: Option[String] = None, taxYear: Option[String] = None, fraudRiskHeaders: FraudRiskHeaders)

object FraudRiskRequest {
  type FraudRiskHeaders = Map[String, String]
  implicit val format: OFormat[FraudRiskRequest] = Json.format[FraudRiskRequest]
}
