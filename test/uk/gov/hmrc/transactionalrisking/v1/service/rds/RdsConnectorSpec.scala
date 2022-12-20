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
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.MimeTypes
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Injecting
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.transactionalrisking.support.{ConnectorSpec, MockAppConfig}
import uk.gov.hmrc.transactionalrisking.v1.TestData.CommonTestData.commonTestData.{noCalculationFound, rdsNewSubmissionReport, rdsSubmissionReportJson, simpleRDSCorrelationId, simpleTaxYearEndInt}
import uk.gov.hmrc.transactionalrisking.v1.models.auth.RdsAuthCredentials
import uk.gov.hmrc.transactionalrisking.v1.models.errors.{DownstreamError, ErrorWrapper, ForbiddenDownstreamError, FormatReportIdError, MatchingResourcesNotFoundError, MtdError, ServiceUnavailableError}
import uk.gov.hmrc.transactionalrisking.v1.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.v1.service.rds.RdsTestData.rdsRequest
import uk.gov.hmrc.transactionalrisking.v1.services.rds.RdsConnector
import uk.gov.hmrc.transactionalrisking.v1.utils.StubResource.loadSubmitResponseTemplate
import uk.gov.hmrc.transactionalrisking.v1.TestData.CommonTestData.commonTestData.{calculationIdWithNoFeedback, simpleReportId}
import uk.gov.hmrc.transactionalrisking.v1.models.domain.PreferredLanguage.PreferredLanguage
import uk.gov.hmrc.transactionalrisking.v1.models.domain.{AssessmentReport, DesTaxYear, Link, PreferredLanguage, Risk}
import uk.gov.hmrc.transactionalrisking.v1.services.ServiceOutcome
import uk.gov.hmrc.transactionalrisking.v1.services.rds.models.response.RdsAssessmentReport

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration._


class RdsConnectorSpec extends ConnectorSpec
  with BeforeAndAfterAll
  with GuiceOneAppPerSuite
  with Injecting
  with MockAppConfig{
  //with MockWSHelpers {
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
   // shutdownHelpers()
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
        await(connector.submit(rdsRequest,Some(rdsAuthCredentials))) shouldBe Right(ResponseWrapper(simpleRDSCorrelationId, rdsNewSubmissionReport))
      }

      "fail when the bearer token is invalid" in new Test {
        stubRDSResponse(status=UNAUTHORIZED)
        await(connector.submit(rdsRequest,Some(rdsAuthCredentials))) shouldBe Left(ErrorWrapper(simpleRDSCorrelationId, ForbiddenDownstreamError))
      }

      "return the feedback, if RDS returns http status 201 and and feedback with responsecode 201" in new Test{
        stubRDSResponse(Some(rdsSubmissionReportJson.toString),status=CREATED)
        await(connector.submit(rdsRequest,Some(rdsAuthCredentials))) shouldBe Right(ResponseWrapper(simpleRDSCorrelationId, rdsNewSubmissionReport))

      }

      "return the empty feedback, if RDS returns http status 201 and no feedback with responsecode 204" in new Test{
        val rdsReportJson = loadSubmitResponseTemplate(calculationIdWithNoFeedback.toString, simpleReportId.toString, simpleRDSCorrelationId,"204")
        stubRDSResponse(Some(rdsReportJson.toString),status=CREATED)
        val feedbackReport: ServiceOutcome[RdsAssessmentReport] = await(connector.submit(rdsRequest,Some(rdsAuthCredentials)))

        feedbackReport shouldBe Right(ResponseWrapper(simpleRDSCorrelationId, rdsReportJson.as[RdsAssessmentReport]))
        val assessmentReport = toAssessmentReport(feedbackReport.right.get.responseData,calculationIdWithNoFeedback.toString,simpleRDSCorrelationId,
  "AA065213C",simpleTaxYearEndInt)

        assessmentReport.right.get.responseData.risks shouldBe Seq.empty
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

    //    "acknowledge method is called" must {
    //      "return the response if successful" in new Test {
    //
    //        val ws = MockWS {
    //          case (POST, acknowledgeUrlTmp) if (acknowledgeUrlTmp == acknowledgeUrl) =>
    //            Action {
    //              Created(rdsAssessmentReport)
    //            }
    //          case (_, _) =>
    //            throw new RuntimeException("Unable to distinguish API call or path whilst testing")
    //        }
    //        val connector = new RdsConnector(ws, mockAppConfig)
    //
    //        MockedAppConfig.rdsBaseUrlForAcknowledge returns acknowledgeUrl
    //
    //        val ret:ServiceOutcome[ NewRdsAssessmentReport ] = await(connector.acknowledgeRds(acknowledgeReportRequest))
    //        val Right( ResponseWrapper( correlationIdRet, newRdsAssessmentReport)) = ret
    //
    //        val rdsCorrelationId:String = newRdsAssessmentReport.rdsCorrelationId.get
    //        val year:Int = newRdsAssessmentReport.taxYear.get
    //        val responceCode:Int = newRdsAssessmentReport.responseCode.get
    //
    //        correlationIdRet shouldBe commonTestData.internalCorrelationId
    //
    //        rdsCorrelationId shouldBe simpleRDSCorrelationID
    //        year shouldBe simpleTaxYearEndInt
    //        responceCode shouldBe NO_CONTENT
    //
    //
    //
    //      }
    //    }
  }
