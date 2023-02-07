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

package uk.gov.hmrc.transactionalrisking.v1.services.ifs

import akka.actor.{ActorSystem, Scheduler}
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.MimeTypes
import play.api.test.Injecting
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.transactionalrisking.support.{ConnectorSpec, MockAppConfig}
import uk.gov.hmrc.transactionalrisking.v1.models.errors.{DownstreamError, ErrorWrapper}
import uk.gov.hmrc.transactionalrisking.v1.services.ifs.models.request.IFRequest
import uk.gov.hmrc.transactionalrisking.v1.services.ifs.models.response.IfsResponse


class IfsConnectorSpec extends ConnectorSpec
  with BeforeAndAfterAll
  with GuiceOneAppPerSuite
  with Injecting
  with MockAppConfig {

    val actorSystem: ActorSystem = inject[ActorSystem]
    implicit val scheduler: Scheduler = actorSystem.scheduler

    var port: Int = _
    val reportId = "12345"
    val apiKeyValue = "api-key"
    val url = "/interaction-data/store-interactions"

    private val ifsRequest: IFRequest = FullRequestTestData.correctModel
    private val ifsSubmissionJsonString: String = FullRequestTestData.correctJsonString

    val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

    class Test() {
      MockedAppConfig.ifsBaseUrl returns (s"http://localhost:$port/interaction-data/store-interactions")
      MockedAppConfig.ifsApiKey returns apiKeyValue

      val connector = new IfsConnector(httpClient, mockAppConfig)

    }

    override def beforeAll(): Unit = {
      wireMockServer.start()
      port = wireMockServer.port()
    }

    override def afterAll(): Unit =
      wireMockServer.stop()

    "IFSConnector" when {
      "successful" must {
        "return the response" in new Test() {
          wireMockServer.stubFor(
            post(urlPathEqualTo(url))
              .withHeader("Content-Type", equalTo(MimeTypes.JSON))
              .withHeader("X-API-Key", equalTo(apiKeyValue))
              .withRequestBody(equalToJson(ifsSubmissionJsonString, true, false))
              .willReturn(aResponse()
                .withStatus(NO_CONTENT)))

          await(connector.submit(ifsRequest)) shouldBe Right(IfsResponse())
        }
      }

      "unsuccessful" must {
        "service unavailable" in new Test() {
          wireMockServer.stubFor(
            post(urlPathEqualTo(url))
              .withHeader("Content-Type", equalTo(MimeTypes.JSON))
              .withHeader("X-API-Key", equalTo(apiKeyValue))
              .withRequestBody(equalToJson(ifsSubmissionJsonString, true, false))
              .willReturn(aResponse()
                .withStatus(SERVICE_UNAVAILABLE)))

          await(connector.submit(ifsRequest)) shouldBe Left(ErrorWrapper("f2fb30e5-4ab6-4a29-b3c1-c00000011111", DownstreamError))
        }

        "bad request" in new Test() {
          wireMockServer.stubFor(
            post(urlPathEqualTo(url))
              .withHeader("Content-Type", equalTo(MimeTypes.JSON))
              .withHeader("X-API-Key", equalTo(apiKeyValue))
              .withRequestBody(equalToJson(ifsSubmissionJsonString, true, false))
              .willReturn(aResponse()
                .withStatus(BAD_REQUEST)))

          await(connector.submit(ifsRequest)) shouldBe Left(ErrorWrapper("f2fb30e5-4ab6-4a29-b3c1-c00000011111", DownstreamError))
        }
      }

    }
}