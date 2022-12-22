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

package uk.gov.hmrc.transactionalrisking.v1.controllers.requestParse.validators.validations

import uk.gov.hmrc.transactionalrisking.support.UnitSpec
import uk.gov.hmrc.transactionalrisking.v1.TestData.CommonTestData._
import uk.gov.hmrc.transactionalrisking.v1.controllers.requestParsers.validators.validations.ReportIdValidation
import uk.gov.hmrc.transactionalrisking.v1.models.errors.FormatReportIdError

class ReportIdValidationSpec extends UnitSpec {

  val validator: ReportIdValidation.type = ReportIdValidation

  "running a validation" should {
    "return no errors" when {
      "a valid request" in {
        validator.validate(simpleReportId.toString) shouldBe Nil
      }

      "return errors" when {
        "an actual invalid request. No UUID format specifier" in {

          validator.validate(invalidUUIDString) shouldBe Seq(FormatReportIdError)
        }

        "an actual invalid request. Strange characters in string" in {

          validator.validate(simpleReportIdStrangeCharsString) shouldBe Seq(FormatReportIdError)
        }
      }

    }
  }
}
