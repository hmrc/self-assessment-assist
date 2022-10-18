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


import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transactionalrisking.mocks.utils.MockCurrentDateTime
import uk.gov.hmrc.transactionalrisking.v1.CommonTestData.commonTestData._
import uk.gov.hmrc.transactionalrisking.v1.mocks.services._
import uk.gov.hmrc.transactionalrisking.v1.mocks.utils.MockProvideRandomCorrelationId

import scala.concurrent.ExecutionContext.Implicits.global


class GenerateReportControllerSpec
  extends ControllerBaseSpec
  with MockIntegrationFrameworkService
  with MockEnrolmentsAuthService
  with MockNrsService
  with MockInsightService
  with MockRdsService
  with MockCurrentDateTime
    with MockProvideRandomCorrelationId
   {



  implicit val correlationId: String = "X-ID"

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller:TestController = new TestController()

    class TestController extends GenerateReportController(
      cc = cc,
      integrationFrameworkService = mockIntegrationFrameworkService,
      authService = mockEnrolmentsAuthService,
      nonRepudiationService = mockNrsService,
      insightService = mockInsightService,
      rdsService = mockRdsService,
      currentDateTime = mockCurrentDateTime,
      provideRandomCorrelationId = mockProvideRandomCorrelationId
    )
      //override authorisedAction(nino: String, nrsRequired: Boolean = false): ActionBuilder[UserRequest, AnyContent]

  }

  "generateReport" when {
    "a valid request is supplied" should {
      "return the expected data when controller is called" in new Test {


        MockEnrolmentsAuthService.authoriseUser()
        MockIntegrationFrameworkService.getCalculationInfo(simpleCalculationID,simpleNino)
        MockInsightService.assess(simpleFraudRiskRequest)
        MockRdsService.submit(simpleAssessmentRequestForSelfAssessment,simpleFraudRiskReport,simpleInternalOrigin)
        MockCurrentDateTime.getDateTime()
        MockNrsService.submit(simpleGenerateReportRequest,simpleGeneratedNrsID,simpleSubmissionTimestamp,simpeNotableEventType)
        MockNrsService.submit(generateReportRequest = simpleGenerateReportRequest, generatedNrsId=simpleGeneratedNrsID,
          submissionTimestamp = simpleSubmissionTimestamp, notableEventType = simpeNotableEventType )
         MockProvideRandomCorrelationId.getRandomCorrelationId()

        val result = controller.generateReportInternal( simpleNino, simpleCalculationID.toString)(fakeGetRequest)
        status(result) shouldBe OK
        contentAsJson(result) shouldBe simpleAsssementReportMtdJson
        contentType(result) shouldBe Some("application/json")
//        header("X-CorrelationId", result) shouldBe Some(correlationID)

        // Put the nrs save to test here.

      }

    }
  }
//
//  "service errors occur" must {
//    def serviceErrors(mtdError: MtdError, expectedStatus: Int): Unit = {
//      s"a $mtdError error is returned from the service" in new Test {
//
//        MockTransactionalRiskingService
//          .assess(request, origin)
//          .returns( Future.successful(Left(ErrorWrapper( /*correlationId,*/ mtdError))))
//
//        val result: Future[Result] = controller.generateReportInternal( nino, calculationID.toString)(fakeGetRequest)
//
//        status(result) shouldBe expectedStatus
//        contentAsJson(result) shouldBe Json.toJson(mtdError)
//
//      }
//    }
//
//    object unexpectedError extends MtdError(code = "UNEXPECTED_ERROR", message = "This is an unexpected error")
//
//    val input = Seq(
//      (ClientOrAgentNotAuthorisedError, FORBIDDEN),
//      (ForbiddenDownstreamError, FORBIDDEN),
//      (unexpectedError, INTERNAL_SERVER_ERROR)
//    )
//
//    input.foreach(args => (serviceErrors _).tupled(args))
//  }
//
////  "a NOT_FOUND error is returned from the service" must {
////    s"return a 404 status with an empty body" in new Test {
////
////      MockViewReturnRequestParser
////        .parse(viewReturnRawData)
////        .returns(Right(viewReturnRequest))
////
////      MockViewReturnService
////        .viewReturn(viewReturnRequest)
////        .returns(Future.successful(Left(ErrorWrapper(correlationId, EmptyNotFoundError))))
////
////      val result: Future[Result] = controller.viewReturn(vrn, periodKey)(fakeGetRequest)
////
////      status(result) shouldBe NOT_FOUND
////      contentAsString(result) shouldBe ""
////      header("X-CorrelationId", result) shouldBe Some(correlationID)
////
////      val auditResponse: AuditResponse = AuditResponse(NOT_FOUND, Some(Seq(AuditError(EmptyNotFoundError.code))), None)
////      MockedAuditService.verifyAuditEvent(AuditEvents.auditReturns(correlationId,
////        UserDetails("Individual", None, "client-Id"), auditResponse)).once
////    }
////  }

}