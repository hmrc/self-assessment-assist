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

import play.api.libs.json.*
import uk.gov.hmrc.selfassessmentassist.support.UnitSpec

class IFRequestPayloadSpec extends UnitSpec {

  private val englishAction: IFRequestPayloadAction = IFRequestPayloadAction(
    title = "English title",
    message = "English message",
    action = "VIEW",
    path = "/english",
    links = Some(Seq(IFRequestPayloadActionLinks("View english details", "/englishDetails")))
  )

  private val welshAction: IFRequestPayloadAction = IFRequestPayloadAction(
    title = "Welsh title",
    message = "Welsh message",
    action = "VIEW",
    path = "/welsh",
    links = Some(Seq(IFRequestPayloadActionLinks("View welsh details", "/welshDetails")))
  )

  private val model = IFRequestPayload(
    messageId = "message-id-123",
    englishAction = englishAction,
    welshAction = welshAction
  )

  private val json: JsObject = Json.obj(
    "messageId" -> "message-id-123",
    "englishAction" -> Json.obj(
      "title"   -> "English title",
      "message" -> "English message",
      "action"  -> "VIEW",
      "path"    -> "/english",
      "links" -> Json.arr(
        Json.obj(
          "linkTitle" -> "View english details",
          "linkUrl"   -> "/englishDetails"
        )
      )
    ),
    "welshAction" -> Json.obj(
      "title"   -> "Welsh title",
      "message" -> "Welsh message",
      "action"  -> "VIEW",
      "path"    -> "/welsh",
      "links" -> Json.arr(
        Json.obj(
          "linkTitle" -> "View welsh details",
          "linkUrl"   -> "/welshDetails"
        )
      )
    )
  )

  "IFRequestPayload" when {
    "read from valid JSON" should {
      "produce the expected model" in {
        json.as[IFRequestPayload] shouldBe model
      }
    }

    "read from invalid JSON" should {
      "produce a JsError" in {
        JsObject.empty.validate[IFRequestPayload] shouldBe a[JsError]
      }
    }

    "written to JSON" should {
      "produce the expected JsObject" in {
        Json.toJson(model) shouldBe json
      }
    }
  }

}
