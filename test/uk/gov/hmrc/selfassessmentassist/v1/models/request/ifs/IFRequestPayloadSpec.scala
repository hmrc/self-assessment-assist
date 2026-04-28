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

package uk.gov.hmrc.selfassessmentassist.v1.models.request.ifs

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class IFRequestPayloadSpec extends AnyWordSpec with Matchers {

  "IFRequestPayload JSON format" should {

    "round-trip successfully" in {
      val englishAction = IFRequestPayloadAction(
        title = "English title",
        message = "English message",
        action = "VIEW",
        path = "/english",
        links = None
      )

      val welshAction = IFRequestPayloadAction(
        title = "Welsh title",
        message = "Welsh message",
        action = "VIEW",
        path = "/welsh",
        links = None
      )

      val payload = IFRequestPayload(
        messageId = "message-id-123",
        englishAction = englishAction,
        welshAction = welshAction
      )

      val json = Json.toJson(payload: IFRequestPayload)
      json.as[IFRequestPayload] shouldBe payload
    }
  }

}
