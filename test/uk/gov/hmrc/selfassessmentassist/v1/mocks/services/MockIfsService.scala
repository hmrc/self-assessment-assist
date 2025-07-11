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

package uk.gov.hmrc.selfassessmentassist.v1.mocks.services

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfassessmentassist.api.models.auth.UserDetails
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{ErrorWrapper, MtdError}
import uk.gov.hmrc.selfassessmentassist.v1.models.domain.{AssessmentReportWrapper, AssessmentRequestForSelfAssessment}
import uk.gov.hmrc.selfassessmentassist.v1.models.request.nrs.AcknowledgeReportRequest
import uk.gov.hmrc.selfassessmentassist.v1.models.response.ifs.IfsResponse
import uk.gov.hmrc.selfassessmentassist.v1.models.response.rds.RdsAssessmentReport
import uk.gov.hmrc.selfassessmentassist.v1.services.{IfsOutcome, IfsService}

import scala.concurrent.Future

trait MockIfsService extends TestSuite with MockFactory {

  val mockIfsService: IfsService = mock[IfsService]

  object MockIfsService {

    def stubGenerateReportSubmit(assessmentReportWrapper: AssessmentReportWrapper,
                                 assessmentRequestForSelfAssessment: AssessmentRequestForSelfAssessment): CallHandler[Future[IfsOutcome]] = {
      (
        mockIfsService
          .submitGenerateReportMessage(_: AssessmentReportWrapper, _: AssessmentRequestForSelfAssessment)(
            _: HeaderCarrier,
            _: String
          )
        )
        .expects(assessmentReportWrapper, *, *, *)
        .anyNumberOfTimes()
        .returns(Future.successful(Right(IfsResponse())))
    }

    def stubFailedSubmit(assessmentReportWrapper: AssessmentReportWrapper,
                         assessmentRequestForSelfAssessment: AssessmentRequestForSelfAssessment,
                         mtdError: MtdError): CallHandler[Future[IfsOutcome]] = {
      (
        mockIfsService
          .submitGenerateReportMessage(_: AssessmentReportWrapper, _: AssessmentRequestForSelfAssessment)(
            _: HeaderCarrier,
            _: String
          )
        )
        .expects(assessmentReportWrapper, *, *, *)
        .anyNumberOfTimes()
        .returns(Future.successful(Left(ErrorWrapper(assessmentReportWrapper.report.rdsCorrelationId, mtdError))))
    }

    def stubAcknowledgeSubmit(): CallHandler[Future[IfsOutcome]] = {
      (mockIfsService
        .submitAcknowledgementMessage(_: AcknowledgeReportRequest, _: RdsAssessmentReport, _: UserDetails)(_: HeaderCarrier, _: String))
        .expects(*, *, *, *, *)
        .anyNumberOfTimes()
        .returns(Future.successful(Right(IfsResponse())))
    }

    def submitGenerateReportNeverCalled(): CallHandler[Future[IfsOutcome]] = {
      (
        mockIfsService
          .submitGenerateReportMessage(_: AssessmentReportWrapper, _: AssessmentRequestForSelfAssessment)(
            _: HeaderCarrier,
            _: String
          )
        )
        .expects(*, *, *, *)
        .never()
    }

    def submitAcknowledgmentNeverCalled(): CallHandler[Future[IfsOutcome]] = {
      (mockIfsService
        .submitAcknowledgementMessage(_: AcknowledgeReportRequest, _: RdsAssessmentReport, _: UserDetails)(_: HeaderCarrier, _: String))
        .expects(*, *, *, *, *)
        .never()
    }

  }

}
