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
import org.scalatest.TestSuite
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfassessmentassist.api.TestData.CommonTestData._
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{ErrorWrapper, MtdError}
import uk.gov.hmrc.selfassessmentassist.api.models.outcomes.ResponseWrapper
import uk.gov.hmrc.selfassessmentassist.v1.models.request.cip.{FraudRiskReport, FraudRiskRequest}
import uk.gov.hmrc.selfassessmentassist.v1.services.{InsightService, ServiceOutcome}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait MockInsightService extends TestSuite with MockFactory {

  val mockInsightService: InsightService = mock[InsightService]

  object MockInsightService {

    def assess(fraudRiskRequest: FraudRiskRequest): CallHandler[Future[ServiceOutcome[FraudRiskReport]]] = {
      (mockInsightService
        .assess(_: FraudRiskRequest)(_: HeaderCarrier, _: String))
        .expects(*, *, *)
        .returns(Future(Right(ResponseWrapper(correlationId, simpleFraudRiskReport))))
    }

    def assessFail(fraudRiskRequest: FraudRiskRequest, error: MtdError): CallHandler[Future[ServiceOutcome[FraudRiskReport]]] = {
      (mockInsightService
        .assess(_: FraudRiskRequest)(_: HeaderCarrier, _: String))
        .expects(*, *, *)
        .returns(Future(Left(ErrorWrapper(correlationId, error))))
    }

  }

}
