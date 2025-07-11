/*
 * Copyright 2024 HM Revenue & Customs
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

package auth

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status.OK
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import uk.gov.hmrc.selfassessmentassist.stubs._
import uk.gov.hmrc.selfassessmentassist.support.IntegrationBaseSpec

abstract class AuthSupportingAgentsAllowedISpec extends IntegrationBaseSpec {

  /** The API's latest version, e.g. "1.0".
    */
  protected val callingApiVersion: String

  /** As the IT supplies the "supported" config below, this can be any endpoint IF there's no actual "supporting agents allowed" endpoint in the API.
    */
  protected val supportingAgentsAllowedEndpoint: String

  protected def sendMtdRequest(request: WSRequest): WSResponse

  protected val mtdUrl: String

  protected val expectedMtdSuccessStatus: Int = OK

  /** One endpoint where supporting agents are allowed.
    */
  override def servicesConfig: Map[String, Any] =
    Map(
      s"api.supporting-agent-endpoints.$supportingAgentsAllowedEndpoint" -> "true"
    ) ++ super.servicesConfig

  protected val nino = "AA123456A"

  val rdsUrl: String       = "/rds/assessments/self-assessment-assist"
  val ifsSubmitUrl: String = "/interaction-data/store-interactions"
  val nrsSubmitUrl: String = "/submission"
  val fraudUrl: String     = "/fraud"

  "Calling an endpoint that allows supporting agents" when {
    "the client is the primary agent" should {
      "return a success response" in new Test {
        def setupStubs(): StubMapping = {
          MtdIdLookupStub.ninoFound(nino)
          AuthStub.authorised()
          FraudStub.submit(fraudUrl)
          RdsStub.submit(rdsUrl)
          IfsServiceStub.submit(ifsSubmitUrl)
          NrsStub.submit(nrsSubmitUrl)
          AuthStub.authorisedWithAgentAffinityGroup()
          AuthStub.authorisedWithPrimaryAgentEnrolment()
        }

        val response: WSResponse = sendMtdRequest(request)
        response.status shouldBe expectedMtdSuccessStatus
      }
    }

    "the client is a supporting agent" should {
      "return a success response" in new Test {
        def setupStubs(): StubMapping = {

          MtdIdLookupStub.ninoFound(nino)
          AuthStub.authorised()
          FraudStub.submit(fraudUrl)
          RdsStub.submit(rdsUrl)
          IfsServiceStub.submit(ifsSubmitUrl)
          NrsStub.submit(nrsSubmitUrl)
          MtdIdLookupStub.ninoFound(nino)

          AuthStub.authorisedWithAgentAffinityGroup()
          AuthStub.unauthorisedForPrimaryAgentEnrolment()
          AuthStub.authorisedWithSupportingAgentEnrolment()

        }

        val response: WSResponse = sendMtdRequest(request)
        response.status shouldBe expectedMtdSuccessStatus
      }
    }
  }

  protected trait Test {

    def setupStubs(): StubMapping

    protected def request: WSRequest = {
      AuthStub.resetAll()
      setupStubs()
      buildRequest(mtdUrl)
        .withHttpHeaders(
          (ACCEPT, s"application/vnd.hmrc.$callingApiVersion+json"),
          (AUTHORIZATION, "Bearer 123")
        )
    }

  }

}
