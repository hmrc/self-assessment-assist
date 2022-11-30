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


import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transactionalrisking.mocks.utils.utils.MockCurrentDateTime
import uk.gov.hmrc.transactionalrisking.v1.TestData.CommonTestData.commonTestData._
import uk.gov.hmrc.transactionalrisking.v1.controllers.GenerateReportController
import uk.gov.hmrc.transactionalrisking.v1.mocks.services._
import uk.gov.hmrc.transactionalrisking.v1.mocks.utils.MockIdGenerator
import uk.gov.hmrc.transactionalrisking.v1.models.errors.{BadRequestError, DownstreamError, MatchingResourcesNotFoundError, MtdError, ServiceUnavailableError, UnsupportedVersionError}

import scala.concurrent.ExecutionContext.Implicits.global


class GenerateReportControllerSpec
  extends ControllerBaseSpec
    with MockIntegrationFrameworkService
    with MockEnrolmentsAuthService
    with MockNrsService
    with MockInsightService
    with MockRdsService
    with MockCurrentDateTime
    with MockIdGenerator {


  object unexpectedError extends MtdError(code = "UNEXPECTED_ERROR", message = "This is an unexpected error")

  implicit val correlationId: String = "X-ID"

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

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
    //override authorisedAction(nino: String, nrsRequired: Boolean = false): ActionBuilder[UserRequest, AnyContent]

  }

  "generateReport" when {
    "a valid request is supplied" should {
      "return the expected data when controller is called" in new Test {

        MockEnrolmentsAuthService.authoriseUser()
        MockIntegrationFrameworkService.getCalculationInfo(simpleCalculationID, simpleNino)
        MockInsightService.assess(simpleFraudRiskRequest)
        MockRdsService.submit(simpleAssessmentRequestForSelfAssessment, simpleFraudRiskReport, simpleInternalOrigin)
        MockCurrentDateTime.getDateTime()
        MockNrsService.submit(simpleGenerateReportControllerRequestData, simpleSubmissionTimestamp, simpleReportNotableEventType, simpleNRSResponseReportSubmission)
        MockProvideRandomCorrelationId.IdGenerator

        val result = controller.generateReportInternal(simpleNino, simpleCalculationID.toString)(fakeGetRequest)
        status(result) shouldBe OK
        contentAsJson(result) shouldBe simpleAsssementReportMtdJson
        contentType(result) shouldBe Some("application/json")
        header("X-CorrelationId", result) shouldBe Some(internalCorrelationID)
      }
    }

    "a request fails due to a failed getCalculationInfo" should {

      def runTest(mtdError: MtdError, expectedStatus: Int, expectedBody: JsValue): Unit = {
        s"return the expected error ${mtdError.code} when controller is set to return error from getCalculationInfo " in new Test {

          MockEnrolmentsAuthService.authoriseUser()
          MockIntegrationFrameworkService.getCalculationInfoFail(simpleCalculationID, simpleNino, mtdError)
          MockCurrentDateTime.getDateTime()
          MockProvideRandomCorrelationId.IdGenerator

          val result = controller.generateReportInternal(simpleNino, simpleCalculationID.toString)(fakeGetRequest)

          status(result) shouldBe expectedStatus
          contentAsJson(result) shouldBe MatchingResourcesNotFoundError.toJson
          contentType(result) shouldBe Some("application/json")
          header("X-CorrelationId", result) shouldBe Some(internalCorrelationID)
        }
      }

      val errorInErrorOut =
        Seq(
          (MatchingResourcesNotFoundError, NOT_FOUND, MatchingResourcesNotFoundError.toJson)
//          , (ServiceUnavailableError, SERVICE_UNAVAILABLE, DownstreamError.toJson)
        )

      errorInErrorOut.foreach(args => (runTest _).tupled(args))

    }

    "a request fails due to a failed assess " should {

      def runTest(mtdError: MtdError, expectedStatus: Int, expectedBody: JsValue): Unit = {
        s"return the expected error ${mtdError.code} when controller is set to return error from assess " in new Test {

          MockEnrolmentsAuthService.authoriseUser()
          MockIntegrationFrameworkService.getCalculationInfo(simpleCalculationID, simpleNino)
          MockInsightService.assessFail(simpleFraudRiskRequest, mtdError)
          MockCurrentDateTime.getDateTime()
          MockProvideRandomCorrelationId.IdGenerator

          val result = controller.generateReportInternal(simpleNino, simpleCalculationID.toString)(fakeGetRequest)

          status(result) shouldBe expectedStatus
          contentAsJson(result) shouldBe MatchingResourcesNotFoundError.toJson
          contentType(result) shouldBe Some("application/json")
          header("X-CorrelationId", result) shouldBe Some(internalCorrelationID)
        }
      }

      val errorInErrorOut =
        Seq(
          (MatchingResourcesNotFoundError, NOT_FOUND, MatchingResourcesNotFoundError.toJson)
          //,(DownstreamError, SERVICE_UNAVAILABLE, DownstreamError.toJson)
  //        , (ServiceUnavailableError, SERVICE_UNAVAILABLE, DownstreamError.toJson)
        )

      errorInErrorOut.foreach(args => (runTest _).tupled(args))

    }

    "a request fails due to a failed RDSService.submit " should {

      def runTest(mtdError: MtdError, expectedStatus: Int, expectedBody: JsValue): Unit = {
        s"return the expected error ${mtdError.code} when controller is set to return error from RDSService.submit " in new Test {

          MockEnrolmentsAuthService.authoriseUser()
          MockIntegrationFrameworkService.getCalculationInfo(simpleCalculationID, simpleNino)
          MockInsightService.assess(simpleFraudRiskRequest)
          MockRdsService.submitFail(simpleAssessmentRequestForSelfAssessment, simpleFraudRiskReport, simpleInternalOrigin, mtdError)
          MockCurrentDateTime.getDateTime()
          MockProvideRandomCorrelationId.IdGenerator

          val result = controller.generateReportInternal(simpleNino, simpleCalculationID.toString)(fakeGetRequest)

          status(result) shouldBe expectedStatus
          contentAsJson(result) shouldBe expectedBody
          contentType(result) shouldBe Some("application/json")
          header("X-CorrelationId", result) shouldBe Some(internalCorrelationID)
        }
      }

      val errorInErrorOut =
        Seq(
          (MatchingResourcesNotFoundError, NOT_FOUND, MatchingResourcesNotFoundError.toJson)
          //,(DownstreamError, SERVICE_UNAVAILABLE, DownstreamError.toJson)
//          , (ServiceUnavailableError, SERVICE_UNAVAILABLE, DownstreamError.toJson)
        )

      errorInErrorOut.foreach(args => (runTest _).tupled(args))

    }


//    "a request fails due to a failed NRSService.submit " should {
//
//      def runTest( expectedStatus: Int, expectedBody: JsValue): Unit = {
//        s"return the expected error ${expectedStatus} when controller is set to return error from NRSService.submit " in new Test {
//
//          MockEnrolmentsAuthService.authoriseUser()
//          MockIntegrationFrameworkService.getCalculationInfo(simpleCalculationID, simpleNino)
//          MockInsightService.assess(simpleFraudRiskRequest)
//          MockRdsService.submit(simpleAssessmentRequestForSelfAssessment, simpleFraudRiskReport, simpleInternalOrigin)
//          MockCurrentDateTime.getDateTime()
//          MockNrsService.submitFail(simpleGenerateReportControllerRequestData, simpleGenerateReportControllerNrsID, simpleSubmissionTimestamp, simpleNotableEventType)
//          MockProvideRandomCorrelationId.IdGenerator
//
//          val result = controller.generateReportInternal(simpleNino, simpleCalculationID.toString)(fakeGetRequest)
//
//          status(result) shouldBe expectedStatus
//          contentAsJson(result) shouldBe expectedBody
//          contentType(result) shouldBe Some("application/json")
//          //        header("X-CorrelationId", result) shouldBe Some(correlationID)
//        }
//      }
//
//      // Only one thing to test. May change later.
//      val errorInErrorOut =
//        Seq(
//          (NOT_FOUND, MatchingResourcesNotFoundError.toJson)
//        )
//
//      errorInErrorOut.foreach(args => (runTest _).tupled(args))
//
//    }
  }
}