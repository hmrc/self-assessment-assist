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

import uk.gov.hmrc.transactionalrisking.controllers.requestParsers.validators.validations.NinoValidation
import uk.gov.hmrc.transactionalrisking.models.errors.{FormatReportIdError, NinoFormatError}
import uk.gov.hmrc.transactionalrisking.support.UnitSpec
import uk.gov.hmrc.transactionalrisking.v1.CommonTestData.commonTestData.{simpleNino, simpleNinoInvalid}

class NinoValidationSpec extends UnitSpec {
  val validator = NinoValidation

  "running a validation" should {
    "return no errors" when {
      "a valid request" in {
        validator.validate(simpleNino) shouldBe Nil
      }


      "an invalid nino." in {

        val vl = validator.validate(simpleNinoInvalid)
        val vr = Seq(NinoFormatError)

        vl shouldBe vr
      }

    }
  }
}
