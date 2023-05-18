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

package uk.gov.hmrc.selfassessmentassist.v1.services.cip

import play.api.libs.json.Json
import uk.gov.hmrc.selfassessmentassist.v1.TestData.CommonTestData._
import uk.gov.hmrc.selfassessmentassist.v1.services.cip.models.{FraudRiskReport, FraudRiskRequest}

object InsightTestData {

  val fraudRiskReport: FraudRiskReport = FraudRiskReport(
    score = 10,
    riskCorrelationId = correlationId,
    reasons = Seq("flag")
  )

  val badRequestError = Json.parse(s"""{"/nino":["Invalid nino"]}""".stripMargin)

  def fraudRiskRequest(nino: String): FraudRiskRequest =
    new FraudRiskRequest(nino = Some(nino), taxYear = Some(simpleTaxYear), fraudRiskHeaders = Map.empty[String, String])

}
