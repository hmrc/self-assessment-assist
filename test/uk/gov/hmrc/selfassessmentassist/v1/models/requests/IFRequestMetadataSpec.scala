/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.selfassessmentassist.v1.models.requests

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.selfassessmentassist.support.UnitSpec
import uk.gov.hmrc.selfassessmentassist.v1.models.request.ifs.IFRequestMetadata

import java.time.{OffsetDateTime, ZoneOffset}

class IFRequestMetadataSpec extends UnitSpec {

  val offsetDateTimeJson: JsValue     = Json.toJson("2021-12-10T11:09:13Z")
  val offsetDateTime: OffsetDateTime  = OffsetDateTime.of(2021, 12, 10, 11, 9, 13, 0, ZoneOffset.UTC)
  val requestModel: IFRequestMetadata = IFRequestMetadata("AA123456A", "2021-22", "calc_id", "Agent", Some("A123"), offsetDateTime)

  val requestJson: JsValue = Json.parse(s"""
                                  |{
                                  | "nino": "AA123456A",
                                  | "taxYear": "2021-22",
                                  | "calculationId": "calc_id",
                                  | "customerType": "Agent",
                                  | "agentReferenceNumber": "A123",
                                  | "calculationTimestamp": $offsetDateTimeJson
                                  |}
                                  |""".stripMargin)

  "reads" when {
    "passed valid JSON" should {
      "return a valid model" in {
        requestModel shouldBe requestJson.as[IFRequestMetadata]
      }
    }
  }

  "writes" when {
    "passed valid model" should {
      "return valid JSON" in {
        Json.toJson(requestModel) shouldBe requestJson
      }
    }
  }

}
