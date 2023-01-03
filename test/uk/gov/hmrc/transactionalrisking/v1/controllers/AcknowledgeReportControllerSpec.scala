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

package uk.gov.hmrc.transactionalrisking.v1.controllers

import play.api.libs.json.JsValue
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transactionalrisking.mocks.utils.utils.MockCurrentDateTime
import uk.gov.hmrc.transactionalrisking.v1.TestData.CommonTestData._
import uk.gov.hmrc.transactionalrisking.v1.mocks.requestParsers._
import uk.gov.hmrc.transactionalrisking.v1.mocks.services._
import uk.gov.hmrc.transactionalrisking.v1.mocks.utils.MockIdGenerator
import uk.gov.hmrc.transactionalrisking.v1.models.errors._
import uk.gov.hmrc.transactionalrisking.v1.models.request.AcknowledgeReportRawData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AcknowledgeReportControllerSpec
  extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockNrsService
    with MockInsightService
    with MockRdsService
    with MockAcknowledgeRequestParser
    with MockCurrentDateTime
    with MockIdGenerator {


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

        val acknowledgeReportRawData: AcknowledgeReportRawData = AcknowledgeReportRawData(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)

        MockEnrolmentsAuthService.authoriseUser()
        MockAcknowledgeRequestParser.parseRequest(acknowledgeReportRawData)
        MockRdsService.acknowlegeRds(simpleAcknowledgeReportRequest)
        MockCurrentDateTime.getDateTime()
        MockNrsService.stubAcknowledgement(simpleNRSResponseAcknowledgeSubmission)

        MockProvideRandomCorrelationId.IdGenerator

        val result: Future[Result] = controller.acknowledgeReportForSelfAssessment(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)(fakeGetRequest)

        status(result) shouldBe NO_CONTENT
        header("X-CorrelationId", result) shouldBe Some(correlationId)

      }

      "return 204 even when NRS was unable to non repudiate the event" in new Test {

        val acknowledgeReportRawData: AcknowledgeReportRawData = AcknowledgeReportRawData(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)

        MockEnrolmentsAuthService.authoriseUser()
        MockAcknowledgeRequestParser.parseRequest(acknowledgeReportRawData)
        MockRdsService.acknowlegeRds(simpleAcknowledgeReportRequest)
        MockCurrentDateTime.getDateTime()
        MockNrsService.stubFailureAcknowledgementDueToException()

        MockProvideRandomCorrelationId.IdGenerator

        val result: Future[Result] = controller.acknowledgeReportForSelfAssessment(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)(fakeGetRequest)

        status(result) shouldBe NO_CONTENT
        header("X-CorrelationId", result) shouldBe Some(correlationId)

      }
    }


    "a request fails due to failed authorisedAction that gives a NinoFormatError" should {

      s"return the NinoFormatError error  to indicate that the nino is  invalid. " in new Test {

        MockCurrentDateTime.getDateTime()
        MockProvideRandomCorrelationId.IdGenerator

        val result: Future[Result] = controller.acknowledgeReportForSelfAssessment(simpleNinoInvalid, simpleReportId.toString, simpleRDSCorrelationId)(fakeGetRequest)

        status(result) shouldBe BAD_REQUEST

        contentAsJson(result) shouldBe NinoFormatError.toJson
        contentType(result) shouldBe Some("application/json")
        header("X-CorrelationId", result) shouldBe Some(correlationId)

      }
    }

    "a request fails due to failed authorisedAction failure" should {

      def runTest(mtdError: MtdError, expectedStatus: Int, expectedBody: JsValue): Unit = {
        s"return the expected error ${mtdError.code} to indicate that the data has not been accepted and saved due to authorisedAction returning an error." in new Test {

          MockEnrolmentsAuthService.authoriseUserFail(mtdError)
          MockCurrentDateTime.getDateTime()
          MockProvideRandomCorrelationId.IdGenerator

          val result: Future[Result] = controller.acknowledgeReportForSelfAssessment(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)(fakeGetRequest)

          status(result) shouldBe expectedStatus
          contentAsJson(result) shouldBe expectedBody
          contentType(result) shouldBe Some("application/json")
          header("X-CorrelationId", result) shouldBe Some(correlationId)

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

    "a request fails due to failed parseRequest failure" should {

      def runTest(mtdError: MtdError, expectedStatus: Int, expectedBody: JsValue): Unit = {
        s"return the expected error ${mtdError.code} to indicate that the data has not been accepted and saved due to parseRequest returning an error. " in new Test {

          val acknowledgeReportRawData: AcknowledgeReportRawData = AcknowledgeReportRawData(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)

          MockEnrolmentsAuthService.authoriseUser()
          MockAcknowledgeRequestParser.parseRequestFail(acknowledgeReportRawData, mtdError)
          MockCurrentDateTime.getDateTime()
          MockProvideRandomCorrelationId.IdGenerator

          val result: Future[Result] = controller.acknowledgeReportForSelfAssessment(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)(fakeGetRequest)

          status(result) shouldBe expectedStatus
          contentAsJson(result) shouldBe expectedBody
          contentType(result) shouldBe Some("application/json")
          header("X-CorrelationId", result) shouldBe Some(correlationId)

        }
      }

      val errorInErrorOut =
        Seq(
          (ServerError, INTERNAL_SERVER_ERROR, DownstreamError.toJson),
          (ServiceUnavailableError, INTERNAL_SERVER_ERROR, DownstreamError.toJson),
          (NinoFormatError, BAD_REQUEST, NinoFormatError.toJson),
          (FormatReportIdError, BAD_REQUEST, FormatReportIdError.toJson),
          (MatchingResourcesNotFoundError, SERVICE_UNAVAILABLE, ServiceUnavailableError.toJson),
          (ClientOrAgentNotAuthorisedError, FORBIDDEN, ClientOrAgentNotAuthorisedError.toJson)
        )

      errorInErrorOut.foreach(args => (runTest _).tupled(args))
    }

    "a request fails due to failed due to RdsService.acknowledge failure" should {

      def runTest(mtdError: MtdError, expectedStatus: Int, expectedBody: JsValue): Unit = {
        s"return the expected error ${mtdError.code} to indicate that the data has not been accepted and saved due to RdsService.acknowledge" in new Test {

          val acknowledgeReportRawData: AcknowledgeReportRawData = AcknowledgeReportRawData(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)

          MockEnrolmentsAuthService.authoriseUser()
          MockAcknowledgeRequestParser.parseRequest(acknowledgeReportRawData)
          MockRdsService.acknowlegeRdsFail(simpleAcknowledgeReportRequest, mtdError)
          MockCurrentDateTime.getDateTime()

          MockProvideRandomCorrelationId.IdGenerator

          val result: Future[Result] = controller.acknowledgeReportForSelfAssessment(simpleNino, simpleCalculationId.toString, simpleRDSCorrelationId)(fakeGetRequest)

          status(result) shouldBe expectedStatus
          contentAsJson(result) shouldBe expectedBody
          contentType(result) shouldBe Some("application/json")
          header("X-CorrelationId", result) shouldBe Some(correlationId)

        }
      }

      val errorInErrorOut =
        Seq(
          (ServerError, INTERNAL_SERVER_ERROR, DownstreamError.toJson),
          (ServiceUnavailableError, INTERNAL_SERVER_ERROR, DownstreamError.toJson),
          (NinoFormatError, BAD_REQUEST, NinoFormatError.toJson),
          (FormatReportIdError, BAD_REQUEST, FormatReportIdError.toJson),
          (MatchingResourcesNotFoundError, SERVICE_UNAVAILABLE, ServiceUnavailableError.toJson),
          (ClientOrAgentNotAuthorisedError, FORBIDDEN, ClientOrAgentNotAuthorisedError.toJson)
        )

      errorInErrorOut.foreach(args => (runTest _).tupled(args))
    }

      "a request fails due to being unable to construct NRS event" should {
        "return the expected error to indicate that the data has not been accepted or saved due to failed NRSService submit" in new Test {

          val acknowledgeReportRawData: AcknowledgeReportRawData = AcknowledgeReportRawData(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)

          MockEnrolmentsAuthService.authoriseUser()
          MockAcknowledgeRequestParser.parseRequest(acknowledgeReportRawData)
          MockRdsService.acknowlegeRds(simpleAcknowledgeReportRequest)
          MockCurrentDateTime.getDateTime()

          MockProvideRandomCorrelationId.IdGenerator

          MockNrsService.stubUnableToConstructNRSEventForAcknowledge()

          val result: Future[Result] = controller.acknowledgeReportForSelfAssessment(simpleNino, simpleCalculationId.toString, simpleRDSCorrelationId)(fakeGetRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR
         // contentAsJson(result) shouldBe ??? // TODO what should the body be as per the spec?
          //contentType(result) shouldBe Some("application/json")
          header("X-CorrelationId", result) shouldBe Some(correlationId)

        }
      }
  }
}