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

package uk.gov.hmrc.selfassessmentassist.v1.controllers.requestParse

import uk.gov.hmrc.selfassessmentassist.support.UnitSpec
import uk.gov.hmrc.selfassessmentassist.v1.TestData.CommonTestData._
import uk.gov.hmrc.selfassessmentassist.v1.controllers.requestParsers.GenerateReportRequestParser
import uk.gov.hmrc.selfassessmentassist.v1.controllers.requestParsers.validators.GenerateReportValidator
import uk.gov.hmrc.selfassessmentassist.v1.models.domain.{AssessmentRequestForSelfAssessment, CustomerType, PreferredLanguage}
import uk.gov.hmrc.selfassessmentassist.v1.models.errors.{ErrorWrapper, MtdError}
import uk.gov.hmrc.selfassessmentassist.v1.models.request.{GenerateReportRawData}

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class GenerateReportRequestParserSpec extends UnitSpec {

  val validator: GenerateReportValidator = new GenerateReportValidator

  val validData = GenerateReportRawData(
    calculationId = "0f14d0ab-9605-4a62-a9e4-5ed26688389b",
    nino = "NJ070957A",
    preferredLanguage = PreferredLanguage.English,
    customerType = CustomerType.Agent,
    agentRef = None,
    taxYear = "2021-22"
  )

  "parse" should {
    "return a Request" when {
      "the validator returns no errors" in {
        val parser: GenerateReportRequestParser = new GenerateReportRequestParser(validator)
        val result = await(
          parser.parseRequest(validData)(implicitly[ExecutionContext], correlationId)
        )

        result shouldBe Right(
          AssessmentRequestForSelfAssessment(
            UUID.fromString("0f14d0ab-9605-4a62-a9e4-5ed26688389b"),
            "NJ070957A",
            PreferredLanguage.English,
            CustomerType.Agent,
            None,
            "2022"
          ))
      }
    }

    "fail a Request" when {
      " nino and taxyear is empty, the validator must returns errors" in {
        val parser: GenerateReportRequestParser = new GenerateReportRequestParser(validator)
        val invalidData                         = validData.copy(nino = "", taxYear = "")
        val result = await(
          parser.parseRequest(invalidData)(implicitly[ExecutionContext], correlationId)
        )

        result shouldBe Left(
          ErrorWrapper(
            "f2fb30e5-4ab6-4a29-b3c1-c00000011111",
            MtdError("INVALID_REQUEST", "Invalid request", None),
            Some(
              List(
                MtdError("FORMAT_NINO", "The provided NINO is invalid", None),
                MtdError("FORMAT_TAX_YEAR", "The provided tax year is invalid", None)))
          ))
      }

      "invalid taxyear range is supplied, the validator must returns errors" in {
        val parser: GenerateReportRequestParser = new GenerateReportRequestParser(validator)
        val invalidData                         = validData.copy(nino = "NJ070957A", taxYear = "2021-23")
        val result = await(
          parser.parseRequest(invalidData)(implicitly[ExecutionContext], correlationId)
        )

        result shouldBe Left(
          ErrorWrapper(
            "f2fb30e5-4ab6-4a29-b3c1-c00000011111",
            MtdError("RULE_TAX_YEAR_RANGE_INVALID", "Tax year range invalid. A tax year range of one year is required.", None),
            None
          ))
      }
    }

    "return a Request with tax year in the expected format" when {
      "the validator returns no errors" in {
        val parser: GenerateReportRequestParser = new GenerateReportRequestParser(validator)
        val result = await(
          parser.parseRequest(validData)(implicitly[ExecutionContext], correlationId)
        )

        result shouldBe Right(
          AssessmentRequestForSelfAssessment(
            UUID.fromString("0f14d0ab-9605-4a62-a9e4-5ed26688389b"),
            "NJ070957A",
            PreferredLanguage.English,
            CustomerType.Agent,
            None,
            "2022"
          ))

        result should not equal Right(
          AssessmentRequestForSelfAssessment(
            UUID.fromString("0f14d0ab-9605-4a62-a9e4-5ed26688389b"),
            "NJ070957A",
            PreferredLanguage.English,
            CustomerType.Agent,
            None,
            "2021-22"
          ))
      }
    }
  }

}
