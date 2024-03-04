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

import com.codahale.metrics.SharedMetricRegistries
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.apache.pekko.actor
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.scalatest.{BeforeAndAfterAll, EitherValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.MimeTypes
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.test.Injecting
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.selfassessmentassist.api.TestData.CommonTestData._
import uk.gov.hmrc.selfassessmentassist.api.models.auth.RdsAuthCredentials
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{ErrorWrapper, ForbiddenDownstreamError, ForbiddenRDSCorrelationIdError, InternalError, MatchingCalculationIDNotFoundError, MtdError, NoAssessmentFeedbackFromRDS}
import uk.gov.hmrc.selfassessmentassist.api.models.outcomes.ResponseWrapper
import uk.gov.hmrc.selfassessmentassist.support.{ConnectorSpec, MockAppConfig}
import uk.gov.hmrc.selfassessmentassist.v1.models.response.rds.RdsAssessmentReport
import uk.gov.hmrc.selfassessmentassist.v1.services.ServiceOutcome
import uk.gov.hmrc.selfassessmentassist.v1.services.testData.RdsTestData.{rdsAcknowledgementRequest, rdsRequest}
import uk.gov.hmrc.selfassessmentassist.v1.utils.StubResource.{loadAckResponseTemplate, loadSubmitResponseTemplate}

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration._

class RdsConnectorSpec extends ConnectorSpec with BeforeAndAfterAll with GuiceOneAppPerSuite with Injecting with MockAppConfig with EitherValues {
  var port: Int = _

  private val actorSystem: ActorSystem    = actor.ActorSystem("unit-testing")
  implicit val materializer: Materializer = Materializer.matFromSystem(actorSystem)
  val httpClient: HttpClient              = app.injector.instanceOf[HttpClient]

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure("metrics.enabled" -> false, "auditing.enabled" -> false)
      .build()

  override def beforeAll(): Unit = {
    wireMockServer.start()
    port = wireMockServer.port()
    SharedMetricRegistries.clear()
  }

  override def afterAll(): Unit = {
    wireMockServer.stop()
    materializer.shutdown()
    Await.result(actorSystem.terminate(), 3.minutes)
  }

  class Test {
    val submitBaseUrl: String                  = s"http://localhost:$port/submit"
    val acknowledgeUrl: String                 = s"http://localhost:$port/rds/assessments/self-assessment-assist/acknowledge"
    val rdsAuthCredentials: RdsAuthCredentials = RdsAuthCredentials(UUID.randomUUID().toString, "bearer", 3600)

    MockedAppConfig.rdsBaseUrlForSubmit returns submitBaseUrl
    MockedAppConfig.rdsBaseUrlForAcknowledge returns acknowledgeUrl
    val connector = new RdsConnector(httpClient, mockAppConfig)

    def stubRDSGenerateReportResponse(body: Option[String] = None, status: Int): StubMapping = {
      body match {
        case Some(data) =>
          wireMockServer.stubFor(
            post(urlPathEqualTo("/submit"))
              .withHeader("Content-Type", equalTo(MimeTypes.JSON))
              .withHeader("Authorization", equalTo(s"Bearer ${rdsAuthCredentials.access_token}"))
              .willReturn(aResponse()
                .withBody(data)
                .withStatus(status)))
        case None =>
          wireMockServer.stubFor(
            post(urlPathEqualTo("/submit"))
              .withHeader("Content-Type", equalTo(MimeTypes.JSON))
              .withHeader("Authorization", equalTo(s"Bearer ${rdsAuthCredentials.access_token}"))
              .willReturn(aResponse()
                .withStatus(status)))
      }
    }

    def stubRDSAcknowledgeReportResponse(body: Option[String] = None, status: Int): StubMapping = {
      body match {
        case Some(data) =>
          wireMockServer.stubFor(
            post(urlPathEqualTo("/rds/assessments/self-assessment-assist/acknowledge"))
              .withHeader("Content-Type", equalTo(MimeTypes.JSON))
              .withHeader("Authorization", equalTo(s"Bearer ${rdsAuthCredentials.access_token}"))
              .willReturn(aResponse()
                .withBody(data)
                .withStatus(status)))
        case None =>
          wireMockServer.stubFor(
            post(urlPathEqualTo("/rds/assessments/self-assessment-assist/acknowledge"))
              .withHeader("Content-Type", equalTo(MimeTypes.JSON))
              .withHeader("Authorization", equalTo(s"Bearer ${rdsAuthCredentials.access_token}"))
              .willReturn(aResponse()
                .withStatus(status)))
      }

    }

  }

  "RDSConnector" when {
    "submit method is called" must {
      "return the response if successful" in new Test {
        stubRDSGenerateReportResponse(Some(rdsSubmissionReportJson.toString), CREATED)

        await(connector.submit(rdsRequest, Some(rdsAuthCredentials))) shouldBe Right(ResponseWrapper(correlationId, rdsNewSubmissionReport))
      }

      "fail when the bearer token is invalid" in new Test {
        stubRDSGenerateReportResponse(status = UNAUTHORIZED)

        await(connector.submit(rdsRequest, Some(rdsAuthCredentials))) shouldBe Left(ErrorWrapper(correlationId, ForbiddenDownstreamError))
      }

      "return the feedback, if RDS returns http status 201 and feedback with responsecode 201" in new Test {
        stubRDSGenerateReportResponse(Some(rdsSubmissionReportJson.toString), status = CREATED)
        await(connector.submit(rdsRequest, Some(rdsAuthCredentials))) shouldBe Right(ResponseWrapper(correlationId, rdsNewSubmissionReport))

      }

      "return the empty feedback, if RDS returns http status 201 and no feedback with responsecode 204" in new Test {
        val rdsReportJson: JsValue = loadSubmitResponseTemplate(calculationIdWithNoFeedback.toString, simpleReportId.toString, simpleRDSCorrelationId)
        stubRDSGenerateReportResponse(Some(rdsReportJson.toString), status = CREATED)

        val feedbackReport: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest, Some(rdsAuthCredentials)))
        feedbackReport shouldBe Left(ErrorWrapper(correlationId, NoAssessmentFeedbackFromRDS))
      }

      "return MatchingResourcesNotFoundError, if RDS returns http status 201 and no calculationId found with responsecode 404" in new Test {
        val rdsReportJson: JsValue = loadSubmitResponseTemplate(noCalculationFound.toString, simpleReportId.toString, simpleRDSCorrelationId)
        stubRDSGenerateReportResponse(Some(rdsReportJson.toString), status = CREATED)

        val feedbackReport: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest, Some(rdsAuthCredentials)))
        feedbackReport shouldBe Left(
          ErrorWrapper(correlationId, MatchingCalculationIDNotFoundError, Some(Seq(MtdError("404", "No feedback applicable", NOT_FOUND)))))
      }

      "return Internal Server Error, if RDS returns http status 400" in new Test {
        stubRDSGenerateReportResponse(status = BAD_REQUEST)

        val feedbackReport: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest, Some(rdsAuthCredentials)))
        feedbackReport shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }

      "return Internal Server Error, if RDS is (unavailable) http status code 404" in new Test {
        stubRDSGenerateReportResponse(status = NOT_FOUND)

        val feedbackReport: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest, Some(rdsAuthCredentials)))
        feedbackReport shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }

      "return Internal Server Error, if RDS fails with 503" in new Test {
        stubRDSGenerateReportResponse(status = SERVICE_UNAVAILABLE)

        val feedbackReport: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest, Some(rdsAuthCredentials)))
        feedbackReport shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }

      "return Internal Server Error, if RDS request Timesout" in new Test {
        stubRDSGenerateReportResponse(status = REQUEST_TIMEOUT)

        val feedbackReport: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest, Some(rdsAuthCredentials)))
        feedbackReport shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }
    }

    "acknowledge method is called" must {
      "return the response if successful" in new Test {
        val rdsAssessmentAckJson: JsValue = loadAckResponseTemplate(simpleReportId.toString, nino = simpleNino, responseCode = 202)
        stubRDSAcknowledgeReportResponse(Some(rdsAssessmentAckJson.toString), status = CREATED)

        await(connector.acknowledgeRds(rdsAcknowledgementRequest, Some(rdsAuthCredentials))) shouldBe Right(
          ResponseWrapper(correlationId, rdsAssessmentAckJson.as[RdsAssessmentReport]))
      }

      "fail when the bearer token is invalid" in new Test {
        stubRDSAcknowledgeReportResponse(status = UNAUTHORIZED)
        await(connector.acknowledgeRds(rdsAcknowledgementRequest, Some(rdsAuthCredentials))) shouldBe Left(
          ErrorWrapper(correlationId, ForbiddenDownstreamError))
      }

      "return no content, if RDS returns http status 201 and with responsecode 202" in new Test {
        stubRDSAcknowledgeReportResponse(Some(rdsAssessmentAckJson.toString), status = CREATED)
        await(connector.acknowledgeRds(rdsAcknowledgementRequest, Some(rdsAuthCredentials))) shouldBe Right(
          ResponseWrapper(correlationId, simpleAcknowledgeNewRdsAssessmentReport))
      }

      "return ForbiddenRDSCorrelationIdError, if RDS returns http status 201 with responsecode 401 for reportid  and correlationId combination " in new Test {
        val rdsAssessmentAckJson: JsValue = loadAckResponseTemplate(simpleReportId.toString, nino = simpleNino, responseCode = 401)
        stubRDSAcknowledgeReportResponse(Some(rdsAssessmentAckJson.toString), status = CREATED)

        val feedbackReport: ServiceOutcome[RdsAssessmentReport] = await(connector.acknowledgeRds(rdsAcknowledgementRequest, Some(rdsAuthCredentials)))
        feedbackReport shouldBe Left(ErrorWrapper(correlationId, ForbiddenRDSCorrelationIdError, None))
      }

      "return Internal Server Error, if RDS returns http status 400" in new Test {
        stubRDSAcknowledgeReportResponse(status = BAD_REQUEST)

        val feedbackReport: ServiceOutcome[RdsAssessmentReport] = await(connector.acknowledgeRds(rdsAcknowledgementRequest, Some(rdsAuthCredentials)))
        feedbackReport shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }

      "return Internal Server Error, if RDS is (unavailable) http status code 404" in new Test {
        stubRDSAcknowledgeReportResponse(status = NOT_FOUND)

        val feedbackReport: ServiceOutcome[RdsAssessmentReport] = await(connector.acknowledgeRds(rdsAcknowledgementRequest, Some(rdsAuthCredentials)))
        feedbackReport shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }

      "return Internal Server Error, if RDS fails with 503" in new Test {
        stubRDSAcknowledgeReportResponse(status = SERVICE_UNAVAILABLE)

        val feedbackReport: ServiceOutcome[RdsAssessmentReport] = await(connector.acknowledgeRds(rdsAcknowledgementRequest, Some(rdsAuthCredentials)))
        feedbackReport shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }

      "return Service Unavailable, if RDS request Timesout" in new Test {
        stubRDSAcknowledgeReportResponse(status = REQUEST_TIMEOUT)

        val feedbackReport: ServiceOutcome[RdsAssessmentReport] = await(connector.acknowledgeRds(rdsAcknowledgementRequest, Some(rdsAuthCredentials)))
        feedbackReport shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }
    }
  }

}
