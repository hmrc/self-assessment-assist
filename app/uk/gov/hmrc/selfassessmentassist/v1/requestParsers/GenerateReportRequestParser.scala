/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.selfassessmentassist.v1.requestParsers

import uk.gov.hmrc.selfassessmentassist.api.models.domain.TaxYear
import uk.gov.hmrc.selfassessmentassist.api.models.errors.MtdError
import uk.gov.hmrc.selfassessmentassist.api.requestParsers.RequestParser
import uk.gov.hmrc.selfassessmentassist.api.requestParsers.validators.validations.TaxYearValidation
import uk.gov.hmrc.selfassessmentassist.v1.models.domain.AssessmentRequestForSelfAssessment
import uk.gov.hmrc.selfassessmentassist.v1.models.request.GenerateReportRawData
import uk.gov.hmrc.selfassessmentassist.v1.requestParsers.validators.GenerateReportValidator

import java.util.UUID
import javax.inject.{Inject, Singleton}

@Singleton
class GenerateReportRequestParser @Inject() (val validator: GenerateReportValidator)
    extends RequestParser[GenerateReportRawData, AssessmentRequestForSelfAssessment] {

  override protected def requestFor(data: GenerateReportRawData): Either[MtdError, AssessmentRequestForSelfAssessment] = {
    val taxYearValidation = TaxYearValidation.validate(data.taxYear)
    if (taxYearValidation.isEmpty) {
      Right(
        AssessmentRequestForSelfAssessment(
          UUID.fromString(data.calculationId),
          data.nino,
          data.preferredLanguage,
          data.customerType,
          data.agentRef,
          TaxYear.fromMtd(data.taxYear)))
    } else Left(taxYearValidation.head)
  }

}
