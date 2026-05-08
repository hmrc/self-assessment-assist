/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.{HttpException, HttpResponse, StringContextOps}
import uk.gov.hmrc.selfassessmentassist.api.TestData.CommonTestData.{simpleFraudRiskReport, simpleFraudRiskRequest}
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{ErrorWrapper, InternalError}
import uk.gov.hmrc.selfassessmentassist.api.models.outcomes.ResponseWrapper
import uk.gov.hmrc.selfassessmentassist.support.{ConnectorSpec, MockAppConfig}
import uk.gov.hmrc.selfassessmentassist.v1.mocks.MockHttpClient
import uk.gov.hmrc.selfassessmentassist.v1.models.request.cip.{FraudRiskReport, FraudRiskRequest}

import java.net.URL
import java.util.Base64
import scala.concurrent.Future

class InsightConnectorSpec extends ConnectorSpec with MockAppConfig with MockHttpClient {

  private val insightBaseUrl: String = s"$baseUrl/fraud"
  private val insightUrl: URL        = url"$insightBaseUrl"

  private val username: String    = "some-user-name"
  private val token: String       = "some-token"
  private val credentials: String = Base64.getEncoder.encodeToString(s"$username:$token".getBytes)

  private val expectedHeaders: Seq[(String, String)] = Seq("Authorization" -> s"Basic $credentials")

  private val insightRequest: FraudRiskRequest = simpleFraudRiskRequest
  private val insightRequestJson: JsValue      = Json.toJson(insightRequest)
  private val successResponse: FraudRiskReport = simpleFraudRiskReport
  private val successResponseJson: String      = Json.toJson(successResponse).toString

  private trait Test {
    MockedAppConfig.cipFraudServiceBaseUrl returns insightBaseUrl
    MockedAppConfig.cipFraudUsername returns username
    MockedAppConfig.cipFraudToken returns token

    val connector: InsightConnector = new InsightConnector(mockHttpClient, mockAppConfig)

    def mockInsightCall(response: Future[HttpResponse]): Unit =
      MockedHttpClient
        .post(insightUrl, insightRequestJson, expectedHeaders)
        .returns(response)

  }

  "InsightConnector" when {
    ".fraudRiskHeaders" should {
      "return correct headers" in new Test {
        connector.fraudRiskHeaders() shouldBe expectedHeaders
      }
    }

    ".assess" should {
      "return a successful response" when {
        "insight call is successful" in new Test {
          mockInsightCall(Future.successful(HttpResponse(OK, successResponseJson)))
          await(connector.assess(insightRequest)) shouldBe Right(ResponseWrapper(correlationId, successResponse))
        }
      }

      "return an error response" when {
        "insight call returns an invalid JSON" in new Test {
          mockInsightCall(Future.successful(HttpResponse(OK, """{ "an": "invalid-json" }""")))
          await(connector.assess(insightRequest)) shouldBe Left(ErrorWrapper(correlationId, InternalError))
        }

        "insight call returns an unexpected status" in new Test {
          mockInsightCall(Future.successful(HttpResponse(SERVICE_UNAVAILABLE)))
          await(connector.assess(insightRequest)) shouldBe Left(ErrorWrapper(correlationId, InternalError))
        }

        "insight call returns an exception" in new Test {
          mockInsightCall(Future.failed(new HttpException("test", INTERNAL_SERVER_ERROR)))
          await(connector.assess(insightRequest)) shouldBe Left(ErrorWrapper(correlationId, InternalError))
        }
      }
    }

  }

}
