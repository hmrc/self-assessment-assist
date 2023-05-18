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

package uk.gov.hmrc.selfassessmentassist.v1.models.domain

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.selfassessmentassist.support.UnitSpec
import uk.gov.hmrc.selfassessmentassist.v1.TestData.CommonTestData._

class AssessmentReportSpec extends UnitSpec {
  val assessmentReportModel: AssessmentReport = simpleAssessmentReport

  val assessmentReportJson: JsValue = Json.parse(s"""
       |{
       |"reportId": "${assessmentReportModel.reportId.toString}",
       |"messages":[
       |  {
       |    "title": "$simpleRiskTitle",
       |    "body": "$simpleRiskBody",
       |    "action": "$simpleRiskAction",
       |    "links": [
       |      {
       |         "title": "$simpleLinkTitle",
       |         "url": "$simpleLinkUrl"
       |      }
       |      ],
       |     "path": "$simplePath"
       |     }
       |    ],
       |    "nino": "$simpleNino",
       |    "taxYear": "${DesTaxYear.fromMtd(simpleTaxYear).toString}",
       |    "calculationId": "$simpleCalculationId",
       |    "correlationId": "$simpleRDSCorrelationId"
       | }
       |""".stripMargin)

  "AssessmentReport" when {
    "written to JSON" must {
      "product the specific JSON body " in {
        Json.toJson(assessmentReportModel) shouldBe assessmentReportJson
      }
    }
  }

}
