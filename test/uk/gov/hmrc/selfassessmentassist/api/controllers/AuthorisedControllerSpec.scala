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

package uk.gov.hmrc.selfassessmentassist.api.controllers

import org.apache.pekko.util.ByteString
import play.api.http.HttpEntity
import play.api.libs.json.{JsValue, Json}
import play.api.Configuration
import play.api.mvc.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfassessmentassist.api.models.errors.BadRequestError
import uk.gov.hmrc.selfassessmentassist.api.models.auth.UserDetails
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{
  ClientOrAgentNotAuthorisedError,
  InternalError,
  InvalidBearerTokenError,
  InvalidCredentialsError,
  NinoFormatError,
  UnauthorisedError
}
import uk.gov.hmrc.selfassessmentassist.api.connectors.MtdIdLookupConnector
import uk.gov.hmrc.selfassessmentassist.api.TestData.CommonTestData
import uk.gov.hmrc.selfassessmentassist.mocks.services.MockEnrolmentsAuthService
import uk.gov.hmrc.selfassessmentassist.support.MockAppConfig
import uk.gov.hmrc.selfassessmentassist.v1.mocks.connectors.MockLookupConnector
import uk.gov.hmrc.selfassessmentassist.v1.services.EnrolmentsAuthService

import uk.gov.hmrc.auth.core.AffinityGroup
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

class AuthorisedControllerSpec extends ControllerBaseSpec with MockAppConfig {

  trait TestWithoutMocks extends MockLookupConnector with MockEnrolmentsAuthService {

    val hc: HeaderCarrier                                 = HeaderCarrier()
    protected def supportingAgentsfeatureEnabled: Boolean = true
    val authorisedController: TestController              = new TestController()

    class TestController extends AuthorisedController(cc) {
      override val authService: EnrolmentsAuthService    = mockEnrolmentsAuthService
      override val lookupConnector: MtdIdLookupConnector = mockLookupConnector
      override val endpointName: String                  = "test-endpoint"

      def authorisedActionAysncSUT(nino: String): Action[AnyContent] = {
        authorisedAction(nino)(CommonTestData.correlationId).async {
          Future.successful(Ok(Json.obj()))
        }
      }

    }

    protected def supportingAgentsAccessControlEnabled: Boolean = true

    protected def endpointAllowsSupportingAgents: Boolean = true

  }

  trait Test extends TestWithoutMocks {

    MockedAppConfig
      .endpointAllowsSupportingAgents(authorisedController.endpointName)
      .anyNumberOfTimes() returns endpointAllowsSupportingAgents

    MockedAppConfig.featureSwitch.returns(
      Some(
        Configuration(
          "supporting-agents-access-control.enabled" -> supportingAgentsfeatureEnabled
        )))

  }

  val ninoIsCorrect: String   = "AA000000B"
  val ninoIsIncorrect: String = "AA000000Z"

