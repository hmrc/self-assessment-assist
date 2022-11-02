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
import uk.gov.hmrc.transactionalrisking.models.domain._
import uk.gov.hmrc.transactionalrisking.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.services.ServiceOutcome
import uk.gov.hmrc.transactionalrisking.services.eis.IntegrationFrameworkService
import uk.gov.hmrc.transactionalrisking.v1.TestData.CommonTestData.commonTestData._

import java.util.UUID


trait MockIntegrationFrameworkService extends MockFactory {

  val mockIntegrationFrameworkService: IntegrationFrameworkService = mock[IntegrationFrameworkService]


  object MockIntegrationFrameworkService {

    def getCalculationInfo(id: UUID, nino: String, correlationID: String): CallHandler[ServiceOutcome[CalculationInfo]] = {

      (mockIntegrationFrameworkService.getCalculationInfo(_: UUID, _: String, _: String))
        .expects(*, *, * /*simpleCalculationId, simpleNino*/).anyNumberOfTimes() returns (Right(ResponseWrapper(correlationID, CalculationInfo(simpleCalculationID, simpleNino, "2021-22"))))

    }
  }
}
