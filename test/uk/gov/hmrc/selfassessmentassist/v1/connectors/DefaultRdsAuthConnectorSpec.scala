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
import uk.gov.hmrc.selfassessmentassist.api.models.auth.{AuthCredential, RdsAuthCredentials}
import uk.gov.hmrc.selfassessmentassist.api.models.errors.MtdError
import uk.gov.hmrc.selfassessmentassist.support.{ConnectorSpec, MockAppConfig}

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration._

class DefaultRdsAuthConnectorSpec
    extends ConnectorSpec
    with BeforeAndAfterAll
    with GuiceOneAppPerSuite
    with Injecting
    with MockAppConfig
    with EitherValues {
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
    val submitBaseUrl: String  = s"http://localhost:$port/submit"
    val acknowledgeUrl: String = s"http://localhost:$port/rds/assessments/self-assessment-assist/acknowledge"
    val authToken              = "YWM4Y2Q4ZDAtZjIxMi00NzA2LTg1ZDEtODJiNzc4NWFkMGIxOmJlYXJlcg=="

    println(UUID.randomUUID().toString)

    val rdsAuthCredentials: AuthCredential = AuthCredential("ac8cd8d0-f212-4706-85d1-82b7785ad0b1", "bearer", "grant_type")

    MockedAppConfig.rdsSasBaseUrlForAuth returns submitBaseUrl
    MockedAppConfig.rdsAuthCredential returns rdsAuthCredentials
    val connector = new DefaultRdsAuthConnector(httpClient)(mockAppConfig, ec)

    def stubRdsAuthResponse(status: Int): StubMapping = {
      wireMockServer.stubFor(
        post(urlPathEqualTo("/submit"))
          .withHeader("Content-Type", equalTo(MimeTypes.FORM))
          .withHeader("Authorization", equalTo(s"Basic $authToken"))
          .willReturn(
            aResponse()
              .withStatus(status)
              .withBody(
                Json.toJson(RdsAuthCredentials("access_token", "bearer", 20)).toString()
              ))
      )
    }

  }

  "DefaultRdsAuthConnector" when {
    "retrieveAuthorisedBearer method is called" must {
      "OK" in new Test {
        stubRdsAuthResponse(200)
        await(connector.retrieveAuthorisedBearer().value) shouldBe Right(RdsAuthCredentials("access_token", "bearer", 20))
      }

      "ACCEPTED" in new Test {
        stubRdsAuthResponse(202)
        await(connector.retrieveAuthorisedBearer().value) shouldBe Right(RdsAuthCredentials("access_token", "bearer", 20))
      }

      "return rds error when no auth" in new Test {
        stubRdsAuthResponse(403)
        await(connector.retrieveAuthorisedBearer().value) shouldBe Left(
          MtdError("RDS_AUTH_ERROR", "RDS authorisation could not be accomplished", None))
      }

    }
  }

}
