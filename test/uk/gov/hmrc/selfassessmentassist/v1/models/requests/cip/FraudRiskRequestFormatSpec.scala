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

package uk.gov.hmrc.selfassessmentassist.v1.models.requests.cip

import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.selfassessmentassist.support.UnitSpec
import uk.gov.hmrc.selfassessmentassist.v1.models.request.cip.{BankAccountNumber, BankAccountSortCode, FraudRiskRequest, UTR}

class FraudRiskRequestFormatSpec extends UnitSpec {

  "FraudRiskRequest JSON format" should {

    "round-trip successfully with minimal required fields" in {
      val request = FraudRiskRequest(
        nino = Some("AA123456A"),
        taxYear = Some("2023-24"),
        fraudRiskHeaders = Map("CorrelationId" -> "correlation-id")
      )

      val json = Json.toJson(request)
      json.as[FraudRiskRequest] shouldBe request
    }

    "round-trip successfully when bank details are present (via JSON)" in {
      val json =
        Json.parse(
          """
            |{
            |  "nino": "AA123456A",
            |  "bankAccountSortCode": { "value": "123456" },
            |  "bankAccountNumber": { "value": "12345678" },
            |  "fraudRiskHeaders": {
            |    "CorrelationId": "correlation-id"
            |  }
            |}
            |""".stripMargin
        )

      val result = json.validate[FraudRiskRequest]

      result shouldBe a[JsSuccess[?]]

      val roundTripped = Json.toJson(result.get)
      roundTripped shouldBe json
    }

    "round-trip successfully when UTR is present (via JSON)" in {
      val json =
        Json.parse(
          """
            |{
            |  "utr": { "value": "1234567890" },
            |  "fraudRiskHeaders": {}
            |}
            |""".stripMargin
        )

      val result = json.validate[FraudRiskRequest]

      result shouldBe a[JsSuccess[?]]

      val roundTripped = Json.toJson(result.get)
      roundTripped shouldBe json
    }
  }

}
