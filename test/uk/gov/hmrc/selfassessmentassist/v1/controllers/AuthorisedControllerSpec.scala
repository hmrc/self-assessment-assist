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

import akka.util.ByteString
import play.api.http.HttpEntity
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfassessmentassist.v1.TestData.CommonTestData._
import uk.gov.hmrc.selfassessmentassist.v1.connectors.MtdIdLookupConnector
import uk.gov.hmrc.selfassessmentassist.v1.mocks.connectors.MockLookupConnector
import uk.gov.hmrc.selfassessmentassist.v1.mocks.services.MockEnrolmentsAuthService
import uk.gov.hmrc.selfassessmentassist.v1.models.errors._
import uk.gov.hmrc.selfassessmentassist.v1.services.EnrolmentsAuthService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}


class AuthorisedControllerSpec extends ControllerBaseSpec {
  trait Test extends MockEnrolmentsAuthService with MockLookupConnector {

    val hc: HeaderCarrier = HeaderCarrier()
    val authorisedController: TestController = new TestController()

    class TestController extends AuthorisedController(cc) {
      override val authService: EnrolmentsAuthService = mockEnrolmentsAuthService
      override val lookupConnector: MtdIdLookupConnector = mockLookupConnector

      def authorisedActionAysncSUT(nino: String): Action[AnyContent] = {
        authorisedAction(nino)(correlationId).async {
          Future.successful(Ok(Json.obj()))
        }
      }

    }
  }

  val ninoIsCorrect: String = "AA000000B"
  val ninoIsIncorrect: String = "AA000000Z"

  "calling an action" when {

    // Happy Path.

    "a user is properly authorised and has a correct nino" should {
      "return a 200 success response" in new Test {
        MockEnrolmentsAuthService.authoriseUser()
        MockLookupConnector.mockMtdIdLookupConnector("1234567890")
        private val resultFuture = authorisedController.authorisedActionAysncSUT(ninoIsCorrect)(fakePostRequest)
        val result: Result = Await.result(resultFuture, defaultTimeout)
        result.header.status shouldBe OK
      }
    }

    // Errors,

    "a user is properly authorised and has an incorrect nino" should {
      "return a 400 error response" in new Test {
        MockEnrolmentsAuthService.authoriseUser()
        MockLookupConnector.mockMtdIdLookupConnector("1234567890")
        private val resultFuture: Future[Result] = authorisedController.authorisedActionAysncSUT(ninoIsIncorrect)(fakePostRequest)
        val result: Result = Await.result(resultFuture, defaultTimeout)

        (result.header.status) shouldBe BAD_REQUEST

        val body: HttpEntity.Strict = result.body.asInstanceOf[HttpEntity.Strict]
        val returnedErrorJSon: ByteString = Await.result(body.data, defaultTimeout)
        val returnedError: String = returnedErrorJSon.utf8String

        val ninoErrorJSon: JsValue = Json.toJson(NinoFormatError)

        returnedError shouldBe Json.toJson(Seq(ninoErrorJSon)).toString()
      }
    }

    "the mtd id lookup service returns an unauthorised error" should {
      "return a 401 error response" in new Test {
        MockLookupConnector.mockMtdIdLookupConnectorError(InvalidBearerTokenError)
        private val resultFuture: Future[Result] = authorisedController.authorisedActionAysncSUT(ninoIsCorrect)(fakePostRequest)
        val result: Result = Await.result(resultFuture, defaultTimeout)

        (result.header.status) shouldBe UNAUTHORIZED

        val body: HttpEntity.Strict = result.body.asInstanceOf[HttpEntity.Strict]
        val returnedErrorJSon: ByteString = Await.result(body.data, defaultTimeout)
        val returnedError: String = returnedErrorJSon.utf8String

        val invalidBearerJson: JsValue = Json.toJson(Seq(InvalidBearerTokenError))
        val ninoError: String = invalidBearerJson.toString()

        returnedError shouldBe ninoError
      }
    }

    "the mtd id lookup service returns an forbidden" should {
      "return a 405 error response" in new Test {
        MockLookupConnector.mockMtdIdLookupConnectorError(UnauthorisedError)
        private val resultFuture: Future[Result] = authorisedController.authorisedActionAysncSUT(ninoIsCorrect)(fakePostRequest)
        val result: Result = Await.result(resultFuture, defaultTimeout)

        (result.header.status) shouldBe FORBIDDEN

        val body: HttpEntity.Strict = result.body.asInstanceOf[HttpEntity.Strict]
        val returnedErrorJSon: ByteString = Await.result(body.data, defaultTimeout)
        val returnedError: String = returnedErrorJSon.utf8String

        val invalidBearerJson: JsValue = Json.toJson(Seq(UnauthorisedError))
        val ninoError: String = invalidBearerJson.toString()

        returnedError shouldBe ninoError
      }
    }

    "the mtd id lookup service returns an unknown error" should {
      "return a 500 error response" in new Test {
        MockLookupConnector.mockMtdIdLookupConnectorError(ForbiddenRDSCorrelationIdError)
        private val resultFuture: Future[Result] = authorisedController.authorisedActionAysncSUT(ninoIsCorrect)(fakePostRequest)
        val result: Result = Await.result(resultFuture, defaultTimeout)

        (result.header.status) shouldBe INTERNAL_SERVER_ERROR

        val body: HttpEntity.Strict = result.body.asInstanceOf[HttpEntity.Strict]
        val returnedErrorJSon: ByteString = Await.result(body.data, defaultTimeout)
        val returnedError: String = returnedErrorJSon.utf8String

        val invalidBearerJson: JsValue = Json.toJson(Seq(DownstreamError))
        val ninoError: String = invalidBearerJson.toString()

        returnedError shouldBe ninoError
      }
    }

    "the mtd id lookup service returns an NinoFormat error" should {
      "return a 403 error response" in new Test {
        MockLookupConnector.mockMtdIdLookupConnectorError(NinoFormatError)
        private val resultFuture: Future[Result] = authorisedController.authorisedActionAysncSUT(ninoIsCorrect)(fakePostRequest)
        val result: Result = Await.result(resultFuture, defaultTimeout)

        (result.header.status) shouldBe FORBIDDEN

        val body: HttpEntity.Strict = result.body.asInstanceOf[HttpEntity.Strict]
        val returnedErrorJSon: ByteString = Await.result(body.data, defaultTimeout)
        val returnedError: String = returnedErrorJSon.utf8String

        val unauthorisedErrorJson: JsValue = Json.toJson(Seq(UnauthorisedError))
        val unauthorisedError: String = unauthorisedErrorJson.toString()

        returnedError shouldBe unauthorisedError
      }
    }
  }

