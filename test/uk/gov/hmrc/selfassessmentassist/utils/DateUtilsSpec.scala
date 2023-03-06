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

package uk.gov.hmrc.selfassessmentassist.utils

import play.api.libs.json.{JsError, JsNumber, JsString, JsSuccess}
import uk.gov.hmrc.selfassessmentassist.support.UnitSpec

import java.time.OffsetDateTime

class DateUtilsSpec extends UnitSpec {


val date = OffsetDateTime.parse("2022-01-01T12:00Z")

  "DateUtils" when {
    "dateTimeWrites" must {
      "writes OK" in {
        val result = DateUtils.dateTimeWrites.writes(date)
        result.as[String] shouldBe "2022-01-01T12:00:00.000Z"
      }

      "dateTimeReads" must {
        "read OK" in {
          DateUtils.dateTimeReads.reads(JsString("2022-01-01T12:00:00.000Z")) shouldBe JsSuccess(date)
        }

        "read fail" in {
          DateUtils.dateTimeReads.reads(JsNumber(3)) shouldBe JsError()
        }
      }

      "isoInstantDateWrites" must {
        "writes OK" in {
          DateUtils.isoInstantDateWrites.writes(date).as[String] shouldBe "2022-01-01T12:00:00Z"
        }
      }

      "isoInstantDateReads" must {

        "read fail" in {
          DateUtils.isoInstantDateReads.reads(JsString("2022-01-01T12:00:00.000Z")) shouldBe JsError()
        }

      }

      "defaultDateTimeReads" must {

        "read fail" in {
          DateUtils.defaultDateTimeReads.reads(JsNumber(2)) shouldBe JsError()
        }
      }

      "dateReads" must {

        "read fail" in {
          DateUtils.dateReads.reads(JsNumber(2)) shouldBe JsError()
        }
      }

    }

  }

}