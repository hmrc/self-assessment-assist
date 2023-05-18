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

package uk.gov.hmrc.selfassessmentassist.v1.controllers.requestParsers.validators.validations

import uk.gov.hmrc.selfassessmentassist.v1.models.errors.{MtdError, TaxYearFormatError, TaxYearRangeInvalid}

object TaxYearValidation {

  def validate(inputTaxYear: String): List[MtdError] = {
    val correctRegex = inputTaxYear.matches("^[0-9]{4}-[0-9]{2}$")
    if (correctRegex) {
      val yearCheck1 = inputTaxYear.slice(2, 4).toInt
      val yearCheck2 = inputTaxYear.drop(5).toInt
      if (yearCheck2.equals(yearCheck1 + 1)) {
        NoValidationErrors
      } else {
        List(TaxYearRangeInvalid)
      }
    } else {
      List(TaxYearFormatError)
    }
  }

}
