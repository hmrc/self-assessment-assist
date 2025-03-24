/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.selfassessmentassist.v1.models.requests.nrs

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.selfassessmentassist.support.UnitSpec
import uk.gov.hmrc.selfassessmentassist.v1.models.request.nrs.NrsSubmissionWorkItem
import uk.gov.hmrc.selfassessmentassist.v1.services.testData.NrsTestData.{correctJson, correctModel}

class NrsSubmissionWorkItemSpec extends UnitSpec {

  private val model: NrsSubmissionWorkItem = NrsSubmissionWorkItem(nrsSubmission = correctModel)

  private val json: JsValue = Json.obj("nrsSubmission" -> correctJson)

  "NrsSubmissionWorkItem" when {
    "reads" should {
      "return a valid model" when {
        "passed valid JSON" in {
          json.as[NrsSubmissionWorkItem] shouldBe model
        }
      }
    }

    "writes" should {
      "return valid JSON" when {
        "passed valid model" in {
          Json.toJson(model) shouldBe json
        }
      }
    }
  }

}
