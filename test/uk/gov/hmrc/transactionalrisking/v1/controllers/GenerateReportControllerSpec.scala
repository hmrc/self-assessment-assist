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
import uk.gov.hmrc.transactionalrisking.v1.models.errors.{BadRequestError, MatchingResourcesNotFoundError, UnsupportedVersionError}

import scala.concurrent.ExecutionContext.Implicits.global


class GenerateReportControllerSpec
  extends ControllerBaseSpec
  with MockIntegrationFrameworkService
  with MockEnrolmentsAuthService
  with MockNrsService
  with MockInsightService
  with MockRdsService
  with MockCurrentDateTime
    with MockIdGenerator
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
      idGenerator = mockIdGenerator
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
        MockNrsService.submit(simpleGenerateReportControllerRequestData,simpleGenerateReportControllerNrsID,simpleSubmissionTimestamp,simpleNotableEventType)
        MockNrsService.submit(generateReportRequest = simpleGenerateReportControllerRequestData, generatedNrsId=simpleGenerateReportControllerNrsID,
          submissionTimestamp = simpleSubmissionTimestamp, notableEventType = simpleNotableEventType )
         MockProvideRandomCorrelationId.IdGenerator

        val result = controller.generateReportInternal( simpleNino, simpleCalculationID.toString)(fakeGetRequest)
        status(result) shouldBe OK
        contentAsJson(result) shouldBe simpleAsssementReportMtdJson
        contentType(result) shouldBe Some("application/json")
//        header("X-CorrelationId", result) shouldBe Some(correlationID)

        // Put the nrs save to test here.

      }

    }

    "a valid request is supplied 2" should {
      "return the expected data when controller is called 2" in new Test {


        MockEnrolmentsAuthService.authoriseUser()
        MockIntegrationFrameworkService.getCalculationInfoFail(simpleCalculationID, simpleNino,MatchingResourcesNotFoundError )
//        MockInsightService.assess(simpleFraudRiskRequest)
//        MockRdsService.submit(simpleAssessmentRequestForSelfAssessment, simpleFraudRiskReport, simpleInternalOrigin)
        MockCurrentDateTime.getDateTime()
//        MockNrsService.submit(simpleGenerateReportControllerRequestData, simpleGenerateReportControllerNrsID, simpleSubmissionTimestamp, simpleNotableEventType)
        MockProvideRandomCorrelationId.IdGenerator

        val result = controller.generateReportInternal(simpleNino, simpleCalculationID.toString)(fakeGetRequest)

        status(result) shouldBe NOT_FOUND

        contentAsJson(result) shouldBe MatchingResourcesNotFoundError.toJson

        contentType(result) shouldBe Some("application/json")
        //        header("X-CorrelationId", result) shouldBe Some(correlationID)

        // Put the nrs save to test here.

      }

    }

    "a valid request is supplied 3" should {
      "return the expected data when controller is called 3" in new Test {

        MockEnrolmentsAuthService.authoriseUser()
        MockIntegrationFrameworkService.getCalculationInfo(simpleCalculationID, simpleNino)
        MockInsightService.assessFail(simpleFraudRiskRequest, MatchingResourcesNotFoundError)
        //        MockRdsService.submit(simpleAssessmentRequestForSelfAssessment, simpleFraudRiskReport, simpleInternalOrigin)
        MockCurrentDateTime.getDateTime()
        //        MockNrsService.submit(simpleGenerateReportControllerRequestData, simpleGenerateReportControllerNrsID, simpleSubmissionTimestamp, simpleNotableEventType)
        MockProvideRandomCorrelationId.IdGenerator

        val result = controller.generateReportInternal(simpleNino, simpleCalculationID.toString)(fakeGetRequest)

        status(result) shouldBe NOT_FOUND

        contentAsJson(result) shouldBe MatchingResourcesNotFoundError.toJson

        contentType(result) shouldBe Some("application/json")
        //        header("X-CorrelationId", result) shouldBe Some(correlationID)
        // Put the nrs save to test here.
      }

    }

    "a valid request is supplied 4" should {
      "return the expected data when controller is called 4" in new Test {

        MockEnrolmentsAuthService.authoriseUser()
        MockIntegrationFrameworkService.getCalculationInfo(simpleCalculationID, simpleNino)
        MockInsightService.assess(simpleFraudRiskRequest)
        MockRdsService.submitFail(simpleAssessmentRequestForSelfAssessment, simpleFraudRiskReport, simpleInternalOrigin, MatchingResourcesNotFoundError)
        MockCurrentDateTime.getDateTime()
        //        MockNrsService.submit(simpleGenerateReportControllerRequestData, simpleGenerateReportControllerNrsID, simpleSubmissionTimestamp, simpleNotableEventType)
        MockProvideRandomCorrelationId.IdGenerator

        val result = controller.generateReportInternal(simpleNino, simpleCalculationID.toString)(fakeGetRequest)

        status(result) shouldBe NOT_FOUND

        contentAsJson(result) shouldBe MatchingResourcesNotFoundError.toJson

        contentType(result) shouldBe Some("application/json")
        //        header("X-CorrelationId", result) shouldBe Some(correlationID)
        // Put the nrs save to test here.
      }

    }


  }


}