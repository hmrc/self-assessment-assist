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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transactionalrisking.mocks.utils.MockCurrentDateTime
import uk.gov.hmrc.transactionalrisking.utils.DateUtils
import uk.gov.hmrc.transactionalrisking.v1.TestData.CommonTestData._
import uk.gov.hmrc.transactionalrisking.v1.mocks.connectors.MockLookupConnector
import uk.gov.hmrc.transactionalrisking.v1.mocks.requestParsers._
import uk.gov.hmrc.transactionalrisking.v1.mocks.services._
import uk.gov.hmrc.transactionalrisking.v1.mocks.utils.MockIdGenerator
import uk.gov.hmrc.transactionalrisking.v1.models.errors._
import uk.gov.hmrc.transactionalrisking.v1.models.request.AcknowledgeReportRawData
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.IdentityDataTestData
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.request.{Metadata, NrsSubmission, SearchKeys}

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
    with MockIdGenerator {


  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller: TestController = new TestController()
    private val timestamp: OffsetDateTime = OffsetDateTime.parse("2018-04-07T12:13:25.156Z")
    private val formattedDate: String = timestamp.format(DateUtils.isoInstantDatePattern)

    class TestController extends AcknowledgeReportController(
      cc = cc,
      requestParser = mockAcknowledgeRequestParser,
      authService = mockEnrolmentsAuthService,
      lookupConnector = mockLookupConnector,
      nonRepudiationService = mockNrsService,
      rdsService = mockRdsService,
      currentDateTime = mockCurrentDateTime,
      idGenerator = mockIdGenerator
    )

     val dummyReportPayload: NrsSubmission =
      NrsSubmission(
        payload = "eyJyZXBvcnRJZCI6ImRiNzQxZGZmLTQwNTQtNDc4ZS04OGQyLTU5OTNlOTI1YzdhYiIsIm1lc3NhZ2VzIjpbeyJ0aXRsZSI6IlR1cm5vdmVyIGFuZCBjb3N0IG9mIHNhbGVzIiwiYm9keSI6IllvdXIgY29zdCBvZiBzYWxlcyBpcyBncmVhdGVyIHRoYW4gaW5jb21lIiwiYWN0aW9uIjoiUGxlYXNlIHJlYWQgb3VyIGd1aWRhbmNlIiwibGlua3MiOlt7InRpdGxlIjoiT3VyIGd1aWRhbmNlIiwidXJsIjoiaHR0cHM6Ly93d3cuZ292LnVrL2V4cGVuc2VzLWlmLXlvdXJlLXNlbGYtZW1wbG95ZWQifV0sInBhdGgiOiJnZW5lcmFsL3RvdGFsX2RlY2xhcmVkX3R1cm5vdmVyIn1dLCJuaW5vIjoibmlubyIsInRheFllYXIiOiIyMDIxLTIwMjIiLCJjYWxjdWxhdGlvbklkIjoiOTlkNzU4ZjYtYzRiZS00MzM5LTgwNGUtZjc5Y2YwNjEwZDRmIiwiY29ycmVsYXRpb25JZCI6ImU0MzI2NGM1LTUzMDEtNGVjZS1iM2QzLTFlOGE4ZGQ5M2I0YiJ9",
        metadata = Metadata(
          businessId = "saa",
          notableEvent = "saa-report-generated",
          payloadContentType = "application/json",
          payloadSha256Checksum = "acdf5c0add9e434375e81797ad21fd409bc55f6d4f264d7aa302ca1ef4a01058",
          userSubmissionTimestamp = formattedDate,
          identityData = Some(IdentityDataTestData.correctModel),
          userAuthToken = "Bearer aaaa",
          headerData = Json.toJson(Map(
            "Host" -> "localhost",
            "dummyHeader1" -> "dummyValue1",
            "dummyHeader2" -> "dummyValue2",
            "Authorization" -> "Bearer aaaa"
          )),
          searchKeys =
            SearchKeys(
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
        MockCurrentDateTime.getDateTime()
        MockNrsService.stubBuildNrsSubmission(dummyReportPayload)
        MockNrsService.stubAcknowledgement(simpleNRSResponseAcknowledgeSubmission)

        MockProvideRandomCorrelationId.IdGenerator

        val result: Future[Result] = controller.acknowledgeReportForSelfAssessment(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)(fakePostRequest)

        status(result) shouldBe NO_CONTENT
        header("X-CorrelationId", result) shouldBe Some(correlationId)

      }

      "return 204 even when NRS was unable to non repudiate the event" in new Test {

        val acknowledgeReportRawData: AcknowledgeReportRawData = AcknowledgeReportRawData(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)

        MockEnrolmentsAuthService.authoriseUser()
        MockLookupConnector.mockMtdIdLookupConnector("1234567890")
        MockAcknowledgeRequestParser.parseRequest(acknowledgeReportRawData)
        MockRdsService.acknowlegeRds(simpleAcknowledgeReportRequest)
        MockCurrentDateTime.getDateTime()
        MockNrsService.stubBuildNrsSubmission(dummyReportPayload)
        MockNrsService.stubFailureAcknowledgementDueToException()


        MockProvideRandomCorrelationId.IdGenerator

        val result: Future[Result] = controller.acknowledgeReportForSelfAssessment(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)(fakePostRequest)

        status(result) shouldBe NO_CONTENT
        header("X-CorrelationId", result) shouldBe Some(correlationId)

      }
    }


    "a request with invalid Nino format" should {

      s"return the NinoFormatError error  to indicate that the nino is  invalid. " in new Test {

        MockCurrentDateTime.getDateTime()
        MockProvideRandomCorrelationId.IdGenerator

        val result: Future[Result] = controller.acknowledgeReportForSelfAssessment(simpleNinoInvalid, simpleReportId.toString, simpleRDSCorrelationId)(fakePostRequest)

        status(result) shouldBe BAD_REQUEST

        contentAsJson(result) shouldBe NinoFormatError.toJson
        contentType(result) shouldBe Some("application/json")
        header("X-CorrelationId", result) shouldBe Some(correlationId)

      }
    }

    "a request fails due to failed authorisedAction failure" should {

      def runTest(mtdError: MtdError, expectedStatus: Int, expectedBody: JsValue): Unit = {
        s"return the expected error ${mtdError.code} to indicate that the data has not been accepted and saved due to authorisedAction returning an error." in new Test {

          MockLookupConnector.mockMtdIdLookupConnector("1234567890")
          MockEnrolmentsAuthService.authoriseUserFail(mtdError)
          MockCurrentDateTime.getDateTime()
          MockProvideRandomCorrelationId.IdGenerator

          val result: Future[Result] = controller.acknowledgeReportForSelfAssessment(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)(fakePostRequest)

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
          MockLookupConnector.mockMtdIdLookupConnector("1234567890")
          MockAcknowledgeRequestParser.parseRequestFail(acknowledgeReportRawData, mtdError)
          MockCurrentDateTime.getDateTime()
          MockProvideRandomCorrelationId.IdGenerator

          val result: Future[Result] = controller.acknowledgeReportForSelfAssessment(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)(fakePostRequest)

          status(result) shouldBe expectedStatus
          contentAsJson(result) shouldBe expectedBody
          contentType(result) shouldBe Some("application/json")
          header("X-CorrelationId", result) shouldBe Some(correlationId)

        }
      }

      val errorInErrorOut =
        Seq(
          (NinoFormatError, BAD_REQUEST, NinoFormatError.toJson),
          (FormatReportIdError, BAD_REQUEST, FormatReportIdError.toJson),
        )

      errorInErrorOut.foreach(args => (runTest _).tupled(args))
    }


//TODO revist, what is the purpose of this test, and the way these test are written doesn't indicate clear purpose of test
    "a request fails due to RdsService.acknowledge failure" should {

      def runTest(mtdError: MtdError, expectedStatus: Int, expectedBody: JsValue): Unit = {
        s"return the expected error ${mtdError.code} to indicate that the data has not been accepted and saved due to RdsService.acknowledge" in new Test {

          val acknowledgeReportRawData: AcknowledgeReportRawData = AcknowledgeReportRawData(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)

          MockEnrolmentsAuthService.authoriseUser()
          MockLookupConnector.mockMtdIdLookupConnector("1234567890")
          MockAcknowledgeRequestParser.parseRequest(acknowledgeReportRawData)
          MockRdsService.acknowlegeRdsFail(simpleAcknowledgeReportRequest, mtdError)
          MockCurrentDateTime.getDateTime()

          MockProvideRandomCorrelationId.IdGenerator

          val result: Future[Result] = controller.acknowledgeReportForSelfAssessment(simpleNino, simpleCalculationId.toString, simpleRDSCorrelationId)(fakePostRequest)

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
          (MatchingResourcesNotFoundError, SERVICE_UNAVAILABLE, ServiceUnavailableError.toJson),
          (ClientOrAgentNotAuthorisedError, FORBIDDEN, ClientOrAgentNotAuthorisedError.toJson)
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
          MockCurrentDateTime.getDateTime()
          MockProvideRandomCorrelationId.IdGenerator
          MockNrsService.stubUnableToConstrucNrsSubmission()

          val result: Future[Result] = controller.acknowledgeReportForSelfAssessment(simpleNino, simpleCalculationId.toString, simpleRDSCorrelationId)(fakePostRequest)


          status(result) shouldBe INTERNAL_SERVER_ERROR
          contentAsJson(result)  shouldBe DownstreamError.toJson
          contentType(result) shouldBe Some("application/json")
          header("X-CorrelationId", result) shouldBe Some(correlationId)

        }
      }
  }
}