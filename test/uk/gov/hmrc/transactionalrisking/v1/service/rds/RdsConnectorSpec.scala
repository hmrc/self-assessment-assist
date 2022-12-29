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

import akka.actor
import akka.actor.ActorSystem
import akka.stream.Materializer
import com.codahale.metrics.SharedMetricRegistries
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.{BeforeAndAfterAll, EitherValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.MimeTypes
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Injecting
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.transactionalrisking.support.{ConnectorSpec, MockAppConfig}
import uk.gov.hmrc.transactionalrisking.v1.TestData.CommonTestData._
import uk.gov.hmrc.transactionalrisking.v1.models.auth.RdsAuthCredentials
import uk.gov.hmrc.transactionalrisking.v1.models.errors.{DownstreamError, ErrorWrapper, ForbiddenDownstreamError, MatchingResourcesNotFoundError, MtdError, ServiceUnavailableError}
import uk.gov.hmrc.transactionalrisking.v1.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.v1.service.rds.RdsTestData.rdsRequest
import uk.gov.hmrc.transactionalrisking.v1.services.rds.RdsConnector
import uk.gov.hmrc.transactionalrisking.v1.utils.StubResource.loadSubmitResponseTemplate
import uk.gov.hmrc.transactionalrisking.v1.services.ServiceOutcome
import uk.gov.hmrc.transactionalrisking.v1.services.rds.models.response.RdsAssessmentReport

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration._


class RdsConnectorSpec extends ConnectorSpec
  with BeforeAndAfterAll
  with GuiceOneAppPerSuite
  with Injecting
  with MockAppConfig with EitherValues{
  var port: Int = _

  private val actorSystem: ActorSystem    = actor.ActorSystem("unit-testing")
  implicit val materializer: Materializer = Materializer.matFromSystem(actorSystem)
  val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "metrics.enabled" -> false,
        "auditing.enabled" -> false)
      .build()

  override def beforeAll(): Unit = {
    wireMockServer.start()
    port = wireMockServer.port()
    println(s"started at $port")
    SharedMetricRegistries.clear()
  }

  override def afterAll(): Unit = {
    wireMockServer.stop()
    materializer.shutdown()
    Await.result(actorSystem.terminate(), 3.minutes)
  }
  class Test {
    val submitBaseUrl:String = s"http://localhost:$port/submit"
    val acknowledgeUrl:String = s"http://localhost:$port/acknowledge"
    val rdsAuthCredentials = RdsAuthCredentials(UUID.randomUUID().toString, "bearer", 3600)

    MockedAppConfig.rdsBaseUrlForSubmit returns submitBaseUrl
    MockedAppConfig.rdsBaseUrlForAcknowledge returns acknowledgeUrl
    val connector = new RdsConnector(httpClient, mockAppConfig)

    def stubRDSResponse(body:Option[String]=None,status:Int) = {
      body match {
        case Some(data) =>
          wireMockServer.stubFor(
          post(urlPathEqualTo("/submit"))
            .withHeader("Content-Type", equalTo(MimeTypes.JSON))
            .withHeader("Authorization" , equalTo(s"Bearer ${rdsAuthCredentials.access_token}"))
            .willReturn(aResponse()
              .withBody(data)
              .withStatus(status)))
        case None =>
          wireMockServer.stubFor(
            post(urlPathEqualTo("/submit"))
              .withHeader("Content-Type", equalTo(MimeTypes.JSON))
              .withHeader("Authorization" , equalTo(s"Bearer ${rdsAuthCredentials.access_token}"))
              .willReturn(aResponse()
                .withStatus(status)))
      }

    }
  }

  "RDSConnector" when {
    "submit method is called" must {
      "return the response if successful" in new Test {
        stubRDSResponse(Some(rdsSubmissionReportJson.toString),CREATED)

        await(connector.submit(rdsRequest,Some(rdsAuthCredentials))) shouldBe Right(ResponseWrapper(correlationId, rdsNewSubmissionReport))
      }

      "fail when the bearer token is invalid" in new Test {
        stubRDSResponse(status=UNAUTHORIZED)

        await(connector.submit(rdsRequest,Some(rdsAuthCredentials))) shouldBe Left(ErrorWrapper(correlationId, ForbiddenDownstreamError))
      }

      "return the feedback, if RDS returns http status 201 and and feedback with responsecode 201" in new Test{
        stubRDSResponse(Some(rdsSubmissionReportJson.toString),status=CREATED)
        await(connector.submit(rdsRequest,Some(rdsAuthCredentials))) shouldBe Right(ResponseWrapper(correlationId, rdsNewSubmissionReport))

      }

      "return the empty feedback, if RDS returns http status 201 and no feedback with responsecode 204" in new Test{
        val expectedReportJson = loadSubmitResponseTemplate(expectedCalculationIdWithNoFeedback.toString, simpleReportId.toString, simpleRDSCorrelationId,"204")
        val rdsReportJson = loadSubmitResponseTemplate(calculationIdWithNoFeedback.toString, simpleReportId.toString, simpleRDSCorrelationId,"204")
        stubRDSResponse(Some(rdsReportJson.toString),status=CREATED)

        val feedbackReport: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest,Some(rdsAuthCredentials)))
        feedbackReport shouldBe Right(ResponseWrapper(correlationId, expectedReportJson.as[RdsAssessmentReport]))
      }


      "return MatchingResourcesNotFoundError, if RDS returns http status 201 and no calculationId found with responsecode 404" in new Test{
        val rdsReportJson = loadSubmitResponseTemplate(noCalculationFound.toString, simpleReportId.toString, simpleRDSCorrelationId,"404")
        stubRDSResponse(Some(rdsReportJson.toString),status=CREATED)

        val feedbackReport: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest,Some(rdsAuthCredentials)))
        feedbackReport shouldBe Left(ErrorWrapper(correlationId, MatchingResourcesNotFoundError,Some(Seq(MtdError("404","No feedback applicable")))))
      }

      "return Internal Server Error, if RDS returns http status 400" in new Test{
        stubRDSResponse(status=BAD_REQUEST)

        val feedbackReport: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest,Some(rdsAuthCredentials)))
        feedbackReport shouldBe Left(ErrorWrapper(correlationId, DownstreamError))
      }

      "return Service Unavailable, if RDS is (unavailable) http status code 404" in new Test{
        stubRDSResponse(status=NOT_FOUND)

        val feedbackReport: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest,Some(rdsAuthCredentials)))
        feedbackReport shouldBe Left(ErrorWrapper(correlationId, ServiceUnavailableError))
      }

      "return Internal Server Error, if RDS fails with 503" in new Test{
        stubRDSResponse(status=SERVICE_UNAVAILABLE)

        val feedbackReport: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest,Some(rdsAuthCredentials)))
        feedbackReport shouldBe Left(ErrorWrapper(correlationId, DownstreamError))
      }

      "return Service Unavailable, if RDS request Timesout" in new Test{
        stubRDSResponse(status=REQUEST_TIMEOUT)

        val feedbackReport: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest,Some(rdsAuthCredentials)))
        feedbackReport shouldBe Left(ErrorWrapper(correlationId, ServiceUnavailableError))
      }
    }
  }
//TODO move this to RDSAuthConnectorSpec
  def stubRdsAuth(response: RdsAuthCredentials, statusCode: Int = 202): StubMapping =
    stubFor(
      post(
        urlPathEqualTo("/prweb/PRRestService/oauth2/v1/token")
      ).willReturn(aResponse().withStatus(statusCode).withBody(Json.toJson(response).toString()))
    )
}
