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

package uk.gov.hmrc.transactionalrisking.v1.service.rds

import mockws.{MockWS, MockWSHelpers}
import org.scalatest.BeforeAndAfterAll
import play.api.mvc.Results.Ok
import play.api.test.Helpers._
import uk.gov.hmrc.transactionalrisking.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.services.rds.RdsConnector
import uk.gov.hmrc.transactionalrisking.support.{ConnectorSpec, MockAppConfig}
import uk.gov.hmrc.transactionalrisking.v1.CommonTestData


class RdsConnectorSpec extends ConnectorSpec
                          with RdsTestData
                          with MockAppConfig
                          with MockWSHelpers
                          with BeforeAndAfterAll {

  var port: Int = _

  val submitBaseUrl = s"http://localhost:$port/submit"
  val acknowledgeUrl = s"http://localhost:$port/acknowledge"

  val ws = MockWS {
    case (POST, submitBaseUrl) => Action { Ok(rdsSubmissionResponse) }
    case (POST, acknowledgeUrl) => Action { Ok }
  }

  override def afterAll(): Unit = {
    shutdownHelpers()
  }

  class Test {
    val connector = new RdsConnector(ws, mockAppConfig)
  }

  "NRSConnector" when {
    "submit method is called" must {
      "return the response if successful" in new Test {
        MockedAppConfig.rdsBaseUrlForSubmit returns submitBaseUrl
        await(connector.submit(rdsRequest)) shouldBe Right(ResponseWrapper(CommonTestData.internalCorrelationId, rdsAssessmentReport))
      }
    }

    "acknowledge method is called" must {
      "return the response if successful" in new Test {
        MockedAppConfig.rdsBaseUrlForAcknowledge returns acknowledgeUrl
        await(connector.acknowledgeRds(acknowledgeReportRequest)) shouldBe NO_CONTENT
      }
    }
  }
}
