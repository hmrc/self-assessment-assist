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

import play.api.libs.json._
import uk.gov.hmrc.selfassessmentassist.support.UnitSpec

import java.time.{LocalDate, OffsetDateTime}

class DateUtilsSpec extends UnitSpec {

  val date: OffsetDateTime = OffsetDateTime.parse("2022-01-01T12:00Z")

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
          DateUtils.isoInstantDateTimeWrites.writes(date).as[String] shouldBe "2022-01-01T12:00:00Z"
        }
      }

      "isoInstantDateReads" must {

        "read fail" in {
          DateUtils.isoInstantDateTimeReads.reads(JsString("2022-01-01T12:00:00.000Z")) shouldBe JsError()
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

      "localDateTime" must {
        "read to offset date time" in {
          val localDateTimeString = "2007-12-03T10:15:30"

          DateUtils.offsetDateTimeFromLocalDateTimeFormatReads.reads(JsString(localDateTimeString)).map(_.toString shouldBe "2007-12-03T10:15:30Z")
        }

        "return error" in {
          val notADateTimeString = "foo"

          DateUtils.offsetDateTimeFromLocalDateTimeFormatReads.reads(JsString(notADateTimeString)).asEither match {
            case Left(List((_, List(JsonValidationError(List(message)))))) =>
              message shouldBe "not a valid date Text 'foo' could not be parsed at index 0"
            case _ => throw new IllegalStateException("unexpected result for offsetDateTimeFromLocalDateTimeFormatReads")
          }

        }
      }

      "date" should {
        "serialize LocalDate to JSON" in {
          val date         = LocalDate.of(2023, 5, 11)
          val expectedJson = JsString("2023-05-11")

          val json = DateUtils.dateWrites.writes(date)

          json shouldBe expectedJson
        }

        "dateReads" should {
          "deserialize JSON to LocalDate" in {
            val json         = JsString("2023-05-11")
            val expectedDate = LocalDate.of(2023, 5, 11)

            val result = Json.fromJson[LocalDate](json)

            result shouldBe JsSuccess(expectedDate)
          }

          "return JsError for invalid date format" in {
            val json = JsString("invalid-date")

            val result = Json.fromJson[LocalDate](json)

            result shouldBe a[JsError]
          }
        }
      }
    }

  }

}
