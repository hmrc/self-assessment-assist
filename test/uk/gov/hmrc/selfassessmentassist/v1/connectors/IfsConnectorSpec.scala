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

import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.JsValue
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.{HttpException, HttpResponse, StringContextOps}
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{ErrorWrapper, InternalError}
import uk.gov.hmrc.selfassessmentassist.support.{ConnectorSpec, MockAppConfig}
import uk.gov.hmrc.selfassessmentassist.v1.mocks.MockHttpClient
import uk.gov.hmrc.selfassessmentassist.v1.models.request.ifs.IFRequest
import uk.gov.hmrc.selfassessmentassist.v1.models.response.ifs.IfsResponse
import uk.gov.hmrc.selfassessmentassist.v1.services.testData.IfsTestData
import uk.gov.hmrc.selfassessmentassist.v1.services.testData.IfsTestData.{correctJson, correctModel}

import java.net.URL
import scala.concurrent.Future

class IfsConnectorSpec extends ConnectorSpec with MockAppConfig with MockHttpClient {

  private val ifsBaseUrl: String = s"$baseUrl/interaction-data/store-interactions"
  private val ifsUrl: URL        = url"$ifsBaseUrl"

  private val ifsToken: String = "ABCD1234"
  private val ifsEnv: String   = "local"

  private val ifsEnvironmentHeaders: Option[Seq[String]] = Some(
    Seq("Accept", "Content-Type", "Location", "X-Request-Timestamp", "X-Session-Id", "X-Request-Id")
  )

  private val expectedHeaders: Seq[(String, String)] = Seq(
    "Environment"            -> ifsEnv,
    "CorrelationId"          -> correlationId,
    HeaderNames.CONTENT_TYPE -> s"${MimeTypes.JSON};charset=UTF-8",
    "accept"                 -> "*/*",
    "Authorization"          -> s"Bearer $ifsToken"
  )

  private val ifsRequest: IFRequest = correctModel
  private val ifsJson: JsValue      = correctJson

  private trait Test {
    MockedAppConfig.ifsBaseUrl returns ifsBaseUrl
    MockedAppConfig.ifsToken returns ifsToken
    MockedAppConfig.ifsEnv returns ifsEnv
    MockedAppConfig.ifsEnvironmentHeaders returns ifsEnvironmentHeaders

    val connector: IfsConnector = new IfsConnector(mockHttpClient, mockAppConfig)

    def mockIfsCall(response: Future[HttpResponse]): Unit =
      MockedHttpClient
        .post(ifsUrl, ifsJson, expectedHeaders)
        .returns(response)

  }

  "IfsConnector" when {
    ".submit" should {
      "return a successful response" when {
        "IFS call is successful" in new Test {
          mockIfsCall(Future.successful(HttpResponse(NO_CONTENT)))
          await(connector.submit(ifsRequest)) shouldBe Right(IfsResponse())
        }
      }

      "return an error response" when {
        "IFS call returns an unexpected status" in new Test {
          mockIfsCall(Future.successful(HttpResponse(SERVICE_UNAVAILABLE)))
          await(connector.submit(ifsRequest)) shouldBe Left(ErrorWrapper(correlationId, InternalError))
        }

        "IFS call returns HttpException" in new Test {
          mockIfsCall(Future.failed(new HttpException("test", INTERNAL_SERVER_ERROR)))
          await(connector.submit(ifsRequest)) shouldBe Left(ErrorWrapper(correlationId, InternalError))
        }
      }
    }
  }

}
