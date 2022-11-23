/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.transactionalrisking.v1.controllers.requestParse.validators

import uk.gov.hmrc.transactionalrisking.controllers.requestParsers.validators.GenerateReportValidator
import uk.gov.hmrc.transactionalrisking.models.errors.{CalculationIdFormatError, NinoFormatError}
import uk.gov.hmrc.transactionalrisking.models.request.GenerateReportRawData
import uk.gov.hmrc.transactionalrisking.support.UnitSpec
import uk.gov.hmrc.transactionalrisking.v1.TestData.CommonTestData.commonTestData.{simpleCalculationID, simpleCalculationIDStrangeCharsString, simpleNino, simpleNinoInvalid}

class GenerateReportValidationSpec extends UnitSpec {
  val validator:GenerateReportValidator = new GenerateReportValidator

  "running a validation" should {
    "return no errors" when {
      "a valid request" in {
        val generateReportRawData: GenerateReportRawData = GenerateReportRawData(simpleNino, simpleCalculationID.toString)
        validator.validate(generateReportRawData) shouldBe Nil
      }

      "return errors" when {
        "an invalid nino." in {
          val generateReportRawData: GenerateReportRawData = GenerateReportRawData(simpleNinoInvalid, simpleCalculationID.toString)

          validator.validate(generateReportRawData) shouldBe Seq(NinoFormatError)
        }

        "an invalid calculationID." in {
          val generateReportRawData: GenerateReportRawData = GenerateReportRawData(simpleNino, simpleCalculationIDStrangeCharsString)

          validator.validate(generateReportRawData) shouldBe Seq(CalculationIdFormatError)
        }

        "an invalid nino and calculationID." in {
          val generateReportRawData: GenerateReportRawData = GenerateReportRawData(simpleNinoInvalid, simpleCalculationIDStrangeCharsString)

          validator.validate(generateReportRawData) shouldBe Seq(NinoFormatError, CalculationIdFormatError)
        }
      }
    }
  }
}