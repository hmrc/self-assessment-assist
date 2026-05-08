/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.selfassessmentassist.v1.services

import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.selfassessmentassist.api.TestData.CommonTestData.*
import uk.gov.hmrc.selfassessmentassist.api.controllers.UserRequest
import uk.gov.hmrc.selfassessmentassist.api.models.auth.UserDetails
import uk.gov.hmrc.selfassessmentassist.api.models.errors.*
import uk.gov.hmrc.selfassessmentassist.api.models.outcomes.ResponseWrapper
import uk.gov.hmrc.selfassessmentassist.support.{MockAppConfig, ServiceSpec}
import uk.gov.hmrc.selfassessmentassist.v1.mocks.connectors.MockRdsConnector
import uk.gov.hmrc.selfassessmentassist.v1.mocks.services.MockRdsAuthConnector
import uk.gov.hmrc.selfassessmentassist.v1.models.domain.AssessmentReportWrapper
import uk.gov.hmrc.selfassessmentassist.v1.models.response.rds.RdsAssessmentReport
import uk.gov.hmrc.selfassessmentassist.v1.models.response.rds.RdsAssessmentReport.*
import uk.gov.hmrc.selfassessmentassist.v1.services.testData.RdsTestData.*

import scala.concurrent.Future

class RdsServiceSpec extends ServiceSpec with MockRdsAuthConnector with MockAppConfig {

  private def removeOutputNamed(report: RdsAssessmentReport, names: Set[String]): RdsAssessmentReport =
    report.copy(
      outputs = report.outputs.filterNot {
        case KeyValueWrapper(name, _)    => names(name)
        case KeyValueWrapperInt(name, _) => names(name)
        case MainOutputWrapper(name, _)  => names(name)
        case _                           => false
      }
    )

  class Test(rdsRequired: Boolean = false) extends MockRdsConnector {
    val submitBaseUrl: String  = "http://localhost:8343/submit"
    val acknowledgeUrl: String = "http://localhost:8343/acknowledge"

    MockedAppConfig.rdsBaseUrlForSubmit returns submitBaseUrl
    MockedAppConfig.rdsBaseUrlForAcknowledge returns acknowledgeUrl
    MockedAppConfig.rdsAuthRequiredForThisEnv returns rdsRequired

    implicit val userRequest: UserRequest[?] =
      UserRequest(
        userDetails = UserDetails(
          userType = AffinityGroup.Individual,
          agentReferenceNumber = None,
          clientID = "aClientID",
          identityData = Some(identityCorrectModel)
        ),
        request = FakeRequest().withHeaders(
          "Authorization" -> "Bearer aaaa",
          "dummyHeader1"  -> "dummyValue1",
          "dummyHeader2"  -> "dummyValue2"
        )
      )

    val service = new RdsService(mockRdsAuthConnector, mockRdsConnector, mockAppConfig)
  }

