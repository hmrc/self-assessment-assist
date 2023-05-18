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

package uk.gov.hmrc.selfassessmentassist.v1.controllers.requestParsers

import uk.gov.hmrc.selfassessmentassist.v1.controllers.requestParsers.validators.GenerateReportValidator
import uk.gov.hmrc.selfassessmentassist.v1.controllers.requestParsers.validators.validations.TaxYearValidation
import uk.gov.hmrc.selfassessmentassist.v1.models.domain.{AssessmentRequestForSelfAssessment, DesTaxYear}
import uk.gov.hmrc.selfassessmentassist.v1.models.errors.MtdError
import uk.gov.hmrc.selfassessmentassist.v1.models.request.GenerateReportRawData

import java.util.UUID
import javax.inject.{Inject, Singleton}

@Singleton
class GenerateReportRequestParser @Inject() (val validator: GenerateReportValidator)
    extends RequestParser[GenerateReportRawData, AssessmentRequestForSelfAssessment] {

  override protected def requestFor(data: GenerateReportRawData): Either[MtdError, AssessmentRequestForSelfAssessment] = {
    val taxYearValidation = TaxYearValidation.validate(data.taxYear)
    if (taxYearValidation.isEmpty) {
      val taxYearInRDSFormat = DesTaxYear.fromMtd(data.taxYear).toString
      Right(
        AssessmentRequestForSelfAssessment(
          UUID.fromString(data.calculationId),
          data.nino,
          data.preferredLanguage,
          data.customerType,
          data.agentRef,
          taxYearInRDSFormat))
    } else Left(taxYearValidation.head)
  }

}
