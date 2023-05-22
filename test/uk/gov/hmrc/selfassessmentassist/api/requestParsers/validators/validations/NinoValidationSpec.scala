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

package uk.gov.hmrc.selfassessmentassist.api.requestParsers.validators.validations

import uk.gov.hmrc.selfassessmentassist.api.TestData.CommonTestData.{simpleNino, simpleNinoInvalid}
import uk.gov.hmrc.selfassessmentassist.api.models.errors.NinoFormatError
import uk.gov.hmrc.selfassessmentassist.support.UnitSpec

class NinoValidationSpec extends UnitSpec {

  private val validator: NinoValidation.type = NinoValidation

  "running a validation" should {
    "a valid request" in {
      validator.validate(simpleNino) shouldBe Nil
    }

    "return errors" when {
      "an invalid nino." in {

        validator.validate(simpleNinoInvalid) shouldBe Seq(NinoFormatError)
      }

    }
  }

}
