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
import uk.gov.hmrc.transactionalrisking.v1.controllers.UserRequest
import uk.gov.hmrc.transactionalrisking.v1.models.domain.AssessmentReport
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.NrsService
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.request.AcknowledgeReportId
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.response.NrsResponse

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait MockNrsService extends MockFactory {

  val mockNrsService: NrsService = mock[NrsService]

  object MockNrsService {

    def stubAssessmentReport(retNrsResponse: NrsResponse) : CallHandler[Future[Option[NrsResponse]]] = {
      (mockNrsService.submit(_: AssessmentReport, _: OffsetDateTime)(_: UserRequest[_], _: HeaderCarrier, _: ExecutionContext, _: String))
        .expects(*, *, *, *, *, *).anyNumberOfTimes()
        .returns(Future(Some(retNrsResponse)))
    }

    def stubAcknowledgement(retNrsResponse: NrsResponse): CallHandler[Future[Option[NrsResponse]]] = {
      (mockNrsService.submit(_: AcknowledgeReportId, _: OffsetDateTime)(_: UserRequest[_], _: HeaderCarrier, _: ExecutionContext, _: String))
        .expects(*, *, *, *, *, *).anyNumberOfTimes()
        .returns(Future(Some(retNrsResponse)))
    }

  }
}
