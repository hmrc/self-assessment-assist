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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transactionalrisking.controllers.UserRequest
import uk.gov.hmrc.transactionalrisking.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.services.nrs.NrsService
import uk.gov.hmrc.transactionalrisking.services.nrs.models.request.{GenerateReportRequest, NotableEventType, NrsSubmission}
import uk.gov.hmrc.transactionalrisking.services.nrs.models.response.NrsResponse

import java.time.{Month, OffsetDateTime, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.transactionalrisking.v1.CommonTestData._

import scala.concurrent.ExecutionContext.Implicits.global

trait MockNrsService extends MockFactory {

  val mockNrsService: NrsService = mock[NrsService]

  object MockNrsService {

    def buildNrsSubmission(selfAssessmentSubmission: GenerateReportRequest,
                           submissionTimestamp: OffsetDateTime,
                           request: UserRequest[_], notableEventType: NotableEventType): CallHandler[NrsSubmission] = {
      (mockNrsService.buildNrsSubmission(_: GenerateReportRequest,
        _: OffsetDateTime,
        _: UserRequest[_], _: NotableEventType))
        .expects(*, *, *, *)
    }

    def submit(generateReportRequest: GenerateReportRequest, generatedNrsId: String, submissionTimestamp: OffsetDateTime, notableEventType: NotableEventType):
    CallHandler[Future[Option[NrsResponse]]] = {
      (mockNrsService.submit(_: GenerateReportRequest, _: String, _: OffsetDateTime, _: NotableEventType)
      (_: UserRequest[_], _: HeaderCarrier, _: ExecutionContext, _: String))
        .expects( *, *, *, simpeNotableEventType, *, *, *, *)
        .returns( Future(Some(simpleNRSResponse) ))
    }

  }

}
