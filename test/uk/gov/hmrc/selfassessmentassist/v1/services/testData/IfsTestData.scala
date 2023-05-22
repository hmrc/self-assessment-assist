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

package uk.gov.hmrc.selfassessmentassist.v1.services.testData

import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.selfassessmentassist.api.TestData.CommonTestData
import uk.gov.hmrc.selfassessmentassist.v1.models.request.ifs.{IFRequest, Messages}
import uk.gov.hmrc.selfassessmentassist.v1.services.testData.RdsTestData.assessmentRequestForSelfAssessment

import java.time.OffsetDateTime

object IfsTestData {

  val correctJson: JsObject = Json.obj(
    "payload"  -> "XXX-base64checksum-XXX",
    "metadata" -> CommonTestData.metaDataCorrectJson
  )

  val correctModel: IFRequest =
    IFRequest(
      serviceRegime = "self-assessment-assist",
      eventName = "GenerateReport",
      eventTimestamp = OffsetDateTime.now(),
      feedbackId = CommonTestData.simpleAcknowledgeNewRdsAssessmentReport.feedbackId.get.toString,
      metaData = List(
        Map("nino"                 -> "nino"),
        Map("taxYear"              -> "2023-2024"),
        Map("calculationId"        -> "calculationId"),
        Map("customerType"         -> assessmentRequestForSelfAssessment.customerType.toString),
        Map("agentReferenceNumber" -> assessmentRequestForSelfAssessment.agentRef.getOrElse("")),
        Map("calculationTimestamp" -> "timestamp")
      ),
      payload = Some(Messages(Some(Vector())))
    )

  val correctJsonString: String = IFRequest.formats.writes(correctModel).toString()
}

