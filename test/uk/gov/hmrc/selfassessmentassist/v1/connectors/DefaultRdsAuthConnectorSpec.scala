/*
 * Copyright 2026 HM Revenue & Customs
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

import com.google.common.base.Charsets
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.libs.ws.WSBodyWritables.writeableOf_urlEncodedForm
import uk.gov.hmrc.http.{HttpException, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.selfassessmentassist.api.models.auth.{AuthCredential, RdsAuthCredentials}
import uk.gov.hmrc.selfassessmentassist.api.models.errors.RdsAuthDownstreamError
import uk.gov.hmrc.selfassessmentassist.support.{ConnectorSpec, MockAppConfig}
import uk.gov.hmrc.selfassessmentassist.v1.mocks.MockHttpClient

import java.net.URL
import java.util.Base64
import scala.concurrent.Future

class DefaultRdsAuthConnectorSpec extends ConnectorSpec with MockAppConfig with MockHttpClient {

  private val rdsSasBaseUrlForAuth: String = s"$baseUrl/SASLogon/oauth/token"
  private val rdsSasUrlForAuth: URL        = url"$rdsSasBaseUrlForAuth"

  private val rdsAuthCredentials: AuthCredential = AuthCredential("ac8cd8d0-f212-4706-85d1-82b7785ad0b1", "bearer", "grant_type")

  private val authToken: String = Base64.getEncoder.encodeToString(
    s"${rdsAuthCredentials.client_id}:${rdsAuthCredentials.client_secret}".getBytes(Charsets.UTF_8)
  )

  private val expectedHeaders: Seq[(String, String)] = Seq(
    "Content-Type"  -> "application/x-www-form-urlencoded",
    "Accept"        -> "application/json",
    "Authorization" -> s"Basic $authToken"
  )

  private val requestBody: Map[String, Seq[String]] = Map("grant_type" -> Seq(rdsAuthCredentials.grant_type))
  private val successResponse: RdsAuthCredentials   = RdsAuthCredentials("access_token", "bearer", 3600)
  private val successResponseJson: String           = Json.toJson(successResponse).toString

  private trait Test {
    MockedAppConfig.rdsSasBaseUrlForAuth returns rdsSasBaseUrlForAuth
    MockedAppConfig.rdsAuthCredential returns rdsAuthCredentials

    val connector: DefaultRdsAuthConnector = new DefaultRdsAuthConnector(mockHttpClient)(mockAppConfig, ec)

    def mockAuthCall(response: Future[HttpResponse]): Unit =
      MockedHttpClient
        .post(rdsSasUrlForAuth, requestBody, expectedHeaders, useProxy = true)
        .returns(response)

  }

  "DefaultRdsAuthConnector" when {
    ".retrieveAuthorisedBearer" should {
      "return credentials" when {
        "auth call returns 200 OK" in new Test {
          mockAuthCall(Future.successful(HttpResponse(OK, successResponseJson)))

          await(connector.retrieveAuthorisedBearer().value) shouldBe Right(successResponse)
        }

        "auth call returns 202 ACCEPTED" in new Test {
          mockAuthCall(Future.successful(HttpResponse(ACCEPTED, successResponseJson)))

          await(connector.retrieveAuthorisedBearer().value) shouldBe Right(successResponse)
        }
      }

      "return RdsAuthDownstreamError" when {
        "auth call returns an unexpected status" in new Test {
          mockAuthCall(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))

          await(connector.retrieveAuthorisedBearer().value) shouldBe Left(RdsAuthDownstreamError)
        }

        "auth call returns invalid JSON" in new Test {
          mockAuthCall(Future.successful(HttpResponse(OK, """{ "not": "a-token" }""")))

          await(connector.retrieveAuthorisedBearer().value) shouldBe Left(RdsAuthDownstreamError)
        }

        "auth call returns HttpException" in new Test {
          mockAuthCall(Future.failed(new HttpException("test", INTERNAL_SERVER_ERROR)))

          await(connector.retrieveAuthorisedBearer().value) shouldBe Left(RdsAuthDownstreamError)
        }

        "auth call returns UpstreamErrorResponse" in new Test {
          mockAuthCall(Future.failed(UpstreamErrorResponse("test", SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))

          await(connector.retrieveAuthorisedBearer().value) shouldBe Left(RdsAuthDownstreamError)
        }
      }
    }
  }

}
