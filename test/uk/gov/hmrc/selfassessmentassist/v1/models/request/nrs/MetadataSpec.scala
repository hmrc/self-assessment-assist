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

package uk.gov.hmrc.selfassessmentassist.v1.models.request.nrs

import play.api.libs.json.*
import uk.gov.hmrc.selfassessmentassist.api.TestData.CommonTestData.{metaDataCorrectJson, metaDataCorrectModel}
import uk.gov.hmrc.selfassessmentassist.support.UnitSpec

class MetadataSpec extends UnitSpec {

  "Metadata" when {
    "read from valid JSON" should {
      "produce the expected model" in {
        metaDataCorrectJson.as[Metadata] shouldBe metaDataCorrectModel
      }
    }

    "read from invalid JSON" should {
      "produce a JsError" in {
        JsObject.empty.validate[Metadata] shouldBe a[JsError]
      }
    }

    "written to JSON" should {
      "produce the expected JsObject" in {
        Json.toJson(metaDataCorrectModel) shouldBe metaDataCorrectJson
      }
    }
  }

}
