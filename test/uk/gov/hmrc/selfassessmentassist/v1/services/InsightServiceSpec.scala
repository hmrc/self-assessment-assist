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

package uk.gov.hmrc.selfassessmentassist.v1.services

import uk.gov.hmrc.selfassessmentassist.api.TestData.CommonTestData.{simpleNino, simpleNinoInvalid}
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{DownstreamError, ErrorWrapper}
import uk.gov.hmrc.selfassessmentassist.api.models.outcomes.ResponseWrapper
import uk.gov.hmrc.selfassessmentassist.support.{MockAppConfig, ServiceSpec}
import uk.gov.hmrc.selfassessmentassist.v1.mocks.connectors.MockInsightConnector
import uk.gov.hmrc.selfassessmentassist.v1.models.request.cip.FraudRiskReport
import uk.gov.hmrc.selfassessmentassist.v1.services.testData.InsightTestData.{fraudRiskReport, fraudRiskRequest}

import scala.concurrent.Future

class InsightServiceSpec extends ServiceSpec with MockAppConfig {

  class Test extends MockInsightConnector {
    val service = new InsightService(mockInsightConnector)
  }

  "Given CIP insight service is available" when {
    "the assess method is called with valid request to fetch fraud risk details then" must {
      "return the expected result" in new Test {
        MockInsightConnector.assess(fraudRiskRequest(simpleNino)) returns Future.successful(Right(ResponseWrapper(correlationId, fraudRiskReport)))

        val fraudRiskReportSO: ServiceOutcome[FraudRiskReport] = await(service.assess(fraudRiskRequest(simpleNino)))
        fraudRiskReportSO shouldBe Right(ResponseWrapper(correlationId, fraudRiskReport))
      }
    }

    "the assess method is called with invalid request to fetch fraud risk details then" must {
      "return error" in new Test {
        MockInsightConnector.assess(fraudRiskRequest(simpleNinoInvalid)) returns Future.successful(Left(ErrorWrapper(correlationId, DownstreamError)))

        val fraudRiskReportSO: ServiceOutcome[FraudRiskReport] = await(service.assess(fraudRiskRequest(simpleNinoInvalid)))
        fraudRiskReportSO shouldBe Left(ErrorWrapper(correlationId, DownstreamError))
      }
    }
  }

}
