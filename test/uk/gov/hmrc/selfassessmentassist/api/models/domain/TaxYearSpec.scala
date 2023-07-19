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

package uk.gov.hmrc.selfassessmentassist.api.models.domain

import uk.gov.hmrc.selfassessmentassist.support.UnitSpec

class TaxYearSpec extends UnitSpec {

  "TaxYear" when {

    val taxYear = TaxYear.fromMtd("2023-24")

    "constructed from an MTD tax year" should {
      "return the FraudRisk tax year" in {
        taxYear.asFraudRisk shouldBe "2024"
      }

      "return the MTD tax year" in {
        taxYear.asMtd shouldBe "2023-24"
      }

      "return the RDS tax year" in {
        taxYear.asRds shouldBe 2024
      }

    }

    "constructed directly" should {
      "not compile" in {
        """new TaxYear("2021-22")""" shouldNot compile
      }
    }

    "compared with equals" should {
      "have equality based on content" in {
        val taxYear = TaxYear.fromMtd("2021-22")
        taxYear shouldBe TaxYear.fromMtd("2021-22")
        taxYear should not be TaxYear.fromMtd("2020-21")
      }
    }
  }

}