  "the enrolments auth service returns an error" must {
    "map to the correct result" when {

      val predicate: String => Predicate = { mtdItId: String =>
        Enrolment("HMRC-MTD-IT")
          .withIdentifier("MTDITID", mtdItId)
          .withDelegatedAuthRule("mtd-it-auth")
      }

      def serviceErrors(mtdError: MtdError, expectedStatus: Int, expectedBody: JsValue): Unit = {
        s"a ${mtdError.code}/${mtdError.message} error is returned from the enrolments auth service" in new Test {

          MockLookupConnector.mockMtdIdLookupConnector("1234567890")
          MockEnrolmentsAuthService.authorised(predicate("1234567890"))
            .returns(Future.successful(Left(mtdError)))

          private val actualResult = authorisedController.authorisedActionAysncSUT(ninoIsCorrect)(fakePostRequest)
          status(actualResult) shouldBe expectedStatus
          contentAsJson(actualResult) shouldBe Json.toJson(Seq(expectedBody))
        }
      }

      object unexpectedError extends MtdError(code = "UNEXPECTED_ERROR", message = "This is an unexpected error")

      val authServiceErrors =
        Seq(
          (ClientOrAgentNotAuthorisedError, FORBIDDEN, Json.toJson(ClientOrAgentNotAuthorisedError)),
          (ForbiddenDownstreamError, FORBIDDEN, Json.toJson(DownstreamError)),
          (InvalidBearerTokenError, FORBIDDEN, Json.toJson(InvalidCredentialsError)),
          (BearerTokenExpiredError, FORBIDDEN, Json.toJson(InvalidCredentialsError)),
          (LegacyUnauthorisedError, FORBIDDEN, Json.toJson(LegacyUnauthorisedError)),
          (RdsAuthError, INTERNAL_SERVER_ERROR, Json.toJson(DownstreamError)),
          (unexpectedError, INTERNAL_SERVER_ERROR, Json.toJson(DownstreamError))
        )

      authServiceErrors.foreach(args => (serviceErrors _).tupled(args))
    }
  }

}

