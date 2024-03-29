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

package uk.gov.hmrc.selfassessmentassist.v1.requestParsers.validators

import uk.gov.hmrc.selfassessmentassist.api.models.errors.MtdError
import uk.gov.hmrc.selfassessmentassist.api.requestParsers.validators.Validator
import uk.gov.hmrc.selfassessmentassist.api.requestParsers.validators.validations.{NinoValidation, ReportIdValidation}
import uk.gov.hmrc.selfassessmentassist.v1.models.request.AcknowledgeReportRawData

import javax.inject.Singleton

@Singleton
class AcknowledgeReportValidator extends Validator[AcknowledgeReportRawData] {

  private val validationSet = List(parameterFormatValidation)

  private def parameterFormatValidation: AcknowledgeReportRawData => List[List[MtdError]] = { data =>
    List(NinoValidation.validate(data.nino), ReportIdValidation.validate(data.reportId))
  }

  override def validate(data: AcknowledgeReportRawData): List[MtdError] = {
    run(validationSet, data).distinct
  }

}
