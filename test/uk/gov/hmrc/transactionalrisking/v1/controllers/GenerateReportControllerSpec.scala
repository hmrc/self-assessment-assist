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


import play.api.libs.json.{JsValue, Json, __}
import play.api.mvc.Result
import uk.gov.hmrc.transactionalrisking.mocks.utils.utils.MockCurrentDateTime
import uk.gov.hmrc.transactionalrisking.utils.DateUtils
import uk.gov.hmrc.transactionalrisking.v1.TestData.CommonTestData._
import uk.gov.hmrc.transactionalrisking.v1.mocks.services._
import uk.gov.hmrc.transactionalrisking.v1.mocks.utils.MockIdGenerator
import uk.gov.hmrc.transactionalrisking.v1.models.errors._
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.IdentityDataTestData
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.request.{Metadata, NrsSubmission, SearchKeys}
import uk.gov.hmrc.transactionalrisking.v1.models.auth.{AuthOutcome, UserDetails}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.http.HeaderCarrier

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class GenerateReportControllerSpec
  extends ControllerBaseSpec
    with MockIntegrationFrameworkService
    with MockEnrolmentsAuthService
    with MockNrsService
    with MockInsightService
    with MockRdsService
    with MockCurrentDateTime
    with MockIdGenerator {

  private val timestamp: OffsetDateTime = OffsetDateTime.parse("2018-04-07T12:13:25.156Z")
  private val formattedDate: String = timestamp.format(DateUtils.isoInstantDatePattern)
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

        MockProvideRandomCorrelationId.IdGenerator
        MockEnrolmentsAuthService.authoriseUser()
        MockIntegrationFrameworkService.getCalculationInfo(simpleCalculationId, simpleNino)
        MockInsightService.assess(simpleFraudRiskRequest)
        MockRdsService.submit(simpleAssessmentRequestForSelfAssessment, simpleFraudRiskReport, simpleInternalOrigin)
        MockCurrentDateTime.getDateTime()
       // MockNrsService.stubAssessmentReport(simpleNRSResponseReportSubmission)
        MockNrsService.stubBuildNrsSubmission(expectedReportPayload)
        MockNrsService.stubNrsSubmit(simpleNRSResponseReportSubmission)

        val result: Future[Result] = controller.generateReportInternal(simpleNino, simpleCalculationId.toString, simpleTaxYear)(fakePostRequest)
        status(result) shouldBe OK
        contentAsJson(result) shouldBe simpleAsssementReportMtdJson
        contentType(result) shouldBe Some("application/json")
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }

      "return the expected data when controller is called even when NRS is unable to non repudiate the event" in new Test {

        MockProvideRandomCorrelationId.IdGenerator
        MockEnrolmentsAuthService.authoriseUser()
        MockIntegrationFrameworkService.getCalculationInfo(simpleCalculationId, simpleNino)
        MockInsightService.assess(simpleFraudRiskRequest)
        MockRdsService.submit(simpleAssessmentRequestForSelfAssessment, simpleFraudRiskReport, simpleInternalOrigin)
        MockCurrentDateTime.getDateTime()
        MockNrsService.stubFailureReportDueToException()
        MockNrsService.stubBuildNrsSubmission(expectedReportPayload)
        MockNrsService.stubNrsSubmit(simpleNRSResponseReportSubmission)

        val result: Future[Result] = controller.generateReportInternal(simpleNino, simpleCalculationId.toString, simpleTaxYear)(fakePostRequest)
        status(result) shouldBe OK
        contentAsJson(result) shouldBe simpleAsssementReportMtdJson
        contentType(result) shouldBe Some("application/json")
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    "a request fails due to an incorrect tax year format" should {
      s"return the tax year format error to indicate the taxYear is invalid" in new Test {

        MockCurrentDateTime.getDateTime()
        MockProvideRandomCorrelationId.IdGenerator
        MockEnrolmentsAuthService.authoriseUser()

        val result: Future[Result] = controller.generateReportInternal(simpleNino, simpleCalculationId.toString, simpleTaxYearInvalid)(fakePostRequest)

        status(result) shouldBe BAD_REQUEST
        Thread.sleep(1000)

        contentAsJson(result) shouldBe TaxYearFormatError.toJson
        contentType(result) shouldBe Some("application/json")
        header("X-CorrelationId", result) shouldBe Some(correlationId)

      }
    }

    "a request fails due to failed authorisedAction that gives a NinoFormatError" should {

      s"return the NinoFormatError error  to indicate that the nino is  invalid. " in new Test {

        MockCurrentDateTime.getDateTime()
        MockProvideRandomCorrelationId.IdGenerator

        val result: Future[Result] = controller.generateReportInternal(simpleNinoInvalid, simpleCalculationId.toString, simpleTaxYear)(fakePostRequest)

        status(result) shouldBe BAD_REQUEST
        Thread.sleep(1000)

        contentAsJson(result) shouldBe NinoFormatError.toJson
        contentType(result) shouldBe Some("application/json")
        header("X-CorrelationId", result) shouldBe Some(correlationId)

      }
    }


    "a request fails due to failed EnrolmentsAuthService.authorised failure" should {

      def runTest(mtdError: MtdError, expectedStatus: Int, expectedBody: JsValue): Unit = {
        s"return the expected error ${mtdError.code} to indicate that the data has not been accepted and saved due to EnrolmentsAuthService.authorised returning an error." in new Test {

          MockEnrolmentsAuthService.authoriseUserFail(mtdError)
          MockCurrentDateTime.getDateTime()
          MockProvideRandomCorrelationId.IdGenerator

          val result: Future[Result] = controller.generateReportInternal(simpleNino, simpleCalculationId.toString, simpleTaxYear)(fakePostRequest)

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

    "a request fails due to a failed InsightService.assess " should {

      def runTest(mtdError: MtdError, expectedStatus: Int, expectedBody: JsValue): Unit = {
        s"return the expected error ${mtdError.code} when controller is set to return error from InsightService.assess " in new Test {

          MockEnrolmentsAuthService.authoriseUser()
          MockIntegrationFrameworkService.getCalculationInfo(simpleCalculationId, simpleNino)
          MockInsightService.assessFail(simpleFraudRiskRequest, mtdError)
          MockCurrentDateTime.getDateTime()
          MockProvideRandomCorrelationId.IdGenerator

          val result: Future[Result] = controller.generateReportInternal(simpleNino, simpleCalculationId.toString, simpleTaxYear)(fakePostRequest)

          status(result) shouldBe expectedStatus
          contentAsJson(result) shouldBe expectedBody
          contentType(result) shouldBe Some("application/json")
          header("X-CorrelationId", result) shouldBe Some(correlationId)
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

          val result: Future[Result] = controller.generateReportInternal(simpleNino, simpleCalculationId.toString, simpleTaxYear)(fakePostRequest)

          status(result) shouldBe expectedStatus
          contentAsJson(result) shouldBe expectedBody
          contentType(result) shouldBe Some("application/json")
          header("X-CorrelationId", result) shouldBe Some(correlationId)
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

    "a request fails due to being unable to construct NRS event" should {
      "return the expected error to indicate that the data has not been accepted or saved due to failed NRSService submit" in new Test {

        MockEnrolmentsAuthService.authoriseUser()
        MockIntegrationFrameworkService.getCalculationInfo(simpleCalculationId, simpleNino)
        MockInsightService.assess(simpleFraudRiskRequest)
        MockRdsService.submit(simpleAssessmentRequestForSelfAssessment, simpleFraudRiskReport, simpleInternalOrigin)
        MockCurrentDateTime.getDateTime()
        MockNrsService.stubUnableToConstrucNrsSubmission()
        MockProvideRandomCorrelationId.IdGenerator
        MockNrsService.stubUnableToConstrucNrsSubmission()
        MockNrsService.stubNrsSubmit(simpleNRSResponseReportSubmission)

        val result: Future[Result] = controller.generateReportInternal(simpleNino, simpleCalculationId.toString, simpleTaxYear)(fakePostRequest)

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsJson(result)  shouldBe DownstreamError.toJson // TODO what should the body be as per the spec?
        contentType(result) shouldBe Some("application/json")
        header("X-CorrelationId", result) shouldBe Some(correlationId)

      }
    }
  }
  private val expectedReportPayload: NrsSubmission =
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