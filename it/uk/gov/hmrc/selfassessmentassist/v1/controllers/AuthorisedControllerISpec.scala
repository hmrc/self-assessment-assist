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

package uk.gov.hmrc.selfassessmentassist.v1.controllers

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{FORBIDDEN, NO_CONTENT}
import play.api.libs.json.{Json, JsValue}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.{await, defaultAwaitTimeout, ACCEPT, AUTHORIZATION}
import uk.gov.hmrc.auth.core.InternalError
import uk.gov.hmrc.selfassessmentassist.stubs._
import uk.gov.hmrc.selfassessmentassist.support.IntegrationBaseSpec
import uk.gov.hmrc.selfassessmentassist.support.TestData.CommonTestData
import uk.gov.hmrc.selfassessmentassist.v1.models.errors.{ClientOrAgentNotAuthorisedError, DownstreamError, InvalidBearerTokenError, InvalidCredentialsError, MtdError}

import scala.collection.Seq

class AuthorisedControllerISpec extends IntegrationBaseSpec {

  "Calling the authorised controller " should {
    "return a 204 status code" when {
      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          MtdIdLookupStub.ninoFound(nino)
          AuthStub.authorised()
          AcknowledgeStub.acknowledge(acknowledgeUrl)
          IfsServiceStub.submit(ifsSubmitUrl)
          NrsStub.submit(nrsSubmitUrl)
        }

        val response: WSResponse = await(request().post(emptyJson))
        response.status shouldBe NO_CONTENT
      }
    }

    "return the correct error status code" when {
      "any an invalid token message is returned" when {
        def validationErrorTest(returnedErrorMessage: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"validation fails with ${expectedBody.code} error and message $returnedErrorMessage" in new Test {

            override def setupStubs(): StubMapping = {
              MtdIdLookupStub.ninoFound(nino)
              AuthStub.unauthorisedOther(returnedErrorMessage)
            }

            val response: WSResponse = await(request().post(emptyJson))
            response.status shouldBe expectedStatus
            response.body shouldBe Json.arr(expectedBody)
          }
        }


        val input = Seq(
          ("InvalidBearerToken", FORBIDDEN, InvalidBearerTokenError),
          ("InsufficientConfidenceLevel", FORBIDDEN, ClientOrAgentNotAuthorisedError),
          ("UnsupportedAffinityGroup", FORBIDDEN, ClientOrAgentNotAuthorisedError),
          ("UnsupportedCredentialRole", FORBIDDEN, ClientOrAgentNotAuthorisedError),
          ("UnsupportedAuthProvider", FORBIDDEN, ClientOrAgentNotAuthorisedError),
          ("BearerTokenExpired", FORBIDDEN, InvalidCredentialsError),
          ("MissingBearerToken", FORBIDDEN, InvalidCredentialsError),
          ("SessionRecordNotFound", FORBIDDEN, InvalidCredentialsError),
          ("IncorrectCredentialStrength", FORBIDDEN, ClientOrAgentNotAuthorisedError),
          ("InsufficientEnrolments", FORBIDDEN, ClientOrAgentNotAuthorisedError),
          ("FailedRelationship", FORBIDDEN, ClientOrAgentNotAuthorisedError),
          ("other_error", FORBIDDEN, DownstreamError)
        )
      }
    }
  }

  private trait Test {

    def nino: String = "AA123456A"

    def reportId: String = CommonTestData.simpleCalculationId.toString

    val acknowledgeUrl: String   = "/rds/assessments/self-assessment-assist/acknowledge"
    val ifsSubmitUrl: String     = "/interaction-data/store-interactions"
    val nrsSubmitUrl: String     = "/submission"
    val rdsCorrelationId: String = CommonTestData.correlationId
    val emptyJson: JsValue       = Json.parse("""{}""")

    def setupStubs(): StubMapping

    val uri = s"/reports/acknowledge/$nino/$reportId/$rdsCorrelationId"

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.1.0+json"),
          (AUTHORIZATION, "Bearer 123")
        )
    }

  }

}
