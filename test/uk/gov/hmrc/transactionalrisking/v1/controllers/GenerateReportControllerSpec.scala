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

package uk.gov.hmrc.transactionalrisking.v1.controllers


import play.api.libs.json.JsValue
import play.api.mvc.Result
import uk.gov.hmrc.transactionalrisking.mocks.utils.utils.MockCurrentDateTime
import uk.gov.hmrc.transactionalrisking.v1.TestData.CommonTestData.commonTestData._
import uk.gov.hmrc.transactionalrisking.v1.mocks.services._
import uk.gov.hmrc.transactionalrisking.v1.mocks.utils.MockIdGenerator
import uk.gov.hmrc.transactionalrisking.v1.models.errors._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GenerateReportControllerSpec
  extends ControllerBaseSpec
    with MockIntegrationFrameworkService
    with MockEnrolmentsAuthService
    with MockNrsService
    with MockInsightService
    with MockRdsService
    with MockCurrentDateTime
    with MockIdGenerator {

  implicit val correlationId: String = "X-ID"

  trait Test {

    val controller: TestController = new TestController()

    class TestController extends GenerateReportController(
      cc = cc,
      integrationFrameworkService = mockIntegrationFrameworkService,
      authService = mockEnrolmentsAuthService,
      nonRepudiationService = mockNrsService,
      insightService = mockInsightService,
      rdsService = mockRdsService,
      currentDateTime = mockCurrentDateTime,
      idGenerator = mockIdGenerator
    )

  }

  "generateReport" when {
    "a valid request is supplied" should {
      "return the expected data when controller is called" in new Test {

        MockEnrolmentsAuthService.authoriseUser()
        MockIntegrationFrameworkService.getCalculationInfo(simpleCalculationId, simpleNino)
        MockInsightService.assess(simpleFraudRiskRequest)
        MockRdsService.submit(simpleAssessmentRequestForSelfAssessment, simpleFraudRiskReport, simpleInternalOrigin)
        MockCurrentDateTime.getDateTime()
        MockNrsService.submit(simpleRequestBodyAcknowledge, simpleSubmissionTimestamp, simpleReportNotableEventType, simpleNRSResponseReportSubmission) // TODO change this to a new model
        MockProvideRandomCorrelationId.IdGenerator

        val result: Future[Result] = controller.generateReportInternal(simpleNino, simpleCalculationId.toString)(fakeGetRequest)
        status(result) shouldBe OK
        contentAsJson(result) shouldBe simpleAsssementReportMtdJson
        contentType(result) shouldBe Some("application/json")
        header("X-CorrelationId", result) shouldBe Some(internalCorrelationId)
      }
    }

    "a request fails due to failed authorisedAction that gives a NinoFormatError" should {

      s"return the NinoFormatError error  to indicate that the nino is  invalid. " in new Test {

        MockCurrentDateTime.getDateTime()
        MockProvideRandomCorrelationId.IdGenerator

        val result: Future[Result] = controller.generateReportInternal(simpleNinoInvalid, simpleCalculationId.toString)(fakeGetRequest)

        status(result) shouldBe BAD_REQUEST
        Thread.sleep(1000)

        contentAsJson(result) shouldBe NinoFormatError.toJson
        contentType(result) shouldBe Some("application/json")
        header("X-CorrelationId", result) shouldBe Some(internalCorrelationId)

      }
    }


    "a request fails due to failed EnrolmentsAuthService.authorised failure" should {

      def runTest(mtdError: MtdError, expectedStatus: Int, expectedBody: JsValue): Unit = {
        s"return the expected error ${mtdError.code} to indicate that the data has not been accepted and saved due to EnrolmentsAuthService.authorised returning an error." in new Test {

          MockEnrolmentsAuthService.authoriseUserFail(mtdError)
          MockCurrentDateTime.getDateTime()
          MockProvideRandomCorrelationId.IdGenerator

          val result: Future[Result] = controller.generateReportInternal(simpleNino, simpleCalculationId.toString)(fakeGetRequest)

          status(result) shouldBe expectedStatus
          contentAsJson(result) shouldBe expectedBody
          contentType(result) shouldBe Some("application/json")
          header("X-CorrelationId", result) shouldBe Some(internalCorrelationId)

        }
      }

      val errorInErrorOut =
        Seq(
          (ClientOrAgentNotAuthorisedError, FORBIDDEN, ClientOrAgentNotAuthorisedError.toJson),
          (ForbiddenDownstreamError, FORBIDDEN, DownstreamError.toJson),
          (ServiceUnavailableError, INTERNAL_SERVER_ERROR, DownstreamError.toJson)
        )

      errorInErrorOut.foreach(args => (runTest _).tupled(args))
    }

    "a request fails due to a failed InsightService.assess " should {

      def runTest(mtdError: MtdError, expectedStatus: Int, expectedBody: JsValue): Unit = {
        s"return the expected error ${mtdError.code} when controller is set to return error from InsightService.assess " in new Test {

          MockEnrolmentsAuthService.authoriseUser()
          MockIntegrationFrameworkService.getCalculationInfo(simpleCalculationId, simpleNino)
          MockInsightService.assessFail(simpleFraudRiskRequest, mtdError)
          MockCurrentDateTime.getDateTime()
          MockProvideRandomCorrelationId.IdGenerator

          val result: Future[Result] = controller.generateReportInternal(simpleNino, simpleCalculationId.toString)(fakeGetRequest)

          status(result) shouldBe expectedStatus
          contentAsJson(result) shouldBe expectedBody
          contentType(result) shouldBe Some("application/json")
          header("X-CorrelationId", result) shouldBe Some(internalCorrelationId)
        }
      }

      val errorInErrorOut =
        Seq(
          (ServerError, INTERNAL_SERVER_ERROR, DownstreamError.toJson),
          (ServiceUnavailableError, INTERNAL_SERVER_ERROR, ServiceUnavailableError.toJson)
        )

      errorInErrorOut.foreach(args => (runTest _).tupled(args))
    }

    "a request fails due to a failed RDSService.submit " should {

      def runTest(mtdError: MtdError, expectedStatus: Int, expectedBody: JsValue): Unit = {
        s"return the expected error ${mtdError.code} when controller is set to return error from RDSService.submit " in new Test {

          MockEnrolmentsAuthService.authoriseUser()
          MockIntegrationFrameworkService.getCalculationInfo(simpleCalculationId, simpleNino)
          MockInsightService.assess(simpleFraudRiskRequest)
          MockRdsService.submitFail(simpleAssessmentRequestForSelfAssessment, simpleFraudRiskReport, simpleInternalOrigin, mtdError)
          MockCurrentDateTime.getDateTime()
          MockProvideRandomCorrelationId.IdGenerator

          val result: Future[Result] = controller.generateReportInternal(simpleNino, simpleCalculationId.toString)(fakeGetRequest)

          status(result) shouldBe expectedStatus
          contentAsJson(result) shouldBe expectedBody
          contentType(result) shouldBe Some("application/json")
          header("X-CorrelationId", result) shouldBe Some(internalCorrelationId)
        }
      }

      val errorInErrorOut =
        Seq(
          (ServerError, INTERNAL_SERVER_ERROR, DownstreamError.toJson),
          (ServiceUnavailableError, INTERNAL_SERVER_ERROR, ServiceUnavailableError.toJson),
          (MatchingResourcesNotFoundError, NOT_FOUND, MatchingResourcesNotFoundError.toJson),
          (InvalidCredentialsError, UNAUTHORIZED, InvalidCredentialsError.toJson)
        )

      errorInErrorOut.foreach(args => (runTest _).tupled(args))
    }
  }
}