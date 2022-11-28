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

package uk.gov.hmrc.transactionalrisking.controllers

import akka.util.ByteString
import play.api.http.HttpEntity
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.{Enrolment, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transactionalrisking.v1.TestData.CommonTestData.commonTestData.internalCorrelationID
import uk.gov.hmrc.transactionalrisking.v1.controllers.AuthorisedController
import uk.gov.hmrc.transactionalrisking.v1.mocks.services.MockEnrolmentsAuthService
import uk.gov.hmrc.transactionalrisking.v1.models.errors._
import uk.gov.hmrc.transactionalrisking.v1.services.EnrolmentsAuthService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}


class AuthorisedControllerSpec extends ControllerBaseSpec {

  trait Test extends MockEnrolmentsAuthService {

    val hc: HeaderCarrier = HeaderCarrier()
    val authorisedController: TestController = new TestController()

    class TestController extends AuthorisedController(cc) {
      override val authService: EnrolmentsAuthService = mockEnrolmentsAuthService

      def authorisedActionAysncSUT(nino: String, correlationID:String, nrsRequired: Boolean = true): Action[AnyContent] = authorisedAction(nino, correlationID, nrsRequired).async {
        Future.successful(Ok(Json.obj()))
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
        private val resultFuture = authorisedController.authorisedActionAysncSUT(ninoIsCorrect, internalCorrelationID )(fakeGetRequest)
        val result = Await.result(resultFuture, defaultTimeout)
        result.header.status shouldBe OK
      }
    }

    // Errors,

    "a user is properly authorised and has an incorrect nino" should {
      "return a 400 success response" in new Test {
        MockEnrolmentsAuthService.authoriseUser()
        private val resultFuture: Future[Result] = authorisedController.authorisedActionAysncSUT(ninoIsIncorrect, internalCorrelationID, false)(fakeGetRequest)
        val result = Await.result(resultFuture, defaultTimeout)

        (result.header.status) shouldBe BAD_REQUEST

        val body: HttpEntity.Strict = result.body.asInstanceOf[HttpEntity.Strict]
        val returnedErrorJSon: ByteString = Await.result(body.data, defaultTimeout)
        val returnedError: String = returnedErrorJSon.utf8String

        val ninoErrorJSon: JsValue = Json.toJson(NinoFormatError)
        val ninoError = ninoErrorJSon.toString()

        returnedError shouldBe ninoError
      }
    }
  }

  //TODO uncomment me and fix me, issue might be because itmp_birthdate was removed from enrollmenta
  // revisit if still broken.
  "the enrolments auth service returns an error" must {
    "map to the correct result" when {

      val predicate: Predicate =
        Nino(hasNino = true, nino = Some(ninoIsCorrect)) or Enrolment("IR-SA").withIdentifier(AuthorisedController.ninoKey, ninoIsCorrect).withDelegatedAuthRule("sa-auth")

      def serviceErrors(mtdError: MtdError, expectedStatus: Int, expectedBody: JsValue): Unit = {
        s"a ${mtdError.code} error is returned from the enrolments auth service" in new Test {

          MockEnrolmentsAuthService.authorised(predicate)
            .returns(Future.successful(Left(mtdError)))

          private val actualResult = authorisedController.authorisedActionAysncSUT(ninoIsCorrect, internalCorrelationID)(fakeGetRequest)
          status(actualResult) shouldBe expectedStatus
          contentAsJson(actualResult) shouldBe expectedBody
        }
      }

      object unexpectedError extends MtdError(code = "UNEXPECTED_ERROR", message = "This is an unexpected error")

      val authServiceErrors =
        Seq(
          (ClientOrAgentNotAuthorisedError, FORBIDDEN, Json.toJson(ClientOrAgentNotAuthorisedError)),
          (ForbiddenDownstreamError, FORBIDDEN, Json.toJson(DownstreamError)),
          (unexpectedError, INTERNAL_SERVER_ERROR, Json.toJson(DownstreamError))
        )

      authServiceErrors.foreach(args => (serviceErrors _).tupled(args))
    }
  }

}

