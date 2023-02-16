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

package uk.gov.hmrc.selfassessmentassist.v1.controllers.requestParse.validators

import uk.gov.hmrc.selfassessmentassist.support.UnitSpec
import uk.gov.hmrc.selfassessmentassist.v1.TestData.CommonTestData._
import uk.gov.hmrc.selfassessmentassist.v1.controllers.requestParsers.validators.GenerateReportValidator
import uk.gov.hmrc.selfassessmentassist.v1.models.errors.{CalculationIdFormatError, NinoFormatError, TaxYearFormatError, TaxYearRangeInvalid}
import uk.gov.hmrc.selfassessmentassist.v1.models.request.GenerateReportRawData

class GenerateReportValidationSpec extends UnitSpec {
  val validator: GenerateReportValidator = new GenerateReportValidator

  "running a validation" should {
    "return no errors" when {
      "a valid request" in {
       
        validator.validate(simpleGenerateReportRawData) shouldBe Nil
      }

      "return errors" when {
        "an invalid nino." in {
          val generateReportRawData: GenerateReportRawData = simpleGenerateReportRawData.copy(nino = simpleNinoInvalid)
          validator.validate(generateReportRawData) shouldBe Seq(NinoFormatError)
        }

        "an invalid calculationId." in {
          val generateReportRawData: GenerateReportRawData = simpleGenerateReportRawData.copy(calculationId = simpleCalculationIdStrangeCharsString)

          validator.validate(generateReportRawData) shouldBe Seq(CalculationIdFormatError)
        }

        "an invalid nino and calculationId." in {
          val generateReportRawData: GenerateReportRawData = simpleGenerateReportRawData.copy(calculationId = invalidUUID.toString, nino = simpleNinoInvalid)

          validator.validate(generateReportRawData) shouldBe Seq(NinoFormatError, CalculationIdFormatError)
        }

        "an invalid nino and tax year." in {
          val generateReportRawData: GenerateReportRawData = simpleGenerateReportRawData.copy( nino = simpleNinoInvalid,taxYear = simpleTaxYearInvalid2)

          validator.validate(generateReportRawData) shouldBe Seq(NinoFormatError, TaxYearFormatError)
        }

        "an invalid nino and no sequential tax year." in {
          val generateReportRawData: GenerateReportRawData = simpleGenerateReportRawData.copy(nino = simpleNinoInvalid, taxYear = simpleTaxYearInvalid1)

          validator.validate(generateReportRawData) shouldBe Seq(NinoFormatError, TaxYearRangeInvalid)
        }
      }
    }
  }
}
