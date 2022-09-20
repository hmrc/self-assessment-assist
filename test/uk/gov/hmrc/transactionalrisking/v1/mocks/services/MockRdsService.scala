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

package uk.gov.hmrc.transactionalrisking.v1.mocks.services

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transactionalrisking.controllers.UserRequest
import uk.gov.hmrc.transactionalrisking.models.auth.{AuthOutcome, UserDetails}
import uk.gov.hmrc.transactionalrisking.models.domain.{AssessmentReport, AssessmentRequestForSelfAssessment, FraudRiskReport, Origin}
import uk.gov.hmrc.transactionalrisking.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.services.nrs.models.request.AcknowledgeReportRequest
import uk.gov.hmrc.transactionalrisking.services.{EnrolmentsAuthService, ServiceOutcome}
import uk.gov.hmrc.transactionalrisking.services.rds.RdsService
import uk.gov.hmrc.transactionalrisking.v1.CommonTestData
import uk.gov.hmrc.transactionalrisking.v1.CommonTestData._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait MockRdsService extends MockFactory {

  val mockRdsService: RdsService = mock[RdsService]

  object MockRdsService {

    def submit(request: AssessmentRequestForSelfAssessment,
               fraudRiskReport: FraudRiskReport,
               origin: Origin): CallHandler[Future[ServiceOutcome[AssessmentReport]]] = {
      (mockRdsService.submit(_: AssessmentRequestForSelfAssessment, _: FraudRiskReport, _: Origin)(_: HeaderCarrier, _: ExecutionContext, _: UserRequest[_], _:String))
        .expects(*, *, simpleInternalOrigin, *, *, *, *) returns(Future(Right(ResponseWrapper(CommonTestData.correlationId,simpleAssementReport) )))
    }

//
//    def acknowlege(request: AcknowledgeReportRequest): CallHandler[Future[ServiceOutcome[AssessmentReport]]] = {
//      (mockRdsService.acknowlege(_: AcknowledgeReportRequest)(_: HeaderCarrier, _: ExecutionContext, _: UserRequest[_], _: String))
//        .expects(*, *, *, *, *)
//    }
//

    def acknowlege(request: AcknowledgeReportRequest): CallHandler[Future[Int]] = {
      (mockRdsService.acknowlege(_: AcknowledgeReportRequest)(_: HeaderCarrier, _: ExecutionContext, _: UserRequest[_], _:String))
        .expects(*, *, *, *, *)
    }

  }

}
