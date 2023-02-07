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

package uk.gov.hmrc.transactionalrisking.v1.mocks.services

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transactionalrisking.v1.models.auth.UserDetails
import uk.gov.hmrc.transactionalrisking.v1.models.domain.{AssessmentReport, AssessmentRequestForSelfAssessment}
import uk.gov.hmrc.transactionalrisking.v1.models.errors.{ErrorWrapper, MtdError}
import uk.gov.hmrc.transactionalrisking.v1.services.ifs.models.response.IfsResponse
import uk.gov.hmrc.transactionalrisking.v1.services.ifs.{IfsOutcome, IfsService}
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.request.AcknowledgeReportRequest
import uk.gov.hmrc.transactionalrisking.v1.services.rds.models.response.RdsAssessmentReport

import java.time.LocalDateTime
import scala.concurrent.Future

trait MockIfsService extends MockFactory {

  val mockIfsService: IfsService = mock[IfsService]

  object MockIfsService {

    def stubGenerateReportSubmit(assesmentRerport: AssessmentReport, localDateTime: LocalDateTime, assessmentRequestForSelfAssessment: AssessmentRequestForSelfAssessment): CallHandler[Future[IfsOutcome]] = {
      (mockIfsService.submitGenerateReportMessage(_: AssessmentReport, _: LocalDateTime, _: AssessmentRequestForSelfAssessment, _: RdsAssessmentReport)(_: HeaderCarrier, _: String))
        .expects(assesmentRerport, localDateTime, *, *, *, *).anyNumberOfTimes()
        .returns(Future.successful(Right(IfsResponse())))
    }

    def stubFailedSubmit(assesmentRerport: AssessmentReport, localDateTime: LocalDateTime, assessmentRequestForSelfAssessment: AssessmentRequestForSelfAssessment, mtdError: MtdError): CallHandler[Future[IfsOutcome]] = {
      (mockIfsService.submitGenerateReportMessage(_: AssessmentReport, _: LocalDateTime, _: AssessmentRequestForSelfAssessment, _: RdsAssessmentReport)(_: HeaderCarrier, _: String))
        .expects(assesmentRerport, localDateTime, *, *, *, *).anyNumberOfTimes()
        .returns(Future.successful(Left(ErrorWrapper(assesmentRerport.rdsCorrelationId, mtdError))))
    }

    def stubAcknowledgeSubmit() = {
      (mockIfsService.submitAcknowledgementMessage(_: AcknowledgeReportRequest, _: RdsAssessmentReport, _: UserDetails)(_: HeaderCarrier, _: String))
        .expects(*, *, *, *, *).anyNumberOfTimes()
        .returns(Future.successful(Right(IfsResponse())))
    }
  }

}