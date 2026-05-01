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

package uk.gov.hmrc.selfassessmentassist.api.models.auth

import play.api.libs.json.*
import uk.gov.hmrc.selfassessmentassist.support.UnitSpec

class AuthCredentialSpec extends UnitSpec {

  private val model: AuthCredential = AuthCredential("client-id", "client-secret", "client_credentials")

  private val json: JsObject = Json.obj(
    "client_id"     -> "client-id",
    "client_secret" -> "client-secret",
    "grant_type"    -> "client_credentials"
  )

  "AuthCredential" when {
    "read from valid JSON" should {
      "produce the expected model" in {
        json.as[AuthCredential] shouldBe model
      }
    }

    "read from invalid JSON" should {
      "produce a JsError" in {
        JsObject.empty.validate[AuthCredential] shouldBe a[JsError]
      }
    }

    "written to JSON" should {
      "produce the expected JsObject" in {
        Json.toJson(model) shouldBe json
      }
    }
  }

}
