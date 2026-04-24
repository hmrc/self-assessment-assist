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

import play.api.libs.json.Json
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AuthCredentialSpec extends AnyWordSpec with Matchers {

  "AuthCredential JSON format" should {

    "read from JSON" in {
      val json = Json.parse(
        """{
          |  "client_id": "client-id",
          |  "client_secret": "client-secret",
          |  "grant_type": "client_credentials"
          |}""".stripMargin
      )

      json.as[AuthCredential] shouldBe
        AuthCredential(
          client_id = "client-id",
          client_secret = "client-secret",
          grant_type = "client_credentials"
        )
    }

    "write to JSON" in {
      val credential = AuthCredential(
        client_id = "client-id",
        client_secret = "client-secret",
        grant_type = "client_credentials"
      )

      Json.toJson(credential: AuthCredential) shouldBe
        Json.parse(
          """{
            |  "client_id": "client-id",
            |  "client_secret": "client-secret",
            |  "grant_type": "client_credentials"
            |}""".stripMargin
        )
    }
  }

}
