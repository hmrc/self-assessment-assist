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

class IFRequestPayloadActionSpec extends UnitSpec {

  private val model: IFRequestPayloadAction = IFRequestPayloadAction(
    title = "Action title",
    message = "Action message",
    action = "VIEW",
    path = "/path",
    links = Some(
      Seq(
        IFRequestPayloadActionLinks("Link 1", "/link-1"),
        IFRequestPayloadActionLinks("Link 2", "/link-2")
      )
    )
  )

  private val json: JsObject = Json.obj(
    "title"   -> "Action title",
    "message" -> "Action message",
    "action"  -> "VIEW",
    "path"    -> "/path",
    "links" -> Json.arr(
      Json.obj("linkTitle" -> "Link 1", "linkUrl" -> "/link-1"),
      Json.obj("linkTitle" -> "Link 2", "linkUrl" -> "/link-2")
    )
  )

  "IFRequestPayloadAction" when {
    "read from valid JSON" should {
      "produce the expected model" in {
        json.as[IFRequestPayloadAction] shouldBe model
      }
    }

    "read from invalid JSON" should {
      "produce a JsError" in {
        JsObject.empty.validate[IFRequestPayloadAction] shouldBe a[JsError]
      }
    }

    "written to JSON" should {
      "produce the expected JsObject" in {
        Json.toJson(model) shouldBe json
      }
    }
  }

}
