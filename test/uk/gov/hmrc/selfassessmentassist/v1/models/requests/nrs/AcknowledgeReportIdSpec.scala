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

package uk.gov.hmrc.selfassessmentassist.v1.models.requests.nrs

import play.api.libs.json.Json
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.selfassessmentassist.v1.models.request.nrs.AcknowledgeReportId

class AcknowledgeReportIdSpec extends AnyWordSpec with Matchers {

  "AcknowledgeReportId JSON format" should {

    "read from JSON" in {
      val json = Json.parse("""{ "reportId": "abc-123" }""")

      json.as[AcknowledgeReportId] shouldBe
        AcknowledgeReportId("abc-123")
    }

    "write to JSON" in {
      val id = AcknowledgeReportId("abc-123")

      Json.toJson(id: AcknowledgeReportId) shouldBe
        Json.parse("""{ "reportId": "abc-123" }""")
    }
  }

  "AcknowledgeReportId.stringify" should {

    "return the JSON string representation" in {
      val id = AcknowledgeReportId("abc-123")

      id.stringify shouldBe
        """{"reportId":"abc-123"}"""
    }
  }

}
