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
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transactionalrisking.controllers.ControllerBaseSpec
import uk.gov.hmrc.transactionalrisking.mocks.utils.utils.MockCurrentDateTime
import uk.gov.hmrc.transactionalrisking.v1.TestData.CommonTestData.commonTestData._
import uk.gov.hmrc.transactionalrisking.v1.mocks.requestParsers._
import uk.gov.hmrc.transactionalrisking.v1.mocks.services._
import uk.gov.hmrc.transactionalrisking.v1.mocks.utils.MockIdGenerator
import uk.gov.hmrc.transactionalrisking.v1.models.errors.{DownstreamError, MatchingResourcesNotFoundError, MtdError, ResourceNotFoundError, ServiceUnavailableError}
import uk.gov.hmrc.transactionalrisking.v1.models.request.AcknowledgeReportRawData

import scala.concurrent.ExecutionContext.Implicits.global

class AcknowledgeReportControllerSpec
  extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockNrsService
    with MockInsightService
    with MockRdsService
    with MockAcknowledgeRequestParser
    with MockCurrentDateTime
    with MockIdGenerator {


  implicit val correlationId: String = "X-ID"

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller: TestController = new TestController()

    class TestController extends AcknowledgeReportController(
      cc = cc,
      requestParser = mockAcknowledgeRequestParser,
      authService = mockEnrolmentsAuthService,
      nonRepudiationService = mockNrsService,
      rdsService = mockRdsService,
      currentDateTime = mockCurrentDateTime,
      idGenerator = mockIdGenerator
    )

  }

  "acknowledgeReportForSelfAssessment" when {
    "a valid request is supplied" should {
      "return 204 to indicate that the data has been accepted and saved and that nothing else needs to be return in the body." in new Test {

        val acknowledgeReportRawData: AcknowledgeReportRawData = AcknowledgeReportRawData(simpleNino, simpleReportID.toString, simpleRDSCorrelationID)

        MockEnrolmentsAuthService.authoriseUser()
        MockAcknowledgeRequestParser.parseRequest(acknowledgeReportRawData)
        MockRdsService.acknowlegeRds(simpleAcknowledgeReportRequest)
        MockCurrentDateTime.getDateTime()
        MockNrsService.submit(simpleAcknowledgeReportRequestData,  simpleSubmissionTimestamp, simpleReportNotableEventType, simpleNRSResponseAcknowledgeSubmission )

        MockProvideRandomCorrelationId.IdGenerator

        val result = controller.acknowledgeReportForSelfAssessment(simpleNino, simpleReportID.toString, simpleRDSCorrelationID)(fakeGetRequest)

        status(result) shouldBe NO_CONTENT
        //val ret = contentAsJson(result)
        //contentAsJson(result) shouldBe expectedBody
        //contentType(result) shouldBe None
        header("X-CorrelationId", result) shouldBe Some(internalCorrelationID)

      }
    }

    "a request fails due to failed parseRequest" should {

      def runTest(mtdError: MtdError, expectedStatus: Int, expectedBody: JsValue): Unit = {
        s"return the expected error ${mtdError.code} to indicate that the data has not been accepted and saved due to parseRequestFail. " in new Test {

          val acknowledgeReportRawData: AcknowledgeReportRawData = AcknowledgeReportRawData(simpleNino, simpleReportID.toString, simpleRDSCorrelationID)

          MockEnrolmentsAuthService.authoriseUser()
          MockAcknowledgeRequestParser.parseRequestFail(acknowledgeReportRawData, mtdError)
          MockCurrentDateTime.getDateTime()
          MockProvideRandomCorrelationId.IdGenerator

          val result = controller.acknowledgeReportForSelfAssessment(simpleNino, simpleCalculationID.toString, simpleRDSCorrelationID)(fakeGetRequest)

          status(result) shouldBe expectedStatus
          contentType(result) shouldBe Some("application/json")
          header("X-CorrelationId", result) shouldBe Some(internalCorrelationID)

        }
      }

      val errorInErrorOut =
        Seq(
          (MatchingResourcesNotFoundError, NOT_FOUND, MatchingResourcesNotFoundError.toJson)
          //,(DownstreamError, SERVICE_UNAVAILABLE, DownstreamError.toJson)
          //, (ServiceUnavailableError, SERVICE_UNAVAILABLE, DownstreamError.toJson)
            a)

      errorInErrorOut.foreach(args => (runTest _).tupled(args))
    }

    "a request fails due to failed due to acknowlegeRdsService.acknowlegeRds failure" should {

      def runTest(mtdError: MtdError, expectedStatus: Int, expectedBody: JsValue): Unit = {
        s"return the expected error ${mtdError.code} to indicate that the data has not been accepted and saved due to acknowlegeRds. " in new Test {

          val acknowledgeReportRawData: AcknowledgeReportRawData = AcknowledgeReportRawData(simpleNino, simpleReportID.toString, simpleRDSCorrelationID)

          MockEnrolmentsAuthService.authoriseUser()
          MockAcknowledgeRequestParser.parseRequest(acknowledgeReportRawData)
          MockRdsService.acknowlegeRdsFail(simpleAcknowledgeReportRequest, mtdError)
          MockCurrentDateTime.getDateTime()

          MockProvideRandomCorrelationId.IdGenerator

          val result = controller.acknowledgeReportForSelfAssessment(simpleNino, simpleCalculationID.toString, simpleRDSCorrelationID)(fakeGetRequest)

          status(result) shouldBe expectedStatus // NOT_FOUND
          contentAsJson(result) shouldBe expectedBody
          contentType(result) shouldBe Some("application/json")

          header("X-CorrelationId", result) shouldBe Some(internalCorrelationID)

        }
      }

      val errorInErrorOut =
        Seq(
          (MatchingResourcesNotFoundError, NOT_FOUND, MatchingResourcesNotFoundError.toJson)
          //,(DownstreamError, SERVICE_UNAVAILABLE, DownstreamError.toJson)
          //, (ServiceUnavailableError, SERVICE_UNAVAILABLE, DownstreamError.toJson)
        )

      errorInErrorOut.foreach(args => (runTest _).tupled(args))
    }

//    "a request fails due to failed NRSService submit" should {
//      "return the expected error to indicate that the data has not been accepted and saved due to failed NRSService submit. " in new Test {
//
//        val acknowledgeReportRawData: AcknowledgeReportRawData = AcknowledgeReportRawData(simpleNino, simpleReportID.toString, simpleRDSCorrelationID)
//
//        MockEnrolmentsAuthService.authoriseUser()
//        MockAcknowledgeRequestParser.parseRequest(acknowledgeReportRawData)
//        MockRdsService.acknowlegeRds(simpleAcknowledgeReportRequest)
//        MockCurrentDateTime.getDateTime()
//
//        MockProvideRandomCorrelationId.IdGenerator
//        MockNrsService.submitFail(generateReportRequest = simpleAcknowledgeReportRequestData, generatedNrsId = simpleAcknowledgeNrsID,
//          submissionTimestamp = simpleSubmissionTimestamp, notableEventType = simpleNotableEventType)
//
//        val result = controller.acknowledgeReportForSelfAssessment(simpleNino, simpleCalculationID.toString, simpleRDSCorrelationID)(fakeGetRequest)
//
//        status(result) shouldBe NOT_FOUND
//        //contentAsJson(result) shouldBe expectedBody
//        contentType(result) shouldBe Some("application/json")
//
//        header("X-CorrelationId", result) shouldBe Some(internalCorrelationID)
//
//      }
//    }
  }
}