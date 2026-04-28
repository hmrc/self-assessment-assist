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

package uk.gov.hmrc.selfassessmentassist.v1.models.request.cip

import play.api.libs.json.Json
import uk.gov.hmrc.selfassessmentassist.support.UnitSpec

class UserIdSpec extends UnitSpec {

  "UserId JSON format" should {

    "read from JSON" in {
      val json = Json.parse("""{ "value": "user-123" }""")

      json.as[UserId] shouldBe UserId("user-123")
    }

    "write to JSON" in {
      val userId = UserId("user-123")

      Json.toJson(userId) shouldBe Json.parse("""{ "value": "user-123" }""")
    }
  }

}
