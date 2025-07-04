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

package uk.gov.hmrc.selfassessmentassist.v1.connectors

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfassessmentassist.v1.models.request.ifs.IFRequest
import uk.gov.hmrc.selfassessmentassist.v1.services.IfsOutcome

import scala.concurrent.Future

trait MockIfsConnector extends TestSuite with MockFactory {

  val mockIfsConnector: IfsConnector = mock[IfsConnector]

  object MockIfsConnector {

    def submit(expectedPayload: IFRequest): CallHandler[Future[IfsOutcome]] = {
      (mockIfsConnector
        .submit(_: IFRequest)(_: HeaderCarrier, _: String))
        .expects(where { (ifsRequest: IFRequest, _: HeaderCarrier, _: String) =>
          ifsRequest == expectedPayload
        })
    }

  }

}
