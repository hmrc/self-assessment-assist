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
import uk.gov.hmrc.transactionalrisking.v1.TestData.CommonTestData.commonTestData._
import uk.gov.hmrc.transactionalrisking.v1.models.domain.CalculationInfo
import uk.gov.hmrc.transactionalrisking.v1.models.errors.{ErrorWrapper, MtdError}
import uk.gov.hmrc.transactionalrisking.v1.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.v1.services.ServiceOutcome
import uk.gov.hmrc.transactionalrisking.v1.services.eis.IntegrationFrameworkService

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}


trait MockIntegrationFrameworkService extends MockFactory {

  val mockIntegrationFrameworkService: IntegrationFrameworkService = mock[IntegrationFrameworkService]


  object MockIntegrationFrameworkService {

    def getCalculationInfo(id: UUID, nino: String): CallHandler[Future[ServiceOutcome[CalculationInfo]]] = {
      (mockIntegrationFrameworkService.getCalculationInfo(_: UUID, _: String)(_: ExecutionContext, _:String))
        .expects(*, *, *, *).anyNumberOfTimes()
        .returns(Future.successful(Right(ResponseWrapper(internalCorrelationIdImplicit, CalculationInfo(simpleCalculationId, simpleNino, "2021-22")))))
    }

    def getCalculationInfoFail(id: UUID, nino: String, error: MtdError): CallHandler[Future[ServiceOutcome[CalculationInfo]]] = {
      (mockIntegrationFrameworkService.getCalculationInfo(_: UUID, _: String)(_: ExecutionContext, _: String))
        .expects(*, *, *, *).anyNumberOfTimes()
        .returns(Future.successful(Left(ErrorWrapper(internalCorrelationIdImplicit, error))))
    }
  }
}
