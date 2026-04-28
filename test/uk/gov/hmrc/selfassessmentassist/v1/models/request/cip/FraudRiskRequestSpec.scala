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

import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.selfassessmentassist.support.UnitSpec
import FraudRiskRequest._

class FraudRiskRequestSpec extends UnitSpec {

  "FraudRiskRequest JSON format" should {

    "round-trip successfully with all fields populated" in {

      val utr =
        Json.parse("""{ "value": "1234567890" }""").as[UTR]
      val sortCode =
        Json.parse("""{ "value": "112233" }""").as[BankAccountSortCode]
      val accountNumber =
        Json.parse("""{ "value": "12345678" }""").as[BankAccountNumber]
      val userId =
        Json.parse("""{ "value": "user-id" }""").as[UserId]

      val request = FraudRiskRequest(
        nino = Some("AA123456A"),
        taxYear = Some("2023-24"),
        utr = Some(utr),
        deviceId = Some("device-id"),
        userId = Some(userId),
        ipAddress = Some("127.0.0.1"),
        bankAccountSortCode = Some(sortCode),
        bankAccountNumber = Some(accountNumber),
        email = Some("test@example.com"),
        submissionId = Some("submission-id"),
        fraudRiskHeaders = Map("header-1" -> "value-1")
      )

      Json.toJson(request).as[FraudRiskRequest].shouldBe(request)
    }

    "round-trip successfully with only required fields" in {

      val minimal = FraudRiskRequest(
        fraudRiskHeaders = Map("header" -> "value")
      )

      Json.toJson(minimal).as[FraudRiskRequest].shouldBe(minimal)
    }

    "fail to read when fraudRiskHeaders is missing" in {

      val json: JsObject = Json.obj(
        "nino" -> "AA123456A"
      )

      json.validate[FraudRiskRequest].isError.shouldBe(true)
    }
  }

}
