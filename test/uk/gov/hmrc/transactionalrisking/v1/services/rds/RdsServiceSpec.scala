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

package uk.gov.hmrc.transactionalrisking.v1.services.rds

import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.transactionalrisking.support.{MockAppConfig, ServiceSpec}
import uk.gov.hmrc.transactionalrisking.v1.TestData.CommonTestData._
import uk.gov.hmrc.transactionalrisking.v1.controllers.UserRequest
import uk.gov.hmrc.transactionalrisking.v1.mocks.connectors.MockRdsConnector
import uk.gov.hmrc.transactionalrisking.v1.mocks.services.MockRdsAuthConnector
import uk.gov.hmrc.transactionalrisking.v1.models.auth.{RdsAuthCredentials, UserDetails}
import uk.gov.hmrc.transactionalrisking.v1.models.domain.{AssessmentReport, Internal}
import uk.gov.hmrc.transactionalrisking.v1.models.outcomes.ResponseWrapper
import RdsTestData.{assessmentReport, assessmentRequestForSelfAssessment, fraudRiskReport, rdsRequest}
import uk.gov.hmrc.transactionalrisking.v1.services.ServiceOutcome
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.IdentityDataTestData
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.request.AcknowledgeReportRequest
import uk.gov.hmrc.transactionalrisking.v1.services.rds.RdsService
import uk.gov.hmrc.transactionalrisking.v1.services.rds.models.request.RdsRequest
import uk.gov.hmrc.transactionalrisking.v1.services.rds.models.response.RdsAssessmentReport

import java.util.UUID
import scala.concurrent.Future

class RdsServiceSpec extends ServiceSpec with MockRdsAuthConnector with MockAppConfig {

  var port: Int = _

  class Test extends MockRdsConnector {
    val submitBaseUrl: String = s"http://localhost:$port/submit"
    val acknowledgeUrl: String = s"http://localhost:$port/acknowledge"
    val rdsAuthCredentials: RdsAuthCredentials = RdsAuthCredentials(UUID.randomUUID().toString, "bearer", 3600)

    MockedAppConfig.rdsBaseUrlForSubmit returns submitBaseUrl
    MockedAppConfig.rdsBaseUrlForAcknowledge returns acknowledgeUrl
    MockedAppConfig.rdsAuthRequiredForThisEnv returns false

    implicit val userRequest: UserRequest[_] =
      UserRequest(
        userDetails =
          UserDetails(
            userType = AffinityGroup.Individual,
            agentReferenceNumber = None,
            clientID = "aClientID",
            identityData = Some(IdentityDataTestData.correctModel)
          ),
        request = FakeRequest().withHeaders(
          "Authorization" -> "Bearer aaaa",
          "dummyHeader1" -> "dummyValue1",
          "dummyHeader2" -> "dummyValue2"
        )
      )

    val service = new RdsService(mockRdsAuthConnector, mockRdsConnector, mockAppConfig)
  }


  "service" when {
    "the submit method is called" must {
      "return the expected result" in new Test {
        MockRdsAuthConnector.retrieveAuthorisedBearer()
        MockRdsConnector.submit(rdsRequest) returns Future.successful(Right(ResponseWrapper(correlationId, rdsNewSubmissionReport)))

        val assessmentReportSO: ServiceOutcome[AssessmentReport] = await(service.submit(assessmentRequestForSelfAssessment, fraudRiskReport, Internal))
        assessmentReportSO shouldBe Right(ResponseWrapper(correlationId, assessmentReport))
      }
    }

    "return the expected result in Welsh if it's selected as preferred Language" in new Test {
      MockRdsAuthConnector.retrieveAuthorisedBearer()
      val rdsAssessmentReportSO: ServiceOutcome[RdsAssessmentReport] = Right(ResponseWrapper(correlationId, rdsNewSubmissionReport))
      MockRdsConnector.submit(rdsRequest) returns Future.successful(rdsAssessmentReportSO)

      private val assessmentReportSO: ServiceOutcome[AssessmentReport] = await(service.submit(assessmentRequestForSelfAssessment, fraudRiskReport, Internal))
      assessmentReportSO shouldBe Right(ResponseWrapper(correlationId, assessmentReport))
    }

    "the acknowledged method is called" must {
      "return the expected result" in new Test {
        MockRdsAuthConnector.retrieveAuthorisedBearer()
        val request: RdsRequest = RdsRequest(
          Vector(
            RdsRequest.InputWithString("feedbackId", simpleReportId.toString),
            RdsRequest.InputWithString("nino", simpleNino)
          )
        )

        val expectedResult: ServiceOutcome[RdsAssessmentReport] = Right(ResponseWrapper(correlationId, simpleAcknowledgeNewRdsAssessmentReport))
        MockRdsConnector.acknowledgeRds(request) returns Future.successful(expectedResult)

        val acknowledgeReportRequest: AcknowledgeReportRequest = AcknowledgeReportRequest(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)

        await(service.acknowledge(acknowledgeReportRequest)) shouldBe expectedResult
      }
    }
  }
}