//TODO move this to RDSAuthConnectorSpec
  def stubRdsAuth(response: RdsAuthCredentials, statusCode: Int = 202): StubMapping =
    stubFor(
      post(
        urlPathEqualTo("/prweb/PRRestService/oauth2/v1/token")
      ).willReturn(aResponse().withStatus(statusCode).withBody(Json.toJson(response).toString()))
    )

  private def toAssessmentReport(report: RdsAssessmentReport, calculationId: String, correlationId: String,nino:String,taxYear:Int): ServiceOutcome[AssessmentReport] = {

    def isPreferredLanguage(language: String, preferredLanguage: PreferredLanguage) = preferredLanguage match {
      case PreferredLanguage.English if language == "EnglishActions" => true
      case PreferredLanguage.Welsh if language == "WelshActions" => true
      case _ => false
    }

    def toRisk(riskParts: Seq[String]):Option[Risk] = {
      if(riskParts.isEmpty) None
      else
        Some(Risk(title = riskParts(2),
          body = riskParts(0), action = riskParts(1),
          links = Seq(Link(riskParts(3), riskParts(4))), path = riskParts(5)))
    }

    def risks(report: RdsAssessmentReport, preferredLanguage: PreferredLanguage, correlationId: String): Seq[Risk] = {
      report.outputs.collect {
        case elm: RdsAssessmentReport.MainOutputWrapper if isPreferredLanguage(elm.name, preferredLanguage) => elm
      }.flatMap(_.value).collect {
        case value: RdsAssessmentReport.DataWrapper => value
      }.flatMap(_.data)
        .map(toRisk).flatten
    }

    (report.calculationId, report.feedbackId) match {
      case (Some(calculationId), Some(reportId)) =>
        if(calculationId.equals(calculationId)) {
          val rdsCorrelationIdOption = report.rdsCorrelationId
          rdsCorrelationIdOption match {
            case Some(rdsCorrelationID) =>
              Right(ResponseWrapper(correlationId,
                AssessmentReport(reportId = reportId,
                  risks = risks(report, PreferredLanguage.English, correlationId), nino = nino,
                  taxYear = DesTaxYear.fromDesIntToString(taxYear),
                  calculationId = calculationId, rdsCorrelationID)))

            case None =>
              Left(ErrorWrapper(correlationId, DownstreamError)): ServiceOutcome[AssessmentReport]
          }
        }else{
          Left(ErrorWrapper(correlationId, DownstreamError)): ServiceOutcome[AssessmentReport]
        }

      case (Some(_), None) =>
        Left(ErrorWrapper(correlationId, FormatReportIdError)): ServiceOutcome[AssessmentReport]

      case (None, Some(_)) =>
        Left(ErrorWrapper(correlationId, DownstreamError)): ServiceOutcome[AssessmentReport]

      case (None, None) =>
        Left(ErrorWrapper(correlationId, DownstreamError)): ServiceOutcome[AssessmentReport]
    }
  }
}
