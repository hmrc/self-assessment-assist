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

package uk.gov.hmrc.transactionalrisking.v1.mocks.connectors

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transactionalrisking.v1.models.auth.RdsAuthCredentials
import uk.gov.hmrc.transactionalrisking.v1.services.ServiceOutcome
import uk.gov.hmrc.transactionalrisking.v1.services.rds.RdsConnector
import uk.gov.hmrc.transactionalrisking.v1.services.rds.models.request.RdsRequest
import uk.gov.hmrc.transactionalrisking.v1.services.rds.models.response.RdsAssessmentReport

import scala.concurrent.{ExecutionContext, Future}

trait MockRdsConnector extends MockFactory {

  val mockRdsConnector: RdsConnector = mock[RdsConnector]

  object MockRdsConnector {

    def submit(rdsRequest: RdsRequest  ): CallHandler[Future[ServiceOutcome[ RdsAssessmentReport]]] = {
      (mockRdsConnector.submit( _: RdsRequest,_:Option[RdsAuthCredentials])( _:HeaderCarrier,_: ExecutionContext,_:String))
        .expects(*, *, *, *, *)
    }

    def acknowledgeRds(request: RdsRequest): CallHandler[Future[ ServiceOutcome[ RdsAssessmentReport]]] = {
      (mockRdsConnector.acknowledgeRds(_: RdsRequest,_:Option[RdsAuthCredentials])(_: HeaderCarrier, _: ExecutionContext, _:String ))
        .expects(*, *, *, *, *)
    }

  }

}
