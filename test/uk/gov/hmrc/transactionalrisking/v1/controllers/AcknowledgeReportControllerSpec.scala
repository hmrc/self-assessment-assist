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

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transactionalrisking.controllers.{AcknowledgeReportController, ControllerBaseSpec}
import uk.gov.hmrc.transactionalrisking.mocks.utils.MockCurrentDateTime
import uk.gov.hmrc.transactionalrisking.models.request.AcknowledgeReportRawData
import uk.gov.hmrc.transactionalrisking.v1.TestData.CommonTestData.commonTestData._
import uk.gov.hmrc.transactionalrisking.v1.mocks.requestParsers._
import uk.gov.hmrc.transactionalrisking.v1.mocks.services._
import uk.gov.hmrc.transactionalrisking.v1.mocks.utils.MockIdGenerator

import scala.concurrent.ExecutionContext.Implicits.global

class AcknowledgeReportControllerSpec
  extends ControllerBaseSpec
  with MockEnrolmentsAuthService
  with MockNrsService
  with MockInsightService
  with MockRdsService
  with MockAcknowledgeRequestParser
  with MockCurrentDateTime
  with MockIdGenerator
   {


  implicit val correlationId: String = "X-ID"

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    val controller:TestController = new TestController()

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
      "return 204 to indicate that the data has been accepted and saved and that there is nothing else needed to return." in new Test {

        val acknowledgeReportRawData:AcknowledgeReportRawData = AcknowledgeReportRawData(simpleNino, simpleReportID.toString, simpleRDSCorrelationID)

        MockEnrolmentsAuthService.authoriseUser()
        MockAcknowledgeRequestParser.parseRequest(acknowledgeReportRawData)
        MockRdsService.acknowlegeRds(simpeAcknowledgeReportRequest)
        MockCurrentDateTime.getDateTime()
        MockNrsService.submit_Acknowledge(generateReportRequest = simpleAcknowledgeReportRequest, generatedNrsId=simpleAcknowledgeNrsID,
          submissionTimestamp = simpleSubmissionTimestamp, notableEventType = simpeNotableEventType )

        MockProvideRandomCorrelationId.IdGenerator

        val result = controller.acknowledgeReportForSelfAssessment( simpleNino, simpleCalculationID.toString, simpleRDSCorrelationID)(fakeGetRequest)
        val retHttpResult = status( result )
        retHttpResult shouldBe NO_CONTENT

        val ct = contentType(result)
        ct shouldBe None

        val xcorrelationId = header("X-CorrelationId", result)
        xcorrelationId shouldBe Some(internalCorrelationID)

      }

    }
  }

}