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

package uk.gov.hmrc.transactionalrisking.v1.service.rds

import com.codahale.metrics.SharedMetricRegistries
import org.scalatest.BeforeAndAfterAll
import play.api.test.FakeRequest
import uk.gov.hmrc.transactionalrisking.controllers.UserRequest
import uk.gov.hmrc.transactionalrisking.models.auth.{RdsAuthCredentials, UserDetails}
import uk.gov.hmrc.transactionalrisking.models.domain.{AssessmentReport, Internal}
import uk.gov.hmrc.transactionalrisking.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.services.ServiceOutcome
import uk.gov.hmrc.transactionalrisking.services.nrs.models.request.AcknowledgeReportRequest
import uk.gov.hmrc.transactionalrisking.services.rds.{RdsConnector, RdsService}
import uk.gov.hmrc.transactionalrisking.services.rds.models.request.RdsRequest
import uk.gov.hmrc.transactionalrisking.services.rds.models.response.NewRdsAssessmentReport
import uk.gov.hmrc.transactionalrisking.support.{MockAppConfig, ServiceSpec}
import uk.gov.hmrc.transactionalrisking.v1.TestData.CommonTestData.commonTestData.{internalCorrelationID, rdsNewSubmissionReport, simpleAcknowledgeNewRdsAssessmentReport, simpleNino, simpleRDSCorrelationID, simpleReportID}
import uk.gov.hmrc.transactionalrisking.v1.mocks.connectors.MockRdsConnector
import uk.gov.hmrc.transactionalrisking.v1.mocks.services.MockRdsAuthConnector
import uk.gov.hmrc.transactionalrisking.v1.service.rds.RdsTestData.{assessmentReport, assessmentRequestForSelfAssessment, fraudRiskReport, rdsRequest}
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.IdentityDataTestData

import java.util.UUID
import scala.concurrent.Future

class RdsServiceSpec extends ServiceSpec with MockRdsAuthConnector with MockAppConfig {
  var port: Int = _


  class Test extends MockRdsConnector {
    val submitBaseUrl:String = s"http://localhost:$port/submit"
    val acknowledgeUrl:String = s"http://localhost:$port/acknowledge"
    val rdsAuthCredentials = RdsAuthCredentials(UUID.randomUUID().toString, "bearer", 3600)

    MockedAppConfig.rdsBaseUrlForSubmit returns submitBaseUrl
    MockedAppConfig.rdsBaseUrlForAcknowledge returns acknowledgeUrl
    MockedAppConfig.rdsAuthRequiredForThisEnv returns false
//    val connector = new RdsConnector(httpClient, mockAppConfig)

    implicit val userRequest: UserRequest[_] =
      UserRequest(
        userDetails =
          UserDetails(
            userType = "Individual",
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

    val service = new RdsService(mockRdsAuthConnector,mockRdsConnector,mockAppConfig)
  }



  "service" when {
    "the submit method is called" must {
      "return the expected result" in new Test {
        MockRdsAuthConnector.retrieveAuthorisedBearer()
        MockRdsConnector.submit(rdsRequest) returns Future.successful(Right(ResponseWrapper(internalCorrelationID, rdsNewSubmissionReport)))

        val assessmentReportSO:ServiceOutcome[AssessmentReport] = await(service.submit( assessmentRequestForSelfAssessment, fraudRiskReport, Internal))
        assessmentReportSO shouldBe Right(ResponseWrapper(internalCorrelationID,  assessmentReport))
      }
    }

    "return the expected result in Welsh if it's selected as preferred Language" in new Test {
      MockRdsAuthConnector.retrieveAuthorisedBearer()
      val rdsAssessmentReportSO: ServiceOutcome[NewRdsAssessmentReport] = Right(ResponseWrapper(internalCorrelationID, rdsNewSubmissionReport))
      MockRdsConnector.submit(rdsRequest) returns Future.successful(rdsAssessmentReportSO)

      private val assementReportSO: ServiceOutcome[AssessmentReport] = await(service.submit(assessmentRequestForSelfAssessment, fraudRiskReport, Internal))
      assementReportSO shouldBe Right(ResponseWrapper(internalCorrelationID, assessmentReport))
    }

    "the acknowledged method is called" must {
      "return the expected result" in new Test {
        MockRdsAuthConnector.retrieveAuthorisedBearer()
        val request: RdsRequest = RdsRequest(
          Vector(
            RdsRequest.InputWithString("feedbackID", simpleReportID.toString),
            RdsRequest.InputWithString("nino", simpleNino)
          )
        )

        val expectedResult:ServiceOutcome[NewRdsAssessmentReport] = Right( ResponseWrapper( internalCorrelationID, simpleAcknowledgeNewRdsAssessmentReport)  )
        MockRdsConnector.acknowledgeRds(request) returns Future.successful(expectedResult)

        val acknowledgeReportRequest: AcknowledgeReportRequest =  AcknowledgeReportRequest(simpleNino, simpleReportID.toString, simpleRDSCorrelationID)

        await(service.acknowledge(acknowledgeReportRequest)) shouldBe expectedResult
      }
    }
  }
}