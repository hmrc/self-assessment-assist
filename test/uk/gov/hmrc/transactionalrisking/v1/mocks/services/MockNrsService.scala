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
import uk.gov.hmrc.transactionalrisking.v1.controllers.UserRequest
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.request.{NotableEventType, NrsSubmission}
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.response.{NrsFailure, NrsResponse}
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.{NrsOutcome, NrsService}

import java.time.OffsetDateTime
import scala.concurrent.Future

trait MockNrsService extends MockFactory {

  val mockNrsService: NrsService = mock[NrsService]

  object MockNrsService {

    def stubBuildNrsSubmission(nrsSubmission: NrsSubmission): CallHandler[Either[NrsFailure, NrsSubmission]] = {
      (mockNrsService.buildNrsSubmission(_: String, _: String, _: OffsetDateTime, _: UserRequest[_], _: NotableEventType)(_: String))
        .expects(*, *, *, *, *, *).anyNumberOfTimes()
        .returns((Right(nrsSubmission)))
    }

    def stubUnableToConstrucNrsSubmission(): CallHandler[Either[NrsFailure, NrsSubmission]] = {
      (mockNrsService.buildNrsSubmission(_: String, _: String, _: OffsetDateTime, _: UserRequest[_], _: NotableEventType)(_: String))
        .expects(*, *, *, *, *, *).anyNumberOfTimes()
        .returns(Left(NrsFailure.UnableToAttempt("no beaker token for user")))
    }

    def stubNrsSubmit(retNrsResponse: NrsResponse): CallHandler[Future[NrsOutcome]] = {
      (mockNrsService.submit(_: NrsSubmission)(_:HeaderCarrier,_: String))
        .expects(*, *, *).anyNumberOfTimes()
        .returns(Future.successful(Right(retNrsResponse)))
    }

    def stubFailureReportDueToException(): CallHandler[Future[NrsOutcome]] = {
      (mockNrsService.submit(_: NrsSubmission)(_: HeaderCarrier, _: String))
        .expects(*, *, *).anyNumberOfTimes()
        .returns(Future.successful(Left(NrsFailure.Exception("GatewayTimeout"))))
    }

    def stubAcknowledgement(retNrsResponse: NrsResponse): CallHandler[Future[NrsOutcome]] = {
      (mockNrsService.submit(_: NrsSubmission)(_: HeaderCarrier, _: String))
        .expects(*, *, *).anyNumberOfTimes()
        .returns(Future.successful(Right(retNrsResponse)))
    }

    def stubFailureAcknowledgementDueToException(): CallHandler[Future[NrsOutcome]] = {
      (mockNrsService.submit(_: NrsSubmission)(_: HeaderCarrier, _: String))
        .expects(*, *, *).anyNumberOfTimes()
        .returns(Future.successful(Left(NrsFailure.Exception("GatewayTimeout"))))
    }
  }
}
