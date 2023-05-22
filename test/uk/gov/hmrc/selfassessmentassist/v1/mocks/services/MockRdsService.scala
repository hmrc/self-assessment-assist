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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfassessmentassist.api.controllers.UserRequest
import uk.gov.hmrc.selfassessmentassist.api.models.domain.Origin
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{ErrorWrapper, MtdError}
import uk.gov.hmrc.selfassessmentassist.api.models.outcomes.ResponseWrapper
import uk.gov.hmrc.selfassessmentassist.v1.models.domain.{AssessmentReportWrapper, AssessmentRequestForSelfAssessment}
import uk.gov.hmrc.selfassessmentassist.v1.models.request.cip.FraudRiskReport
import uk.gov.hmrc.selfassessmentassist.v1.models.request.nrs.AcknowledgeReportRequest
import uk.gov.hmrc.selfassessmentassist.v1.models.response.rds.RdsAssessmentReport
import uk.gov.hmrc.selfassessmentassist.v1.services.{RdsService, ServiceOutcome}
import uk.gov.hmrc.selfassessmentassist.api.TestData.CommonTestData._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait MockRdsService extends MockFactory {

  val mockRdsService: RdsService = mock[RdsService]

  object MockRdsService {

    def submit(request: AssessmentRequestForSelfAssessment,
               fraudRiskReport: FraudRiskReport,
               origin: Origin,
               simpleAssessmentReportWrapper: AssessmentReportWrapper): CallHandler[Future[ServiceOutcome[AssessmentReportWrapper]]] = {
      (mockRdsService
        .submit(_: AssessmentRequestForSelfAssessment, _: FraudRiskReport)(_: HeaderCarrier, _: ExecutionContext, _: UserRequest[_], _: String))
        .expects(*, *, *, *, *, *) returns Future(Right(ResponseWrapper(correlationId, simpleAssessmentReportWrapper)))
    }

    def submitFail(request: AssessmentRequestForSelfAssessment,
                   fraudRiskReport: FraudRiskReport,
                   origin: Origin,
                   error: MtdError): CallHandler[Future[ServiceOutcome[AssessmentReportWrapper]]] = {
      (mockRdsService
        .submit(_: AssessmentRequestForSelfAssessment, _: FraudRiskReport)(_: HeaderCarrier, _: ExecutionContext, _: UserRequest[_], _: String))
        .expects(*, *, *, *, *, *) returns Future(Left(ErrorWrapper(correlationId, error)))
    }

    def acknowlegeRds(request: AcknowledgeReportRequest): CallHandler[Future[ServiceOutcome[RdsAssessmentReport]]] = {
      (mockRdsService
        .acknowledge(_: AcknowledgeReportRequest)(_: HeaderCarrier, _: ExecutionContext, _: String))
        .expects(*, *, *, *)
        .anyNumberOfTimes()
        .returns(Future(Right(ResponseWrapper(correlationId, simpleAcknowledgeNewRdsAssessmentReport))))
    }

    def acknowlegeRdsFail(request: AcknowledgeReportRequest, error: MtdError): CallHandler[Future[ServiceOutcome[RdsAssessmentReport]]] = {
      (mockRdsService
        .acknowledge(_: AcknowledgeReportRequest)(_: HeaderCarrier, _: ExecutionContext, _: String))
        .expects(*, *, *, *)
        .anyNumberOfTimes()
        .returns(Future(Left(ErrorWrapper(correlationId, error))))
    }

  }

}
