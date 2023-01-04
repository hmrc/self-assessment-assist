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
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.NrsService
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.request.{NotableEventType, NrsSubmission, RequestData}
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.response.NrsResponse

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait MockNrsService extends MockFactory {

  val mockNrsService: NrsService = mock[NrsService]

  object MockNrsService {

    def buildNrsSubmission(selfAssessmentSubmission: RequestData,
                           submissionTimestamp: OffsetDateTime,
                           request: UserRequest[_], notableEventType: NotableEventType, taxYear: String)(corrrelationId: String): CallHandler[NrsSubmission] = {
      (mockNrsService.buildNrsSubmission(_: RequestData,
        _: OffsetDateTime,
        _: UserRequest[_], _: NotableEventType)(_: String))
        .expects(*, *, *, *, *).anyNumberOfTimes()
    }

    def submit(generateReportRequest: RequestData, submissionTimestamp: OffsetDateTime, notableEventType: NotableEventType, retNrsResponse: NrsResponse):
    CallHandler[Future[Option[NrsResponse]]] = {
      (mockNrsService.submit(_: RequestData, _: OffsetDateTime, _: NotableEventType)
      (_: UserRequest[_], _: HeaderCarrier, _: ExecutionContext, _: String))
        .expects(*, *, *, *, *, *, *).anyNumberOfTimes()
        .returns(Future(Some(retNrsResponse)))
    }

    def submitFail(generateReportRequest: RequestData, submissionTimestamp: OffsetDateTime, notableEventType: NotableEventType):
    CallHandler[Future[Option[NrsResponse]]] = {
      (mockNrsService.submit(_: RequestData, _: OffsetDateTime, _: NotableEventType)
      (_: UserRequest[_], _: HeaderCarrier, _: ExecutionContext, _: String))
        .expects(*, *, *, *, *, *, *).anyNumberOfTimes()
        .returns(Future(None))
    }
  }
}
