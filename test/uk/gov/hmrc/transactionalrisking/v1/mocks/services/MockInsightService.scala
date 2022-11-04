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
import uk.gov.hmrc.transactionalrisking.models.domain.{FraudRiskReport, FraudRiskRequest}
import uk.gov.hmrc.transactionalrisking.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.services.ServiceOutcome
import uk.gov.hmrc.transactionalrisking.services.cip.InsightService
import uk.gov.hmrc.transactionalrisking.v1.TestData.CommonTestData.commonTestData.{internalCorrelationIDImplicit, simpleFraudRiskReport}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

trait MockInsightService extends MockFactory {

  val mockInsightService: InsightService = mock[InsightService]

  object MockInsightService {

    def assess(fraudRiskRequest: FraudRiskRequest): CallHandler[Future[ServiceOutcome[FraudRiskReport]]] = {
      (mockInsightService.assess(_: FraudRiskRequest)(_: ExecutionContext,_: String))
        .expects(*,*,*).returns(Future(Right(ResponseWrapper(internalCorrelationIDImplicit, simpleFraudRiskReport))))
    }

  }

}
