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

package uk.gov.hmrc.selfassessmentassist.v1.services.rds.models.response

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.selfassessmentassist.support.UnitSpec
import uk.gov.hmrc.selfassessmentassist.v1.services.rds.models.response.RdsAssessmentReport._

import java.util.UUID

class RdsAssessmentReportSpec extends UnitSpec {

  val calculationId: UUID = UUID.randomUUID()
  val correlationId: String = "some-correlationId"
  val feedbackId: UUID = UUID.randomUUID()
  val responseCode: Int = 211
  val responseMessage: String = "some-message"
  val calculationTimeStamp: String ="2023-04-04T11:11:11.000Z"


  val outputs: Seq[Output] = Seq(
    KeyValueWrapper("calculationId", Some(calculationId.toString)),
    KeyValueWrapper("correlationId", Some(correlationId)),
    KeyValueWrapper("feedbackId", Some(feedbackId.toString)),
    KeyValueWrapperInt("responseCode", responseCode),
    KeyValueWrapper("responseMessage", Some(responseMessage)),
    KeyValueWrapper("calculationTimestamp", Some(calculationTimeStamp))
  )

  val rdsAssessmentReport: RdsAssessmentReport = RdsAssessmentReport(
    links = Seq("https://google.com"),
    version = 2,
    moduleId = "HMRC_ASSIST_ITSA_FINSUB_FEEDBACK_ACK",
    stepId = "execute",
    executionState = "completed",
    outputs = outputs)

  val json: JsValue = Json.parse(
    s"""
    {
       |  "links": ["https://google.com"],
       |  "version": 2,
       |  "moduleId": "HMRC_ASSIST_ITSA_FINSUB_FEEDBACK_ACK",
       |  "stepId": "execute",
       |  "executionState": "completed",
       |  "outputs": [
       |   {
       |      "name": "calculationId",
       |      "value": "${calculationId}"
       |    },
       |    {
       |      "name": "correlationId",
       |      "value": "${correlationId}"
       |    },
       |    {
       |      "name": "feedbackId",
       |      "value": "${feedbackId}"
       |    },
       |    {
       |      "name": "responseCode",
       |      "value": $responseCode
       |    },
       |    {
       |      "name": "responseMessage",
       |      "value": "$responseMessage"
       |    },
       |    {
       |      "name": "calculationTimestamp",
       |      "value": "$calculationTimeStamp"
       |    }
       |   ]
       |   }
       |""".stripMargin)


  "RdsAssessmentReport" when {


    "reads JSON" must {
      "return the valid model" in {
        json.as[RdsAssessmentReport] shouldBe rdsAssessmentReport
      }
    }

    "When a function is called" must {
      "return calculationId" in {
        rdsAssessmentReport.calculationId shouldBe Some(calculationId)
      }
      "return rdsCorrelationId" in {
        rdsAssessmentReport.rdsCorrelationId shouldBe Some(correlationId)
      }
      "return feedbackId" in {
        rdsAssessmentReport.feedbackId shouldBe Some(feedbackId)
      }
      "return responseCode" in {
        rdsAssessmentReport.responseCode shouldBe Some(responseCode)
      }
      "return responseMessage" in {
        rdsAssessmentReport.responseMessage shouldBe Some(responseMessage)
      }
      "return calculationTimeStamp" in {
        rdsAssessmentReport.calculationTimestamp shouldBe Some(calculationTimeStamp)
      }
    }
  }

}