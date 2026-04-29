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
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.{BeforeAndAfterAll, EitherValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Injecting
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.selfassessmentassist.api.models.auth.{AuthCredential, RdsAuthCredentials}
import uk.gov.hmrc.selfassessmentassist.api.models.errors.RdsAuthDownstreamError
import uk.gov.hmrc.selfassessmentassist.support.{ConnectorSpec, MockAppConfig}

class DefaultRdsAuthConnectorSpec
    extends ConnectorSpec
    with BeforeAndAfterAll
    with GuiceOneAppPerSuite
    with Injecting
    with MockAppConfig
    with EitherValues {
  val httpClient: HttpClientV2 = app.injector.instanceOf[HttpClientV2]

  def port: Int = wireMockServer.port()

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure("metrics.enabled" -> false, "auditing.enabled" -> false)
      .build()

  override def beforeAll(): Unit = {
    wireMockServer.start()
    SharedMetricRegistries.clear()
  }

  override def afterAll(): Unit = {
    wireMockServer.stop()
  }

  class Test {
    val submitBaseUrl: String  = s"http://localhost:$port/submit"
    val acknowledgeUrl: String = s"http://localhost:$port/rds/assessments/self-assessment-assist/acknowledge"
    val authToken              = "YWM4Y2Q4ZDAtZjIxMi00NzA2LTg1ZDEtODJiNzc4NWFkMGIxOmJlYXJlcg=="

    val rdsAuthCredentials: AuthCredential = AuthCredential("ac8cd8d0-f212-4706-85d1-82b7785ad0b1", "bearer", "grant_type")

    val tempSasHost = "test-sas-host"

    MockedAppConfig.rdsSasBaseUrlForAuth returns submitBaseUrl
    MockedAppConfig.rdsAuthCredential returns rdsAuthCredentials
    MockedAppConfig.rdsSasV2HostTemp returns tempSasHost

    val connector = new DefaultRdsAuthConnector(httpClient)(mockAppConfig, ec)

    def stubRdsAuthResponse(status: Int, body: String): StubMapping =
      wireMockServer.stubFor(
        post(urlPathEqualTo("/submit"))
          .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
          .withHeader("Authorization", equalTo(s"Basic $authToken"))
          .withRequestBody(equalTo(s"grant_type=${rdsAuthCredentials.grant_type}"))
          .willReturn(
            aResponse()
              .withStatus(status)
              .withBody(body)
          )
      )

  }

  "DefaultRdsAuthConnector.retrieveAuthorisedBearer" should {

    "return credentials on 200 OK" in new Test {
      stubRdsAuthResponse(
        OK,
        Json.toJson(RdsAuthCredentials("access_token", "bearer", 3600)).toString()
      )

      await(connector.retrieveAuthorisedBearer().value) shouldBe
        Right(RdsAuthCredentials("access_token", "bearer", 3600))
    }

    "return credentials on 202 ACCEPTED" in new Test {
      stubRdsAuthResponse(
        ACCEPTED,
        Json.toJson(RdsAuthCredentials("access_token", "bearer", 3600)).toString()
      )

      await(connector.retrieveAuthorisedBearer().value).isRight shouldBe true
    }

    "return RdsAuthDownstreamError on unexpected status" in new Test {
      stubRdsAuthResponse(INTERNAL_SERVER_ERROR, "")

      await(connector.retrieveAuthorisedBearer().value) shouldBe
        Left(RdsAuthDownstreamError)
    }

    "return RdsAuthDownstreamError when auth JSON is invalid" in new Test {
      stubRdsAuthResponse(OK, """{ "not": "a-token" }""")

      await(connector.retrieveAuthorisedBearer().value) shouldBe
        Left(RdsAuthDownstreamError)
    }

    "return RdsAuthDownstreamError when upstream error response is returned" in new Test {
      stubRdsAuthResponse(SERVICE_UNAVAILABLE, "service unavailable")

      await(connector.retrieveAuthorisedBearer().value) shouldBe
        Left(RdsAuthDownstreamError)
    }

  }

}
