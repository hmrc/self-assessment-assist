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

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.MimeTypes
import play.api.libs.json.{JsValue, Json}
import play.api.test.Injecting
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.selfassessmentassist.api.TestData.CommonTestData.simpleFraudRiskRequest
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{ErrorWrapper, InternalError}
import uk.gov.hmrc.selfassessmentassist.api.models.outcomes.ResponseWrapper
import uk.gov.hmrc.selfassessmentassist.support.{ConnectorSpec, MockAppConfig}
import uk.gov.hmrc.selfassessmentassist.v1.models.request.cip.FraudRiskReport

import scala.collection.Seq
import java.util.Base64

class InsightConnectorSpec extends ConnectorSpec with BeforeAndAfterAll with GuiceOneAppPerSuite with Injecting with MockAppConfig {

  def port: Int = wireMockServer.port()
  val url       = "/fraud"

  private val successResponseJson: JsValue =
    Json.parse("""{"riskCorrelationId":"8d844f4a-0630-4568-99ef-d4606ae45d17",
        |"riskScore":50,
        |"reasons":["No NINO has path to something risky."]}""".stripMargin)

  private val malformedSuccessResponseJson: JsValue =
    Json.parse("""{"invalid":"8d844f4a-0630-4568-99ef-d4606ae45d17",
        |"invalid2":50,
        |"invalid3":["No NINO has path to something risky."]}""".stripMargin)

  private val fraudRiskRequestJsonString: String = Json.toJson(simpleFraudRiskRequest).toString()
  private val fraudRiskResponse                  = successResponseJson.validate[FraudRiskReport].get
  val httpClient: HttpClientV2                   = app.injector.instanceOf[HttpClientV2]

  class Test {
    val username: String = "some-user-name"
    val token: String    = "some-token"
    MockedAppConfig.cipFraudServiceBaseUrl returns s"http://localhost:$port/fraud"
    MockedAppConfig.cipFraudUsername returns username
    MockedAppConfig.cipFraudToken returns token
    val connector = new InsightConnector(httpClient, mockAppConfig)

    def stubCIPResponse(body: Option[String] = None, status: Int): StubMapping = {
      body match {
        case Some(data) =>
          wireMockServer.stubFor(
            post(urlPathEqualTo(url))
              .withHeader("Content-Type", equalTo(MimeTypes.JSON))
              .withRequestBody(equalToJson(fraudRiskRequestJsonString, true, false))
              .willReturn(aResponse()
                .withBody(data)
                .withStatus(status)))

        case None =>
          wireMockServer.stubFor(
            post(urlPathEqualTo(url))
              .withHeader("Content-Type", equalTo(MimeTypes.JSON))
              .withRequestBody(equalToJson(fraudRiskRequestJsonString, true, false))
              .willReturn(aResponse()
                .withStatus(status)))
      }
    }

  }

  override def beforeAll(): Unit = wireMockServer.start()

  override def afterAll(): Unit = wireMockServer.stop()

  "Give InsightConnector" when {

    "fraudTestHeaders " must {
      "return correct headers " in new Test {
        val credentials = Base64.getEncoder.encodeToString(s"$username:$token".getBytes)
        connector.fraudRiskHeaders() should be(Seq("Authorization" -> s"Basic $credentials"))
      }
    }

    "is immediately successful then" must {
      "return the response" in new Test {
        stubCIPResponse(Some(successResponseJson.toString), OK)
        await(connector.assess(simpleFraudRiskRequest)) shouldBe Right(ResponseWrapper(correlationId, fraudRiskResponse))
      }

      "return invalid response" in new Test {
        stubCIPResponse(Some(malformedSuccessResponseJson.toString), OK)
        await(connector.assess(simpleFraudRiskRequest)) shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }
    }

    "fails with 400 status" must {
      "fail the request" in new Test {
        stubCIPResponse(None, BAD_REQUEST)
        await(connector.assess(simpleFraudRiskRequest)) shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }
    }

    "fails with 404(not found) status" must {
      "fail the request" in new Test {
        stubCIPResponse(None, NOT_FOUND)
        await(connector.assess(simpleFraudRiskRequest)) shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }
    }

    "fails with 408(REQUEST_TIMEOUT) status" must {
      "fail the request" in new Test {
        stubCIPResponse(None, REQUEST_TIMEOUT)
        await(connector.assess(simpleFraudRiskRequest)) shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }
    }

    "fails with 500(INTERNAL SERVER ERROR) status" must {
      "fail the request" in new Test {
        stubCIPResponse(None, INTERNAL_SERVER_ERROR)
        await(connector.assess(simpleFraudRiskRequest)) shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }
    }

  }

}
