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

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import uk.gov.hmrc.selfassessmentassist.api.TestData.CommonTestData
import uk.gov.hmrc.selfassessmentassist.api.TestData.CommonTestData._
import uk.gov.hmrc.selfassessmentassist.api.controllers.ControllerBaseSpec
import uk.gov.hmrc.selfassessmentassist.api.models.errors._
import uk.gov.hmrc.selfassessmentassist.config.AppConfig
import uk.gov.hmrc.selfassessmentassist.mocks.utils.MockCurrentDateTime
import uk.gov.hmrc.selfassessmentassist.utils.DateUtils
import uk.gov.hmrc.selfassessmentassist.mocks.services.MockEnrolmentsAuthService
import uk.gov.hmrc.selfassessmentassist.v1.mocks.connectors.MockLookupConnector
import uk.gov.hmrc.selfassessmentassist.v1.mocks.requestParsers.MockGenerateReportRequestParser
import uk.gov.hmrc.selfassessmentassist.v1.mocks.services._
import uk.gov.hmrc.selfassessmentassist.v1.mocks.utils.MockIdGenerator
import uk.gov.hmrc.selfassessmentassist.v1.models.request.nrs.{NrsSubmission, SearchKeys}
import uk.gov.hmrc.selfassessmentassist.v1.models.request.{GenerateReportRawData, nrs}
import uk.gov.hmrc.selfassessmentassist.v1.services.testData.RdsTestData.assessmentReportWrapper

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GenerateReportControllerSpec
    extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockLookupConnector
    with MockNrsService
    with MockInsightService
    with MockRdsService
    with MockCurrentDateTime
    with MockIdGenerator
    with MockGenerateReportRequestParser
    with MockIfsService
    with GuiceOneAppPerSuite {

  private val timestamp: OffsetDateTime = OffsetDateTime.parse("2018-04-07T12:13:25.156Z")
  private val formattedDate: String     = timestamp.format(DateUtils.isoInstantDateTimePattern)

  private val appConfig = app.injector.instanceOf[AppConfig]

  trait Test {

    val controller: TestController = new TestController()

    class TestController
        extends GenerateReportController(
          cc = cc,
          requestParser = mockGenerateReportRequestParser,
          authService = mockEnrolmentsAuthService,
          lookupConnector = mockLookupConnector,
          nonRepudiationService = mockNrsService,
          insightService = mockInsightService,
          rdsService = mockRdsService,
          currentDateTime = mockCurrentDateTime,
          idGenerator = mockIdGenerator,
          ifService = mockIfsService,
          config = appConfig
        )

  }

  "generateReport" when {
    "a valid request is supplied" should {
      "return the expected data when controller is called" in new Test {

        MockGenerateReportRequestParser.parseRequest(simpleGenerateReportRawData)
        MockProvideRandomCorrelationId.IdGenerator
        MockEnrolmentsAuthService.authoriseUser()
        MockLookupConnector.mockMtdIdLookupConnector("1234567890")
        MockInsightService.assess(simpleFraudRiskRequest)
        MockRdsService.submit(simpleAssessmentRequestForSelfAssessment, simpleFraudRiskReport, simpleInternalOrigin, simpleAssessmentReportWrapper)
        MockCurrentDateTime.getDateTime
        // MockNrsService.stubAssessmentReport(simpleNRSResponseReportSubmission)
        MockNrsService.stubBuildNrsSubmission(expectedReportPayload)
        MockNrsService.stubNrsSubmit(simpleNRSResponseReportSubmission)
        MockIfsService.stubGenerateReportSubmit(
          simpleAssessmentReport,
          simpleAssessmentReportWrapper.calculationTimestamp,
          simpleAssessmentRequestForSelfAssessment)

        val result: Future[Result] = controller.generateReportInternal(simpleNino, simpleCalculationId.toString, simpleTaxYearFullString)(fakePostRequest)
        status(result) shouldBe OK
        contentAsJson(result) shouldBe simpleAssessmentReportMtdJson
        contentType(result) shouldBe Some("application/json")
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }

      "return the expected data when controller is called even when NRS is unable to non repudiate the event" in new Test {

        MockGenerateReportRequestParser.parseRequest(simpleGenerateReportRawData)
        MockProvideRandomCorrelationId.IdGenerator
        MockEnrolmentsAuthService.authoriseUser()
        MockLookupConnector.mockMtdIdLookupConnector("1234567890")
        MockInsightService.assess(simpleFraudRiskRequest)
        MockRdsService.submit(simpleAssessmentRequestForSelfAssessment, simpleFraudRiskReport, simpleInternalOrigin, simpleAssessmentReportWrapper)
        MockCurrentDateTime.getDateTime
        MockNrsService.stubFailureReportDueToException()
        MockNrsService.stubBuildNrsSubmission(expectedReportPayload)
        MockNrsService.stubNrsSubmit(simpleNRSResponseReportSubmission)
        MockIfsService.stubGenerateReportSubmit(
          simpleAssessmentReportWrapper.report,
          simpleAssessmentReportWrapper.calculationTimestamp,
          simpleAssessmentRequestForSelfAssessment)

        val result: Future[Result] = controller.generateReportInternal(simpleNino, simpleCalculationId.toString, simpleTaxYearFullString)(fakePostRequest)
        status(result) shouldBe OK
        contentAsJson(result) shouldBe simpleAssessmentReportMtdJson
        contentType(result) shouldBe Some("application/json")
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    "a request fails due to an incorrect tax year format" should {
      s"return the tax year format error to indicate the taxYear is invalid if the tax years are not consecutive" in new Test {
        val rawDataNonSequentialTaxYear: GenerateReportRawData = simpleGenerateReportRawData.copy(taxYear = simpleTaxYearInvalid1)
        MockLookupConnector.mockMtdIdLookupConnector("1234567890")
        MockCurrentDateTime.getDateTime
        MockProvideRandomCorrelationId.IdGenerator
        MockGenerateReportRequestParser.parseRequestFail(rawDataNonSequentialTaxYear, TaxYearRangeInvalid)
        MockEnrolmentsAuthService.authoriseUser()

        val result: Future[Result] =
          controller.generateReportInternal(simpleNino, simpleCalculationId.toString, simpleTaxYearInvalid1)(fakePostRequest)

        status(result) shouldBe BAD_REQUEST
        Thread.sleep(1000)

        contentAsJson(result) shouldBe Json.toJson(Seq(TaxYearRangeInvalid))
        contentType(result) shouldBe Some("application/json")
        header("X-CorrelationId", result) shouldBe Some(correlationId)

      }
      s"return the tax year format error to indicate the taxYear format is invalid" in new Test {
        val rawDataInvalidTaxYear: GenerateReportRawData = simpleGenerateReportRawData.copy(simpleTaxYearInvalid2)
        MockLookupConnector.mockMtdIdLookupConnector("1234567890")
        MockCurrentDateTime.getDateTime
        MockProvideRandomCorrelationId.IdGenerator
        MockGenerateReportRequestParser.parseRequestFail(rawDataInvalidTaxYear, TaxYearFormatError)
        MockEnrolmentsAuthService.authoriseUser()

        val result: Future[Result] =
          controller.generateReportInternal(simpleNino, simpleCalculationId.toString, simpleTaxYearInvalid2)(fakePostRequest)

        status(result) shouldBe BAD_REQUEST
        Thread.sleep(1000)

        contentAsJson(result) shouldBe Json.toJson(Seq(TaxYearFormatError))
        contentType(result) shouldBe Some("application/json")
        header("X-CorrelationId", result) shouldBe Some(correlationId)

      }

      s"return the tax year format error to indicate the taxYear format is an invalid string" in new Test {
        val rawDataInvalidTaxYear: GenerateReportRawData = simpleGenerateReportRawData.copy(simpleTaxYearInvalid3)
        MockLookupConnector.mockMtdIdLookupConnector("1234567890")
        MockCurrentDateTime.getDateTime
        MockProvideRandomCorrelationId.IdGenerator
        MockGenerateReportRequestParser.parseRequestFail(rawDataInvalidTaxYear, TaxYearFormatError)
        MockEnrolmentsAuthService.authoriseUser()

        val result: Future[Result] =
          controller.generateReportInternal(simpleNino, simpleCalculationId.toString, simpleTaxYearInvalid3)(fakePostRequest)

        status(result) shouldBe BAD_REQUEST
        Thread.sleep(1000)

        contentAsJson(result) shouldBe Json.toJson(Seq(TaxYearFormatError))
        contentType(result) shouldBe Some("application/json")
        header("X-CorrelationId", result) shouldBe Some(correlationId)

      }
    }

    "a request fails due to failed authorisedAction that gives a NinoFormatError" should {

      s"return the NinoFormatError error  to indicate that the nino is  invalid. " in new Test {

        MockCurrentDateTime.getDateTime
        MockProvideRandomCorrelationId.IdGenerator
        val result: Future[Result] =
          controller.generateReportInternal(simpleNinoInvalid, simpleCalculationId.toString, simpleTaxYearFullString)(fakePostRequest)

        status(result) shouldBe BAD_REQUEST
        Thread.sleep(1000)

        contentAsJson(result) shouldBe Json.toJson(Seq(NinoFormatError))
        contentType(result) shouldBe Some("application/json")
        header("X-CorrelationId", result) shouldBe Some(correlationId)

      }
    }

    "a request fails due to failed EnrolmentsAuthService.authorised failure" should {

      def runTest(mtdError: MtdError, expectedStatus: Int, expectedBody: JsValue): Unit = {
        s"return the expected error ${mtdError.code} to indicate that the data has not been accepted and saved due to EnrolmentsAuthService.authorised returning an error." in new Test {

          MockLookupConnector.mockMtdIdLookupConnector("1234567890")
          MockEnrolmentsAuthService.authoriseUserFail(mtdError)
          MockCurrentDateTime.getDateTime
          MockProvideRandomCorrelationId.IdGenerator
          MockGenerateReportRequestParser.parseRequest(simpleGenerateReportRawData)

          val result: Future[Result] = controller.generateReportInternal(simpleNino, simpleCalculationId.toString, simpleTaxYearFullString)(fakePostRequest)

          status(result) shouldBe expectedStatus
          contentAsJson(result) shouldBe Json.toJson(Seq(expectedBody))
          contentType(result) shouldBe Some("application/json")
          header("X-CorrelationId", result) shouldBe Some(correlationId)

        }
      }

      val errorInErrorOut =
        Seq(
          (ClientOrAgentNotAuthorisedError, FORBIDDEN, ClientOrAgentNotAuthorisedError.asJson),
          (ForbiddenDownstreamError, FORBIDDEN, InternalError.asJson),
          (ServiceUnavailableError, INTERNAL_SERVER_ERROR, InternalError.asJson)
        )

      errorInErrorOut.foreach(args => (runTest _).tupled(args))
    }

    "a request fails due to a failed InsightService.assess " should {

      def runTest(mtdError: MtdError, expectedStatus: Int, expectedBody: JsValue): Unit = {
        s"return the expected error ${mtdError.code} when controller is set to return error from InsightService.assess " in new Test {

          MockEnrolmentsAuthService.authoriseUser()
          MockLookupConnector.mockMtdIdLookupConnector("1234567890")
          MockGenerateReportRequestParser.parseRequest(simpleGenerateReportRawData)
          MockInsightService.assessFail(simpleFraudRiskRequest, mtdError)
          MockCurrentDateTime.getDateTime
          MockProvideRandomCorrelationId.IdGenerator

          val result: Future[Result] = controller.generateReportInternal(simpleNino, simpleCalculationId.toString, simpleTaxYearFullString)(fakePostRequest)

          status(result) shouldBe expectedStatus
          contentAsJson(result) shouldBe Json.toJson(Seq(expectedBody))
          contentType(result) shouldBe Some("application/json")
          header("X-CorrelationId", result) shouldBe Some(correlationId)
        }
      }

      val errorInErrorOut =
        Seq(
          (ServerError, INTERNAL_SERVER_ERROR, InternalError.asJson),
          (ServiceUnavailableError, INTERNAL_SERVER_ERROR, ServiceUnavailableError.asJson)
        )

      errorInErrorOut.foreach(args => (runTest _).tupled(args))
    }

    "a request fails due to a failed RDSService.submit" should {

      def runTest(mtdError: MtdError, expectedStatus: Int, expectedBody: Option[JsValue]): Unit = {
        s"return the expected error ${mtdError.code} when controller is set to return error from RDSService.submit " in new Test {

          MockEnrolmentsAuthService.authoriseUser()
          MockLookupConnector.mockMtdIdLookupConnector("1234567890")
          MockGenerateReportRequestParser.parseRequest(simpleGenerateReportRawData)
          MockInsightService.assess(simpleFraudRiskRequest)
          MockRdsService.submitFail(simpleAssessmentRequestForSelfAssessment, simpleFraudRiskReport, simpleInternalOrigin, mtdError)

          MockIfsService.submitGenerateReportNeverCalled()
          MockNrsService.submitNeverCalled()

          MockCurrentDateTime.getDateTime
          MockProvideRandomCorrelationId.IdGenerator

          val result: Future[Result] = controller.generateReportInternal(simpleNino, simpleCalculationId.toString, simpleTaxYearFullString)(fakePostRequest)

          status(result) shouldBe expectedStatus
          if (expectedBody.nonEmpty) {
            contentAsJson(result) shouldBe Json.toJson(Seq(expectedBody.get))
            contentType(result) shouldBe Some("application/json")
          }
          header("X-CorrelationId", result) shouldBe Some(correlationId)
        }
      }

      val errorInErrorOut =
        Seq(
          (ServerError, INTERNAL_SERVER_ERROR, Some(InternalError.asJson)),
          (ServiceUnavailableError, INTERNAL_SERVER_ERROR, Some(ServiceUnavailableError.asJson)),
          (MatchingResourcesNotFoundError, NOT_FOUND, Some(MatchingResourcesNotFoundError.asJson)),
          (InvalidCredentialsError, UNAUTHORIZED, Some(InvalidCredentialsError.asJson)),
          (NinoFormatError, BAD_REQUEST, Some(NinoFormatError.asJson)),
          (NoAssessmentFeedbackFromRDS, NO_CONTENT, None),
          (CalculationIdFormatError, BAD_REQUEST, Some(CalculationIdFormatError.asJson)),
          (MatchingCalculationIDNotFoundError, NOT_FOUND, Some(MatchingCalculationIDNotFoundError.asJson)),
          (ClientOrAgentNotAuthorisedError, FORBIDDEN, Some(ClientOrAgentNotAuthorisedError.asJson)),
          (RdsAuthError, INTERNAL_SERVER_ERROR, Some(ForbiddenDownstreamError.asJson)),
          (BadRequestError, BAD_REQUEST, Some(BadRequestError.asJson)),
          (InvalidJson, INTERNAL_SERVER_ERROR, Some(MatchingResourcesNotFoundError.asJson))
        )

      errorInErrorOut.foreach(args => (runTest _).tupled(args))
    }

    "a request fails due to being unable to construct NRS event" should {
      "return the expected error to indicate that the data has not been accepted or saved due to failed NRSService submit" in new Test {

        MockEnrolmentsAuthService.authoriseUser()
        MockLookupConnector.mockMtdIdLookupConnector("1234567890")
        MockInsightService.assess(simpleFraudRiskRequest)
        MockRdsService.submit(simpleAssessmentRequestForSelfAssessment, simpleFraudRiskReport, simpleInternalOrigin, assessmentReportWrapper)
        MockCurrentDateTime.getDateTime
        MockNrsService.stubUnableToConstrucNrsSubmission()
        MockProvideRandomCorrelationId.IdGenerator
        MockNrsService.stubUnableToConstrucNrsSubmission()
        MockNrsService.stubNrsSubmit(simpleNRSResponseReportSubmission)
        MockGenerateReportRequestParser.parseRequest(simpleGenerateReportRawData)
        MockIfsService.stubGenerateReportSubmit(
          assessmentReportWrapper.report,
          assessmentReportWrapper.calculationTimestamp,
          simpleAssessmentRequestForSelfAssessment)

        val result: Future[Result] = controller.generateReportInternal(simpleNino, simpleCalculationId.toString, simpleTaxYearFullString)(fakePostRequest)

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsJson(result) shouldBe Json.toJson(Seq(InternalError))
        contentType(result) shouldBe Some("application/json")
        header("X-CorrelationId", result) shouldBe Some(correlationId)

      }
    }

    "a request fails due to a failed IfsService.submit " should {

      def runTest(mtdError: MtdError, expectedStatus: Int, expectedBody: JsValue): Unit = {
        s"return the expected error ${mtdError.code} when controller is set to return error from IfService.submit " in new Test {
          MockGenerateReportRequestParser.parseRequest(simpleGenerateReportRawData)
          MockProvideRandomCorrelationId.IdGenerator
          MockEnrolmentsAuthService.authoriseUser()
          MockLookupConnector.mockMtdIdLookupConnector("1234567890")
          MockInsightService.assess(simpleFraudRiskRequest)
          MockRdsService.submit(simpleAssessmentRequestForSelfAssessment, simpleFraudRiskReport, simpleInternalOrigin, simpleAssessmentReportWrapper)
          MockCurrentDateTime.getDateTime
          MockIfsService.stubFailedSubmit(
            simpleAssessmentReport,
            simpleAssessmentReportWrapper.calculationTimestamp,
            simpleAssessmentRequestForSelfAssessment,
            mtdError)

          val result: Future[Result] = controller.generateReportInternal(simpleNino, simpleCalculationId.toString, simpleTaxYearFullString)(fakePostRequest)
          status(result) shouldBe expectedStatus
          contentAsJson(result) shouldBe Json.toJson(Seq(expectedBody))
          contentType(result) shouldBe Some("application/json")
          header("X-CorrelationId", result) shouldBe Some(correlationId)

        }
      }

      runTest(ServerError, INTERNAL_SERVER_ERROR, InternalError.asJson)
    }
  }

  private val expectedReportPayload: NrsSubmission =
    NrsSubmission(
      payload =
        "eyJyZXBvcnRJZCI6ImRiNzQxZGZmLTQwNTQtNDc4ZS04OGQyLTU5OTNlOTI1YzdhYiIsIm1lc3NhZ2VzIjpbeyJ0aXRsZSI6IlR1cm5vdmVyIGFuZCBjb3N0IG9mIHNhbGVzIiwiYm9keSI6IllvdXIgY29zdCBvZiBzYWxlcyBpcyBncmVhdGVyIHRoYW4gaW5jb21lIiwiYWN0aW9uIjoiUGxlYXNlIHJlYWQgb3VyIGd1aWRhbmNlIiwibGlua3MiOlt7InRpdGxlIjoiT3VyIGd1aWRhbmNlIiwidXJsIjoiaHR0cHM6Ly93d3cuZ292LnVrL2V4cGVuc2VzLWlmLXlvdXJlLXNlbGYtZW1wbG95ZWQifV0sInBhdGgiOiJnZW5lcmFsL3RvdGFsX2RlY2xhcmVkX3R1cm5vdmVyIn1dLCJuaW5vIjoibmlubyIsInRheFllYXIiOiIyMDIxLTIwMjIiLCJjYWxjdWxhdGlvbklkIjoiOTlkNzU4ZjYtYzRiZS00MzM5LTgwNGUtZjc5Y2YwNjEwZDRmIiwiY29ycmVsYXRpb25JZCI6ImU0MzI2NGM1LTUzMDEtNGVjZS1iM2QzLTFlOGE4ZGQ5M2I0YiJ9",
      metadata = nrs.Metadata(
        businessId = "saa",
        notableEvent = "saa-report-generated",
        payloadContentType = "application/json",
        payloadSha256Checksum = "acdf5c0add9e434375e81797ad21fd409bc55f6d4f264d7aa302ca1ef4a01058",
        userSubmissionTimestamp = formattedDate,
        identityData = Some(CommonTestData.identityCorrectModel),
        userAuthToken = "Bearer aaaa",
        headerData = Json.toJson(
          Map(
            "Host"          -> "localhost",
            "dummyHeader1"  -> "dummyValue1",
            "dummyHeader2"  -> "dummyValue2",
            "Authorization" -> "Bearer aaaa"
          )),
        searchKeys = SearchKeys(
          reportId = "db741dff-4054-478e-88d2-5993e925c7ab"
        )
      )
    )

}