  "RdsService" when {
    ".submit" should {
      "return the expected result when RDS auth is not required" in new Test {
        MockRdsConnector
          .submit(rdsRequest)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, rdsNewSubmissionReport))))

        val result: ServiceOutcome[AssessmentReportWrapper] = await(service.submit(assessmentRequestForSelfAssessment, fraudRiskReport))

        result shouldBe Right(ResponseWrapper(correlationId, assessmentReportWrapper))
      }

      "return the expected result when RDS auth is required" in new Test(rdsRequired = true) {
        MockRdsAuthConnector.retrieveAuthorisedBearer()
        MockRdsConnector
          .submit(rdsRequest)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, rdsNewSubmissionReport))))

        val result: ServiceOutcome[AssessmentReportWrapper] = await(service.submit(assessmentRequestForSelfAssessment, fraudRiskReport))

        result shouldBe Right(ResponseWrapper(correlationId, assessmentReportWrapper))
      }

      "return the expected error result when rdsConnector fails" in new Test(rdsRequired = true) {
        MockRdsAuthConnector.retrieveAuthorisedBearer()
        MockRdsConnector
          .submit(rdsRequest)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, NinoFormatError))))

        val result: ServiceOutcome[AssessmentReportWrapper] = await(service.submit(assessmentRequestForSelfAssessment, fraudRiskReport))

        result shouldBe Left(ErrorWrapper(correlationId, NinoFormatError))
      }

      "return the expected error result when RDS auth is required and auth retrieval fails" in new Test(rdsRequired = true) {
        MockRdsAuthConnector.retrieveAuthorisedBearer(Left(NinoFormatError))

        val result: ServiceOutcome[AssessmentReportWrapper] = await(service.submit(assessmentRequestForSelfAssessment, fraudRiskReport))

        result shouldBe Left(ErrorWrapper(correlationId, NinoFormatError))
      }

      "return InternalError when rdsCorrelationId is missing" in new Test {
        MockRdsConnector
          .submit(rdsRequest)
          .returns(
            Future.successful(Right(ResponseWrapper(correlationId, removeOutputNamed(rdsNewSubmissionReport, Set("correlationId")))))
          )

        val result: ServiceOutcome[AssessmentReportWrapper] = await(service.submit(assessmentRequestForSelfAssessment, fraudRiskReport))

        result shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }

      "return the expected error result when RDS returns NoAssessmentFeedbackFromRDS" in new Test {
        MockRdsConnector
          .submit(rdsRequest)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, NoAssessmentFeedbackFromRDS))))

        val result: ServiceOutcome[AssessmentReportWrapper] = await(service.submit(assessmentRequestForSelfAssessment, fraudRiskReport))

        result shouldBe Left(ErrorWrapper(correlationId, NoAssessmentFeedbackFromRDS))
      }

      "return the expected error result when RDS returns MatchingCalculationIDNotFoundError" in new Test {
        MockRdsConnector
          .submit(rdsRequest)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, MatchingCalculationIDNotFoundError))))

        val result: ServiceOutcome[AssessmentReportWrapper] = await(service.submit(assessmentRequestForSelfAssessment, fraudRiskReport))

        result shouldBe Left(ErrorWrapper(correlationId, MatchingCalculationIDNotFoundError))
      }

      "return InternalError when calculationId/feedbackId/timestamp are missing in RDS response" in new Test {
        MockRdsConnector
          .submit(rdsRequest)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, rdsNewSubmissionReport.copy(outputs = Seq.empty)))))

        val result: ServiceOutcome[AssessmentReportWrapper] = await(service.submit(assessmentRequestForSelfAssessment, fraudRiskReport))

        result shouldBe Left(ErrorWrapper(correlationId, InternalError))
      }
    }

    ".acknowledgeRds" should {
      val expectedResult: ServiceOutcome[RdsAssessmentReport] = Right(ResponseWrapper(correlationId, simpleAcknowledgeNewRdsAssessmentReport))

      "return the expected result when RDS auth is not required" in new Test {
        MockRdsConnector
          .acknowledgeRds(rdsAcknowledgementRequest)
          .returns(Future.successful(expectedResult))

        val result: ServiceOutcome[RdsAssessmentReport] = await(service.acknowledge(acknowledgeReportRequest))

        result shouldBe expectedResult
      }

      "return the expected result when RDS auth is required" in new Test(rdsRequired = true) {
        MockRdsAuthConnector.retrieveAuthorisedBearer()
        MockRdsConnector
          .acknowledgeRds(rdsAcknowledgementRequest)
          .returns(Future.successful(expectedResult))

        val result: ServiceOutcome[RdsAssessmentReport] = await(service.acknowledge(acknowledgeReportRequest))

        result shouldBe expectedResult
      }

      "return the expected error result when RDS auth is required and auth retrieval fails" in new Test(rdsRequired = true) {
        MockRdsAuthConnector.retrieveAuthorisedBearer(Left(NinoFormatError))

        val result: ServiceOutcome[RdsAssessmentReport] = await(service.acknowledge(acknowledgeReportRequest))

        result shouldBe Left(ErrorWrapper(correlationId, NinoFormatError))
      }
    }
  }

}