  "Calling an action" when {

    "the user is authorised" should {
      "return a 200" in new Test {

        override protected def supportingAgentsfeatureEnabled: Boolean = true

        MockLookupConnector.mockMtdIdLookupConnector("1234567890")
        MockEnrolmentsAuthService.authoriseUser()

        private val resultFuture = authorisedController.authorisedActionAysncSUT(ninoIsCorrect)(fakePostRequest)
        val result: Result       = Await.result(resultFuture, defaultTimeout)
        result.header.status shouldBe OK
      }
    }

    "the Primary Agent is authorised and supporting agents aren't allowed for this endpoint" should {
      "return a 200" in new Test {

        override def endpointAllowsSupportingAgents: Boolean = false

        MockLookupConnector.mockMtdIdLookupConnector("1234567890")

        MockEnrolmentsAuthService
          .authoriseAgent("1234567890")
          .returns(
            Future.successful(
              Right(
                UserDetails(
                  userType = AffinityGroup.Agent,
                  agentReferenceNumber = Some("arn"),
                  clientID = "",
                  Some(CommonTestData.identityCorrectModel)
                )
              )))

        private val resultFuture = authorisedController.authorisedActionAysncSUT(ninoIsCorrect)(fakePostRequest)
        val result: Result       = Await.result(resultFuture, defaultTimeout)
        result.header.status shouldBe OK
      }

    }

    "the supporting agent is authorised" should {
      "return a 200" in new Test {

        override protected def endpointAllowsSupportingAgents: Boolean = true

        MockLookupConnector.mockMtdIdLookupConnector("1234567890")

        MockEnrolmentsAuthService
          .authoriseAgent("1234567890", supportingAgentAccessAllowed = true)
          .returns(
            Future.successful(
              Right(
                UserDetails(
                  userType = AffinityGroup.Agent,
                  agentReferenceNumber = Some("arn"),
                  clientID = "",
                  Some(CommonTestData.identityCorrectModel)
                )
              )))

        private val resultFuture = authorisedController.authorisedActionAysncSUT(ninoIsCorrect)(fakePostRequest)
        val result: Result       = Await.result(resultFuture, defaultTimeout)
        result.header.status shouldBe OK
      }
    }

    "the supporting agent is not authorised" should {
      "return a 403" in new Test {

        override protected def endpointAllowsSupportingAgents: Boolean = true
        MockLookupConnector.mockMtdIdLookupConnector("1234567890")

        MockEnrolmentsAuthService
          .authoriseAgent("1234567890", supportingAgentAccessAllowed = true)
          .returns(Future.successful(Left(ClientOrAgentNotAuthorisedError)))

        private val resultFuture = authorisedController.authorisedActionAysncSUT(ninoIsCorrect)(fakePostRequest)
        val result: Result       = Await.result(resultFuture, defaultTimeout)
        result.header.status shouldBe FORBIDDEN
      }
    }

    "the EnrolmentsAuthService returns an error" should {
      "return that error with its status code" in new Test {

        override protected def endpointAllowsSupportingAgents: Boolean = true
        MockLookupConnector.mockMtdIdLookupConnector("1234567890")

        MockEnrolmentsAuthService
          .authoriseAgent("1234567890", supportingAgentAccessAllowed = true)
          .returns(Future.successful(Left(BadRequestError)))

        private val resultFuture = authorisedController.authorisedActionAysncSUT(ninoIsCorrect)(fakePostRequest)
        val result: Result       = Await.result(resultFuture, defaultTimeout)
        status(result) shouldBe BadRequestError.httpStatus
        contentAsJson(result) shouldBe BadRequestError.asJson
      }
    }

    "the MtdIdLookupService returns an error" should {
      "return that error with its status code" in new TestWithoutMocks {

        MockLookupConnector.mockMtdIdLookupConnectorError(UnauthorisedError)

        private val resultFuture = authorisedController.authorisedActionAysncSUT(ninoIsCorrect)(fakePostRequest)
        val result: Result       = Await.result(resultFuture, defaultTimeout)
        status(result) shouldBe ClientOrAgentNotAuthorisedError.httpStatus
        contentAsJson(result) shouldBe ClientOrAgentNotAuthorisedError.asJson
      }
    }

    "the nino is invalid" should {
      "return a 400" in new TestWithoutMocks {

        MockEnrolmentsAuthService.authoriseUser()
        MockLookupConnector.mockMtdIdLookupConnector("1234567890")
        private val resultFuture: Future[Result] = authorisedController.authorisedActionAysncSUT(ninoIsIncorrect)(fakePostRequest)
        val result: Result                       = Await.result(resultFuture, defaultTimeout)

        result.header.status shouldBe BAD_REQUEST

        val body: HttpEntity.Strict       = result.body.asInstanceOf[HttpEntity.Strict]
        val returnedErrorJSon: ByteString = Await.result(body.data, defaultTimeout)
        val returnedError: String         = returnedErrorJSon.utf8String

        val ninoErrorJSon: JsValue = NinoFormatError.asJson

        returnedError shouldBe Json.toJson(Seq(ninoErrorJSon)).toString()
      }
    }

    "the nino is valid but invalid bearer token" should {
      "return a 401" in new TestWithoutMocks {

        MockLookupConnector.mockMtdIdLookupConnectorError(InvalidBearerTokenError)
        private val resultFuture: Future[Result] = authorisedController.authorisedActionAysncSUT(ninoIsCorrect)(fakePostRequest)
        val result: Result                       = Await.result(resultFuture, defaultTimeout)

        result.header.status shouldBe UNAUTHORIZED

        val body: HttpEntity.Strict       = result.body.asInstanceOf[HttpEntity.Strict]
        val returnedErrorJSon: ByteString = Await.result(body.data, defaultTimeout)
        val returnedError: String         = returnedErrorJSon.utf8String

        val invalidCredentialsJson: JsValue = Json.toJson(InvalidCredentialsError)
        val ninoError: String               = invalidCredentialsJson.toString()

        returnedError shouldBe ninoError
      }
    }

    "authorisation checks fail when retrieving the MTD ID" should {
      "return a 403" in new TestWithoutMocks {

        MockLookupConnector.mockMtdIdLookupConnectorError(ClientOrAgentNotAuthorisedError)

        private val resultFuture = authorisedController.authorisedActionAysncSUT(ninoIsCorrect)(fakePostRequest)
        val result: Result       = Await.result(resultFuture, defaultTimeout)
        status(result) shouldBe FORBIDDEN
        contentAsJson(result) shouldBe ClientOrAgentNotAuthorisedError.asJson
      }
    }

    "an error occurs retrieving the MTD ID" should {
      "return a 500" in new TestWithoutMocks {
        MockLookupConnector.mockMtdIdLookupConnectorError(InternalError)

        private val resultFuture = authorisedController.authorisedActionAysncSUT(ninoIsCorrect)(fakePostRequest)
        val result: Result       = Await.result(resultFuture, defaultTimeout)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

}
