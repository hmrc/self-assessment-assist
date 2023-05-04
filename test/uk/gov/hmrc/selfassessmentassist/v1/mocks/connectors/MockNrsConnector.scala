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

package uk.gov.hmrc.selfassessmentassist.v1.mocks.connectors

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfassessmentassist.v1.connectors.NrsConnector
import uk.gov.hmrc.selfassessmentassist.v1.services.nrs.NrsOutcome
import uk.gov.hmrc.selfassessmentassist.v1.services.nrs.models.request.NrsSubmission

import scala.concurrent.Future

trait MockNrsConnector extends MockFactory {

  val mockNrsConnector: NrsConnector = mock[NrsConnector]

  object MockNrsConnector {

    def submitNrs(expectedPayload: NrsSubmission): CallHandler[Future[NrsOutcome]] = {
      (mockNrsConnector
        .submit(_: NrsSubmission)(_: HeaderCarrier, _: String))
        .expects(where {
          (nrsSubmission: NrsSubmission, _: HeaderCarrier, _: String) => nrsSubmission == expectedPayload
        })
    }
  }

}
