/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.selfassessmentassist.api.TestData.CommonTestData
import uk.gov.hmrc.selfassessmentassist.v1.models.request.ifs.*
import uk.gov.hmrc.selfassessmentassist.v1.services.testData.RdsTestData.assessmentRequestForSelfAssessment

import java.time.OffsetDateTime

object IfsTestData {

  val correctModel: IFRequest =
    IFRequest(
      serviceRegime = "self-assessment-assist",
      eventName = "GenerateReport",
      eventTimestamp = OffsetDateTime.now(),
      feedbackId = CommonTestData.simpleAcknowledgeNewRdsAssessmentReport.feedbackId.get.toString,
      metaData = List(
        Map("nino"                 -> "nino"),
        Map("taxYear"              -> "2023-24"),
        Map("calculationId"        -> "calculationId"),
        Map("customerType"         -> assessmentRequestForSelfAssessment.customerType.toString),
        Map("agentReferenceNumber" -> assessmentRequestForSelfAssessment.agentRef.getOrElse("")),
        Map("calculationTimestamp" -> "timestamp")
      ),
      payload = Some(
        Messages(
          messages = Some(
            Seq(
              IFRequestPayload(
                messageId = "messageId",
                englishAction = IFRequestPayloadAction(
                  title = "English title",
                  message = "English message",
                  action = "VIEW",
                  path = "/english",
                  links = Some(Seq(IFRequestPayloadActionLinks("View english details", "/englishDetails")))
                ),
                welshAction = IFRequestPayloadAction(
                  title = "Welsh title",
                  message = "Welsh message",
                  action = "VIEW",
                  path = "/welsh",
                  links = Some(Seq(IFRequestPayloadActionLinks("View welsh details", "/welshDetails")))
                )
              )
            )
          )
        )
      )
    )

  val correctJson: JsValue = Json.toJson(correctModel)
}
