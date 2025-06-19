/*
 * Copyright 2025 HM Revenue & Customs
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
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.apache.pekko.actor.{ActorSystem, Scheduler}
import org.mongodb.scala.model.Filters
import org.scalatest.BeforeAndAfterAll
import play.api.http.MimeTypes
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.Injecting
import play.api.{Application, Environment, Mode}
import uk.gov.hmrc.http.test.HttpClientV2Support
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport
import uk.gov.hmrc.mongo.workitem.WorkItem
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing
import uk.gov.hmrc.selfassessmentassist.support.{ConnectorSpec, MockAppConfig}
import uk.gov.hmrc.selfassessmentassist.utils.Retrying.fibonacciDelays
import uk.gov.hmrc.selfassessmentassist.v1.models.request.nrs.{NrsSubmission, NrsSubmissionWorkItem}
import uk.gov.hmrc.selfassessmentassist.v1.models.response.nrs.{NrsFailure, NrsResponse}
import uk.gov.hmrc.selfassessmentassist.v1.repositories.NrsSubmissionWorkItemRepository
import uk.gov.hmrc.selfassessmentassist.v1.services.testData.NrsTestData

import java.time.Duration
import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class NrsConnectorSpec
  extends ConnectorSpec
    with BeforeAndAfterAll
    with Injecting
    with MockAppConfig
    with CleanMongoCollectionSupport
    with LogCapturing
    with HttpClientV2Support {

  private val nrsSubmission: NrsSubmission = NrsTestData.correctModel
  private val nrsSubmissionJsonString: String = NrsTestData.correctJsonString
  override implicit val defaultTimeout: FiniteDuration = 20.seconds

  override lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure("metrics.enabled" -> "false")
    .build()

  def port: Int = wireMockServer.port()

  val actorSystem: ActorSystem = inject[ActorSystem]
  implicit val scheduler: Scheduler = actorSystem.scheduler

  // Long delays to force a test to timeout if it does retry when we're not expecting it...
  val longDelays: List[FiniteDuration] = List(10.minutes)

  val successResponseJson: JsValue =
    Json.parse(
      """{
        |   "nrSubmissionId": "submissionId"
        |}""".stripMargin)

  override def beforeEach(): Unit = {
    super.beforeEach()
    wireMockServer.resetAll()
  }

  override def beforeAll(): Unit = wireMockServer.start()

  override def afterAll(): Unit = wireMockServer.stop()

  val url = "/"
  val apiKeyValue = "api-key"

  class Test(retryDelays: List[FiniteDuration] = List(100.millis)) {
    MockedAppConfig.nrsBaseUrl.returns(s"http://localhost:$port")
    MockedAppConfig.nrsRetries returns retryDelays
    MockedAppConfig.nrsApiKey returns apiKeyValue
    MockedAppConfig.nrsSchedulerInitialDelay returns 100.millis
    MockedAppConfig.nrsSchedulerDelay returns 200.millis
    MockedAppConfig.nrsFailedBeforeSeconds returns 1
    MockedAppConfig.nrsInProgressRetryAfter returns Duration.ofMinutes(2)

    val repository: NrsSubmissionWorkItemRepository = new NrsSubmissionWorkItemRepository(
      appConfig = mockAppConfig,
      mongoComponent = mongoComponent
    )

    def findByPayload(nrsSubmission: NrsSubmission): Future[Option[WorkItem[NrsSubmissionWorkItem]]] =
      repository.collection
        .find(
          Filters.equal("item.nrsSubmission.metadata.payloadSha256Checksum", nrsSubmission.metadata.payloadSha256Checksum)
        )
        .headOption()

    val connector: NrsConnector = new NrsConnector(httpClientV2, mockAppConfig, repository)
  }

  "NrsConnector" when {
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

        val result: Option[WorkItem[NrsSubmissionWorkItem]] = await(findByPayload(nrsSubmission))

        result shouldBe None
      }
    }

    "fails with 5xx status" must {
      "retry" in new Test {
        wireMockServer.stubFor(
          post(urlPathEqualTo(url))
            .withHeader("Content-Type", equalTo(MimeTypes.JSON))
            .withHeader("X-API-Key", equalTo(apiKeyValue))
            .withRequestBody(equalToJson(nrsSubmissionJsonString, true, false))
            .inScenario("Retry")
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse()
              .withStatus(GATEWAY_TIMEOUT))
            .willSetStateTo("SUCCESS"))

        wireMockServer.stubFor(
          post(urlPathEqualTo(url))
            .withHeader("Content-Type", equalTo(MimeTypes.JSON))
            .withHeader("X-API-Key", equalTo(apiKeyValue))
            .withRequestBody(equalToJson(nrsSubmissionJsonString, true, false))
            .inScenario("Retry")
            .whenScenarioStateIs("SUCCESS")
            .willReturn(aResponse()
              .withBody(successResponseJson.toString)
              .withStatus(ACCEPTED)))

        await(connector.submit(nrsSubmission)) shouldBe Right(NrsResponse("submissionId"))

        val result: Option[WorkItem[NrsSubmissionWorkItem]] = await(findByPayload(nrsSubmission))

        result shouldBe None
      }

      "give up after all retries" in new Test(fibonacciDelays(100.millis, 5)) {
        wireMockServer.stubFor(
          post(urlPathEqualTo(url))
            .withHeader("Content-Type", equalTo(MimeTypes.JSON))
            .withHeader("X-API-Key", equalTo(apiKeyValue))
            .withRequestBody(equalToJson(nrsSubmissionJsonString, true, false))
            .inScenario("Retry")
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)))

        (1 to 4).foreach { i =>
          wireMockServer.stubFor(
            post(urlPathEqualTo(url))
              .withHeader("Content-Type", equalTo(MimeTypes.JSON))
              .withHeader("X-API-Key", equalTo(apiKeyValue))
              .withRequestBody(equalToJson(nrsSubmissionJsonString, true, false))
              .inScenario("Retry")
              .whenScenarioStateIs(s"Retry-$i")
              .willReturn(aResponse()
                .withStatus(INTERNAL_SERVER_ERROR))
              .willSetStateTo(s"Retry-${i + 1}")
          )
        }

        wireMockServer.stubFor(
          post(urlPathEqualTo(url))
            .withHeader("Content-Type", equalTo(MimeTypes.JSON))
            .withHeader("X-API-Key", equalTo(apiKeyValue))
            .withRequestBody(equalToJson(nrsSubmissionJsonString, true, false))
            .inScenario("Retry")
            .whenScenarioStateIs("Retry-5")
            .willReturn(aResponse()
              .withStatus(INTERNAL_SERVER_ERROR))
        )

        await(connector.submit(nrsSubmission)) shouldBe Left(NrsFailure.ErrorResponse(INTERNAL_SERVER_ERROR))

        val result: Option[WorkItem[NrsSubmissionWorkItem]] = await(findByPayload(nrsSubmission))

        result.map(_.item.nrsSubmission) shouldBe Some(nrsSubmission)
      }
    }

    "fails with 4xx status" must {
      "give up" in new Test(longDelays) {
        wireMockServer.stubFor(
          post(urlPathEqualTo(url))
            .withHeader("Content-Type", equalTo(MimeTypes.JSON))
            .withHeader("X-API-Key", equalTo(apiKeyValue))
            .withRequestBody(equalToJson(nrsSubmissionJsonString, true, false))
            .willReturn(aResponse()
              .withStatus(BAD_REQUEST)))

        await(connector.submit(nrsSubmission)) shouldBe Left(NrsFailure.ErrorResponse(BAD_REQUEST))

        val result: Option[WorkItem[NrsSubmissionWorkItem]] = await(findByPayload(nrsSubmission))

        result.map(_.item.nrsSubmission) shouldBe None
      }
    }

    "fails with exception" must {
      "give up" in new Test(longDelays) {
        wireMockServer.stubFor(
          post(urlPathEqualTo(url))
            .withHeader("Content-Type", equalTo(MimeTypes.JSON))
            .withHeader("X-API-Key", equalTo(apiKeyValue))
            .withRequestBody(equalToJson(nrsSubmissionJsonString, true, false))
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)))

        await(connector.submit(nrsSubmission)) shouldBe Left(NrsFailure.ExceptionThrown)

        val result: Option[WorkItem[NrsSubmissionWorkItem]] = await(findByPayload(nrsSubmission))

        result.map(_.item.nrsSubmission) shouldBe None
      }
    }

    "fails because unparseable JSON returned" must {
      "give up" in new Test(longDelays) {
        wireMockServer.stubFor(
          post(urlPathEqualTo(url))
            .withHeader("Content-Type", equalTo(MimeTypes.JSON))
            .withHeader("X-API-Key", equalTo(apiKeyValue))
            .withRequestBody(equalToJson(nrsSubmissionJsonString, true, false))
            .willReturn(aResponse()
              .withBody(
                """{
                  |   "badKey": "badValue"
                  |}""".stripMargin)
              .withStatus(BAD_REQUEST)))

        await(connector.submit(nrsSubmission)) shouldBe Left(NrsFailure.ErrorResponse(BAD_REQUEST))

        val result: Option[WorkItem[NrsSubmissionWorkItem]] = await(findByPayload(nrsSubmission))

        result.map(_.item.nrsSubmission) shouldBe None
      }
    }

    "submission fails and is retryable" must {
      "insert the failed submission into MongoDB on first failure" in new Test {
        val expectedLogMessage: String =
          s"$correlationId::[NrsConnector:submit] Storing new failed NRS submission in MongoDB"

        wireMockServer.stubFor(
          post(urlPathEqualTo(url))
            .withHeader("Content-Type", equalTo(MimeTypes.JSON))
            .withHeader("X-API-Key", equalTo(apiKeyValue))
            .withRequestBody(equalToJson(nrsSubmissionJsonString, true, false))
            .willReturn(aResponse().withStatus(REQUEST_TIMEOUT))
        )

        withCaptureOfLoggingFrom(connector.logger) { events =>
          await(connector.submit(nrsSubmission)) shouldBe Left(NrsFailure.ErrorResponse(REQUEST_TIMEOUT))

          events.map(_.getMessage) should contain(expectedLogMessage)
        }

        val result: Option[WorkItem[NrsSubmissionWorkItem]] = await(findByPayload(nrsSubmission))

        result.map(_.item.nrsSubmission) shouldBe Some(nrsSubmission)
      }

      "skip inserting the failed submission into MongoDB if it already exists" in new Test {
        val expectedLogMessage: String =
          s"$correlationId::[NrsConnector:submit] NRS submission already exists in MongoDB, skipping insert"

        wireMockServer.stubFor(
          post(urlPathEqualTo(url))
            .withHeader("Content-Type", equalTo(MimeTypes.JSON))
            .withHeader("X-API-Key", equalTo(apiKeyValue))
            .withRequestBody(equalToJson(nrsSubmissionJsonString, true, false))
            .willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE))
        )

        await(repository.pushNew(NrsSubmissionWorkItem(nrsSubmission)))

        withCaptureOfLoggingFrom(connector.logger) { events =>
          await(connector.submit(nrsSubmission)) shouldBe Left(NrsFailure.ErrorResponse(SERVICE_UNAVAILABLE))

          events.map(_.getMessage) should contain(expectedLogMessage)
        }

        val result: Option[WorkItem[NrsSubmissionWorkItem]] = await(findByPayload(nrsSubmission))

        result.map(_.item.nrsSubmission) shouldBe Some(nrsSubmission)
      }
    }
  }

}
