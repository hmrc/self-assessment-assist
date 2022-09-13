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

package uk.gov.hmrc.transactionalrisking.controllers.requestParsers

import uk.gov.hmrc.transactionalrisking.controllers.requestParsers.validators.AcknowledgeReportValidator
import uk.gov.hmrc.transactionalrisking.models.request.AcknowledgeReportRawData
import uk.gov.hmrc.transactionalrisking.services.nrs.models.request.{AcknowledgeReportRequest}

import javax.inject.Inject

class AcknowledgeRequestParser @Inject()(val validator: AcknowledgeReportValidator)
  extends RequestParser[AcknowledgeReportRawData, AcknowledgeReportRequest] {

  override protected def requestFor(data: AcknowledgeReportRawData): AcknowledgeReportRequest = {
    AcknowledgeReportRequest(data.nino, data.reportId)
  }
}
