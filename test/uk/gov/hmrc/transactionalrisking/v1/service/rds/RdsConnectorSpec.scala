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
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, post, stubFor, urlPathEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import mockws.MockWSHelpers
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.MimeTypes
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Injecting
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.transactionalrisking.models.auth.RdsAuthCredentials
import uk.gov.hmrc.transactionalrisking.models.errors.{ErrorWrapper, ForbiddenDownstreamError}
import uk.gov.hmrc.transactionalrisking.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.services.rds.RdsConnector
import uk.gov.hmrc.transactionalrisking.support.{ConnectorSpec, MockAppConfig}
import uk.gov.hmrc.transactionalrisking.v1.TestData.CommonTestData.commonTestData.{rdsNewSubmissionReport, rdsSubmissionReportJson, simpleRDSCorrelationID}
import uk.gov.hmrc.transactionalrisking.v1.service.rds.RdsTestData.rdsRequest

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
  }
//TODO move this to RDSAuthConnectorStub
//  class RDSAuthConnectorStub(
//                               result: Either[MtdError, RdsAuthCredentials] = Right(
//                                 RdsAuthCredentials(UUID.randomUUID().toString, "bearer", 3600)
//                               )
//                             ) extends RdsAuthConnector[Future] {
//    override def retrieveAuthorisedBearer()(implicit
//                                            hc: HeaderCarrier
//    ): EitherT[Future, MtdError, RdsAuthCredentials] =
//      EitherT[Future, MtdError, RdsAuthCredentials](Future(result))
//  }


  "RDSConnector" when {
    "submit method is called" must {
      "return the response if successful" in new Test {
        wireMockServer.stubFor(
          post(urlPathEqualTo("/submit"))
            .withHeader("Content-Type", equalTo(MimeTypes.JSON))
            .withHeader("Authorization" , equalTo(s"Bearer ${rdsAuthCredentials.access_token}"))
            .willReturn(aResponse()
              .withBody(rdsSubmissionReportJson.toString)
              .withStatus(OK)))

       await(connector.submit(rdsRequest,rdsAuthCredentials)) shouldBe Right(ResponseWrapper(simpleRDSCorrelationID, rdsNewSubmissionReport))
      }

      "fail when the bearer token is invalid" in new Test {
        wireMockServer.stubFor(
          post(urlPathEqualTo("/submit"))
            .withHeader("Content-Type", equalTo(MimeTypes.JSON))
            .withHeader("Authorization" , equalTo(s"Bearer ${rdsAuthCredentials.access_token}"))
            .willReturn(aResponse()
              .withStatus(UNAUTHORIZED)))

        await(connector.submit(rdsRequest,rdsAuthCredentials)) shouldBe Left(ErrorWrapper(simpleRDSCorrelationID, ForbiddenDownstreamError))
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
}
