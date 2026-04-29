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

    "fail to read when fraudRiskHeaders is missing" in {
      val json = Json.obj(
        "nino"    -> "AA123456A",
        "taxYear" -> "2023-24"
      )

      json.validate[FraudRiskRequest].isError shouldBe true
    }
  }

}
