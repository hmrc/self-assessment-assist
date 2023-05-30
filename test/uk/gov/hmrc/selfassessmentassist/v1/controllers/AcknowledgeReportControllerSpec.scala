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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfassessmentassist.api.TestData.CommonTestData
import uk.gov.hmrc.selfassessmentassist.api.TestData.CommonTestData._
import uk.gov.hmrc.selfassessmentassist.api.controllers.ControllerBaseSpec
import uk.gov.hmrc.selfassessmentassist.api.models.errors._
import uk.gov.hmrc.selfassessmentassist.config.AppConfig
import uk.gov.hmrc.selfassessmentassist.mocks.services.MockEnrolmentsAuthService
import uk.gov.hmrc.selfassessmentassist.mocks.utils.MockCurrentDateTime
import uk.gov.hmrc.selfassessmentassist.utils.DateUtils
import uk.gov.hmrc.selfassessmentassist.v1.mocks.connectors.MockLookupConnector
import uk.gov.hmrc.selfassessmentassist.v1.mocks.requestParsers._
import uk.gov.hmrc.selfassessmentassist.v1.mocks.services._
import uk.gov.hmrc.selfassessmentassist.v1.mocks.utils.MockIdGenerator
import uk.gov.hmrc.selfassessmentassist.v1.models.request.nrs.{NrsSubmission, SearchKeys}
import uk.gov.hmrc.selfassessmentassist.v1.models.request.{AcknowledgeReportRawData, nrs}

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AcknowledgeReportControllerSpec
    extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockLookupConnector
    with MockNrsService
    with MockInsightService
    with MockRdsService
    with MockAcknowledgeRequestParser
    with MockCurrentDateTime
    with MockIdGenerator
    with MockIfsService
    with GuiceOneAppPerSuite {

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller: TestController        = new TestController()
    private val timestamp: OffsetDateTime = OffsetDateTime.parse("2018-04-07T12:13:25.156Z")
    private val formattedDate: String     = timestamp.format(DateUtils.isoInstantDateTimePattern)

    private lazy val appConfig = app.injector.instanceOf[AppConfig]

    class TestController
        extends AcknowledgeReportController(
          cc = cc,
          parser = mockAcknowledgeRequestParser,
          authService = mockEnrolmentsAuthService,
          lookupConnector = mockLookupConnector,
          nonRepudiationService = mockNrsService,
          rdsService = mockRdsService,
          currentDateTime = mockCurrentDateTime,
          idGenerator = mockIdGenerator,
          ifsService = mockIfsService,
        )

    val dummyReportPayload: NrsSubmission =
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

  "acknowledgeReportForSelfAssessment" when {

    "a valid request is supplied" should {

      "return 204 to indicate that the data has been accepted and saved and that nothing else needs to be return in the body." in new Test {

        val acknowledgeReportRawData: AcknowledgeReportRawData = AcknowledgeReportRawData(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)

        MockEnrolmentsAuthService.authoriseUser()
        MockLookupConnector.mockMtdIdLookupConnector("1234567890")
        MockAcknowledgeRequestParser.parseRequest(acknowledgeReportRawData)
        MockRdsService.acknowlegeRds(simpleAcknowledgeReportRequest)
        MockCurrentDateTime.getDateTime
        MockNrsService.stubBuildNrsSubmission(dummyReportPayload)
        MockNrsService.stubAcknowledgement(simpleNRSResponseAcknowledgeSubmission)
        MockIfsService.stubAcknowledgeSubmit()

        MockProvideRandomCorrelationId.IdGenerator

        val result: Future[Result] =
          controller.acknowledgeReportForSelfAssessment(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)(fakePostRequest)

        status(result) shouldBe NO_CONTENT
        header("X-CorrelationId", result) shouldBe Some(correlationId)

      }

      "return 204 even when NRS was unable to non repudiate the event" in new Test {

        val acknowledgeReportRawData: AcknowledgeReportRawData = AcknowledgeReportRawData(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)

        MockEnrolmentsAuthService.authoriseUser()
        MockLookupConnector.mockMtdIdLookupConnector("1234567890")
        MockAcknowledgeRequestParser.parseRequest(acknowledgeReportRawData)
        MockRdsService.acknowlegeRds(simpleAcknowledgeReportRequest)
        MockCurrentDateTime.getDateTime
        MockNrsService.stubBuildNrsSubmission(dummyReportPayload)
        MockNrsService.stubFailureAcknowledgementDueToException()
        MockIfsService.stubAcknowledgeSubmit()

        MockProvideRandomCorrelationId.IdGenerator

        val result: Future[Result] =
          controller.acknowledgeReportForSelfAssessment(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)(fakePostRequest)

        status(result) shouldBe NO_CONTENT
        header("X-CorrelationId", result) shouldBe Some(correlationId)

      }
    }

    "a request with invalid Nino format" should {

      s"return the NinoFormatError error  to indicate that the nino is  invalid. " in new Test {

        MockCurrentDateTime.getDateTime
        MockProvideRandomCorrelationId.IdGenerator

        val result: Future[Result] =
          controller.acknowledgeReportForSelfAssessment(simpleNinoInvalid, simpleReportId.toString, simpleRDSCorrelationId)(fakePostRequest)

        status(result) shouldBe BAD_REQUEST

        contentAsJson(result) shouldBe Json.toJson(Seq(NinoFormatError))
        contentType(result) shouldBe Some("application/json")
        header("X-CorrelationId", result) shouldBe Some(correlationId)

      }
    }

    "a request fails due to failed authorisedAction failure" should {

      def runTest(mtdError: MtdError, expectedStatus: Int, expectedBody: JsValue): Unit = {
        s"return the expected error ${mtdError.code} to indicate that the data has not been accepted and saved due to authorisedAction returning an error." in new Test {

          MockLookupConnector.mockMtdIdLookupConnector("1234567890")
          MockEnrolmentsAuthService.authoriseUserFail(mtdError)
          MockCurrentDateTime.getDateTime
          MockProvideRandomCorrelationId.IdGenerator

          val result: Future[Result] =
            controller.acknowledgeReportForSelfAssessment(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)(fakePostRequest)

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

    "a request fails due to failed parseRequest failure" should {

      def runTest(mtdError: MtdError, expectedStatus: Int, expectedBody: JsValue): Unit = {
        s"return the expected error ${mtdError.code} to indicate that the data has not been accepted and saved due to parseRequest returning an error. " in new Test {

          val acknowledgeReportRawData: AcknowledgeReportRawData =
            AcknowledgeReportRawData(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)

          MockEnrolmentsAuthService.authoriseUser()
          MockLookupConnector.mockMtdIdLookupConnector("1234567890")
          MockAcknowledgeRequestParser.parseRequestFail(acknowledgeReportRawData, mtdError)
          MockCurrentDateTime.getDateTime
          MockProvideRandomCorrelationId.IdGenerator

          val result: Future[Result] =
            controller.acknowledgeReportForSelfAssessment(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)(fakePostRequest)

          status(result) shouldBe expectedStatus
          contentAsJson(result) shouldBe Json.toJson(Seq(expectedBody))
          contentType(result) shouldBe Some("application/json")
          header("X-CorrelationId", result) shouldBe Some(correlationId)

        }
      }

      val errorInErrorOut =
        Seq(
          (NinoFormatError, BAD_REQUEST, NinoFormatError.asJson),
          (FormatReportIdError, BAD_REQUEST, FormatReportIdError.asJson)
        )

      errorInErrorOut.foreach(args => (runTest _).tupled(args))
    }

    "a request fails due to RdsService.acknowledge failure" should {

      def runTest(mtdError: MtdError, expectedStatus: Int, expectedBody: JsValue): Unit = {
        s"return the expected error ${mtdError.code} to indicate that the data has not been accepted and saved due to RdsService.acknowledge" in new Test {

          val acknowledgeReportRawData: AcknowledgeReportRawData =
            AcknowledgeReportRawData(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)

          MockEnrolmentsAuthService.authoriseUser()
          MockLookupConnector.mockMtdIdLookupConnector("1234567890")
          MockAcknowledgeRequestParser.parseRequest(acknowledgeReportRawData)
          MockRdsService.acknowlegeRdsFail(simpleAcknowledgeReportRequest, mtdError)
          MockCurrentDateTime.getDateTime

          MockProvideRandomCorrelationId.IdGenerator

          val result: Future[Result] =
            controller.acknowledgeReportForSelfAssessment(simpleNino, simpleCalculationId.toString, simpleRDSCorrelationId)(fakePostRequest)

          status(result) shouldBe expectedStatus
          contentAsJson(result) shouldBe Json.toJson(Seq(expectedBody))
          contentType(result) shouldBe Some("application/json")
          header("X-CorrelationId", result) shouldBe Some(correlationId)

        }
      }

      val errorInErrorOut =
        Seq(
          (ServerError, INTERNAL_SERVER_ERROR, InternalError.asJson),
          (ForbiddenDownstreamError, FORBIDDEN, ForbiddenDownstreamError.asJson),
          (ForbiddenRDSCorrelationIdError, FORBIDDEN, ForbiddenRDSCorrelationIdError.asJson),
          (InternalError, INTERNAL_SERVER_ERROR, InternalError.asJson),
          (MatchingResourcesNotFoundError, SERVICE_UNAVAILABLE, ServiceUnavailableError.asJson),
          (ClientOrAgentNotAuthorisedError, FORBIDDEN, ClientOrAgentNotAuthorisedError.asJson),
          (InvalidBodyTypeError, SERVICE_UNAVAILABLE, InternalError.asJson)
        )

      errorInErrorOut.foreach(args => (runTest _).tupled(args))
    }

    "a request that is unable to construct NRS event" should {
      "return the expected error to indicate that the data has not been accepted or saved due to event generation failure" in new Test {

        val acknowledgeReportRawData: AcknowledgeReportRawData = AcknowledgeReportRawData(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)

        MockEnrolmentsAuthService.authoriseUser()
        MockLookupConnector.mockMtdIdLookupConnector("1234567890")
        MockAcknowledgeRequestParser.parseRequest(acknowledgeReportRawData)
        MockRdsService.acknowlegeRds(simpleAcknowledgeReportRequest)
        MockCurrentDateTime.getDateTime
        MockProvideRandomCorrelationId.IdGenerator
        MockNrsService.stubUnableToConstrucNrsSubmission()
        MockIfsService.stubAcknowledgeSubmit()

        val result: Future[Result] =
          controller.acknowledgeReportForSelfAssessment(simpleNino, simpleCalculationId.toString, simpleRDSCorrelationId)(fakePostRequest)

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsJson(result) shouldBe Json.toJson(Seq(InternalError.asJson))
        contentType(result) shouldBe Some("application/json")
        header("X-CorrelationId", result) shouldBe Some(correlationId)

      }
    }
  }

}
