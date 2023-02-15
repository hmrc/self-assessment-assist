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

package uk.gov.hmrc.transactionalrisking.v1.services.nrs

import akka.actor.{ActorSystem, Scheduler}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.MimeTypes
import play.api.libs.json.{JsValue, Json}
import play.api.test.Injecting
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.transactionalrisking.support.{ConnectorSpec, MockAppConfig}
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.request.NrsSubmission
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.response.NrsFailure.Exception
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.response.{NrsFailure, NrsResponse}

import scala.concurrent.duration.{DurationInt, FiniteDuration}


class NrsConnectorSpec extends ConnectorSpec
  with BeforeAndAfterAll
  with GuiceOneAppPerSuite
  with Injecting
  with MockAppConfig {

  val actorSystem: ActorSystem = inject[ActorSystem]
  implicit val scheduler: Scheduler = actorSystem.scheduler

  var port: Int = _
  val reportId = "12345"
  val apiKeyValue = "api-key"
  val url = "/"
  val longDelays: List[FiniteDuration] = List(10.minutes)

  val successResponseJson: JsValue =
    Json.parse(
      """{
        |   "nrSubmissionId": "submissionId"
        |}""".stripMargin)

  private val nrsSubmission: NrsSubmission = FullRequestTestData.correctModel
  private val nrsSubmissionJsonString: String = FullRequestTestData.correctJsonString

  val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

  class Test(retryDelays: List[FiniteDuration] = List(100.millis)) {
    MockedAppConfig.nrsBaseUrl returns (s"http://localhost:$port")
    MockedAppConfig.nrsRetries returns retryDelays
    MockedAppConfig.nrsApiKey returns apiKeyValue

    val connector = new NrsConnector(httpClient, mockAppConfig)

  }

  override def beforeAll(): Unit = {
    wireMockServer.start()
    port = wireMockServer.port()
  }

  override def afterAll(): Unit =
    wireMockServer.stop()

  "NRSConnector" when {
    "immediately successful" must {
      "return the response" in new Test(longDelays) {
        wireMockServer.stubFor(
          post(urlPathEqualTo(url))
            .withHeader("Content-Type", equalTo(MimeTypes.JSON))
            .withHeader("X-API-Key", equalTo(apiKeyValue))
            .withRequestBody(equalToJson(nrsSubmissionJsonString, true, false))
            .willReturn(aResponse()
              .withBody(successResponseJson.toString)
              .withStatus(ACCEPTED)))

        await(connector.submit(nrsSubmission)) shouldBe Right(NrsResponse("submissionId"))
      }
    }

    "fails with 5xx status" must {
      "retry" in new Test {
        wireMockServer.stubFor(
          post(urlPathEqualTo(url))
            .withHeader("Content-Type", equalTo(MimeTypes.JSON))
            .withHeader("X-API-Key", equalTo(apiKeyValue))
            .inScenario("Retry")
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse()
              .withStatus(GATEWAY_TIMEOUT))
            .willSetStateTo("SUCCESS"))

        wireMockServer.stubFor(
          post(urlPathEqualTo(url))
            .withHeader("Content-Type", equalTo(MimeTypes.JSON))
            .withHeader("X-API-Key", equalTo(apiKeyValue))
            .inScenario("Retry")
            .whenScenarioStateIs("SUCCESS")
            .willReturn(aResponse()
              .withBody(successResponseJson.toString)
              .withStatus(ACCEPTED)))

        await(connector.submit(nrsSubmission)) shouldBe Right(NrsResponse("submissionId"))
      }

      "give up after all retries" in new Test {
        wireMockServer.stubFor(
          post(urlPathEqualTo(url))
            .withHeader("Content-Type", equalTo(MimeTypes.JSON))
            .withHeader("X-API-Key", equalTo(apiKeyValue))
            .willReturn(aResponse()
              .withStatus(GATEWAY_TIMEOUT)))

        await(connector.submit(nrsSubmission)) shouldBe Left(NrsFailure.ErrorResponse(GATEWAY_TIMEOUT))
      }
    }

    "fails with 4xx status" must {
      "give up" in new Test(longDelays) {
        wireMockServer.stubFor(
          post(urlPathEqualTo(url))
            .withHeader("Content-Type", equalTo(MimeTypes.JSON))
            .withHeader("X-API-Key", equalTo(apiKeyValue))
            .willReturn(aResponse()
              .withStatus(BAD_REQUEST)))

        await(connector.submit(nrsSubmission)) shouldBe Left(NrsFailure.ErrorResponse(BAD_REQUEST))
      }
    }

    "fails with exception" must {
      "give up" in new Test(longDelays) {

        wireMockServer.stubFor(
          post(urlPathEqualTo(url))
            .withHeader("Content-Type", equalTo(MimeTypes.JSON))
            .withHeader("X-API-Key", equalTo(apiKeyValue))
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)))

        await(connector.submit(nrsSubmission)) shouldBe Left(Exception("Connection reset by peer"))
      }
    }

    "fails because unparsable JSON returned" must {
      "give up" in new Test(longDelays) {
        wireMockServer.stubFor(
          post(urlPathEqualTo(url))
            .withHeader("Content-Type", equalTo(MimeTypes.JSON))
            .withHeader("X-API-Key", equalTo(apiKeyValue))
            .willReturn(aResponse()
              .withBody(
                """{
                  |   "badKey": "badValue"
                  |}""".stripMargin)
              .withStatus(ACCEPTED)))

        await(connector.submit(nrsSubmission)) shouldBe Left(Exception("JsResultException(errors:List((/nrSubmissionId,List(JsonValidationError(List(error.path.missing),ArraySeq())))))"))
      }
    }
  }
}
