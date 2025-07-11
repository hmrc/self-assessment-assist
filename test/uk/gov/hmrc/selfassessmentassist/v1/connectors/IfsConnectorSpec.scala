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

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.MimeTypes
import play.api.test.Injecting
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{ErrorWrapper, InternalError}
import uk.gov.hmrc.selfassessmentassist.support.{ConnectorSpec, MockAppConfig}
import uk.gov.hmrc.selfassessmentassist.v1.models.request.ifs.IFRequest
import uk.gov.hmrc.selfassessmentassist.v1.models.response.ifs.IfsResponse
import uk.gov.hmrc.selfassessmentassist.v1.services.testData.IfsTestData

class IfsConnectorSpec extends ConnectorSpec with BeforeAndAfterAll with GuiceOneAppPerSuite with Injecting with MockAppConfig {

  val httpClient: HttpClientV2 = app.injector.instanceOf[HttpClientV2]

  val reportId      = "12345"
  val ifsTokenValue = "ABCD1234"
  val ifsEnv        = "local"

  val ifsEnvironmentHeaders: Option[Seq[String]] = Some(
    Seq("Accept", "Content-Type", "Location", "X-Request-Timestamp", "X-Session-Id", "X-Request-Id"))

  val url                                     = "/interaction-data/store-interactions"
  private val ifsRequest: IFRequest           = IfsTestData.correctModel
  private val ifsSubmissionJsonString: String = IfsTestData.correctJsonString

  def port: Int = wireMockServer.port()

  override def beforeAll(): Unit = wireMockServer.start()

  override def afterAll(): Unit = wireMockServer.stop()

  class Test() {
    MockedAppConfig.ifsBaseUrl returns s"""http://localhost:$port/interaction-data/store-interactions"""
    MockedAppConfig.ifsToken returns ifsTokenValue
    MockedAppConfig.ifsEnv returns ifsEnv
    MockedAppConfig.ifsEnvironmentHeaders returns ifsEnvironmentHeaders

    val connector = new IfsConnector(httpClient, mockAppConfig)

  }

  "IFSConnector" when {
    "successful" must {
      "return the response" in new Test() {
        wireMockServer.stubFor(
          post(urlPathEqualTo(url))
            .withRequestBody(equalToJson(ifsSubmissionJsonString, true, false))
            .withHeader("Content-Type", equalTo(s"${MimeTypes.JSON};charset=UTF-8"))
            .withHeader("Authorization", equalTo(s"Bearer $ifsTokenValue"))
            .willReturn(aResponse()
              .withStatus(NO_CONTENT)))

        await(connector.submit(ifsRequest)) shouldBe Right(IfsResponse())
      }
    }

    "service unavailable" must {
      "return downstream error" in new Test() {
        wireMockServer.stubFor(
          post(urlPathEqualTo(url))
            .withRequestBody(equalToJson(ifsSubmissionJsonString, true, false))
            .withHeader("Content-Type", equalTo(s"${MimeTypes.JSON};charset=UTF-8"))
            .withHeader("Authorization", equalTo(s"Bearer $ifsTokenValue"))
            .willReturn(aResponse()
              .withStatus(SERVICE_UNAVAILABLE)))

        await(connector.submit(ifsRequest)) shouldBe Left(ErrorWrapper("f2fb30e5-4ab6-4a29-b3c1-c00000011111", InternalError))
      }
    }

    "bad request" must {
      "return downstream error" in new Test() {
        wireMockServer.stubFor(
          post(urlPathEqualTo(url))
            .withRequestBody(equalToJson(ifsSubmissionJsonString, true, false))
            .withHeader("Content-Type", equalTo(s"${MimeTypes.JSON};charset=UTF-8"))
            .withHeader("Authorization", equalTo(s"Bearer $ifsTokenValue"))
            .willReturn(aResponse()
              .withStatus(BAD_REQUEST)))

        await(connector.submit(ifsRequest)) shouldBe Left(ErrorWrapper("f2fb30e5-4ab6-4a29-b3c1-c00000011111", InternalError))
      }
    }

  }

}
