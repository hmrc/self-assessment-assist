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

package uk.gov.hmrc.transactionalrisking.v1.service.ifs

import uk.gov.hmrc.transactionalrisking.mocks.utils.MockCurrentDateTime
import uk.gov.hmrc.transactionalrisking.support.ServiceSpec
import uk.gov.hmrc.transactionalrisking.utils.DateUtils
import uk.gov.hmrc.transactionalrisking.v1.TestData.CommonTestData
import uk.gov.hmrc.transactionalrisking.v1.models.domain.{AssessmentReport, Link, Risk}
import uk.gov.hmrc.transactionalrisking.v1.models.errors.{DownstreamError, ErrorWrapper}
import uk.gov.hmrc.transactionalrisking.v1.services.ifs.IfsService
import uk.gov.hmrc.transactionalrisking.v1.services.ifs.models.request.{IFRequest, Messages}
import uk.gov.hmrc.transactionalrisking.v1.services.ifs.models.response.IfsResponse
import uk.gov.hmrc.transactionalrisking.v1.services.rds.RdsTestData.assessmentRequestForSelfAssessment

import java.util.UUID
import scala.concurrent.Future

class IfsServiceSpec extends ServiceSpec with MockCurrentDateTime {

  private val rdsReport: AssessmentReport = AssessmentReport(
    reportId = UUID.fromString("db741dff-4054-478e-88d2-5993e925c7ab"),
    risks = Seq(
      Risk(
        title = "Turnover and cost of sales",
        body = "Your cost of sales is greater than income",
        action = "Please read our guidance",
        links = Seq(Link(title = "Our guidance", url = "https://www.gov.uk/expenses-if-youre-self-employed")),
        path = "general/total_declared_turnover"
      )
    ),
    nino = "nino",
    taxYear = "2021-2022",
    calculationId = UUID.fromString("99d758f6-c4be-4339-804e-f79cf0610d4f"),
    rdsCorrelationId = "e43264c5-5301-4ece-b3d3-1e8a8dd93b4b"
  )

  private def expectedReportGenerationPayload =
    IFRequest(
      serviceRegime = "self-assessment-assist",
      eventName = "GenerateReport",
      eventTimestamp = mockCurrentDateTime.getDateTime(),
      feedbackId = CommonTestData.simpleAcknowledgeNewRdsAssessmentReport.feedbackId.get.toString,
      metadata = List(
        Map("nino" -> rdsReport.nino),
        Map("taxYear" -> rdsReport.taxYear),
        Map("calculationId" -> rdsReport.calculationId.toString),
        Map("customerType" -> "Individual"),
        Map("calculationTimestamp" -> mockCurrentDateTime.getDateTime().toLocalDateTime.format(DateUtils.dateTimePattern)),
      ),
      payload = Some(Messages(Some(Vector())))
    )

  private def expectedAcknowledgementPayload =
    IFRequest(
      serviceRegime = "self-assessment-assist",
      eventName = "AcknowledgeReport",
      eventTimestamp = mockCurrentDateTime.getDateTime(),
      feedbackId = CommonTestData.simpleAcknowledgeNewRdsAssessmentReport.feedbackId.get.toString,
      metadata = List(
        Map("nino" -> CommonTestData.simpleAcknowledgeReportRequest.nino),
        Map("customerType" -> "Individual"),
      ),
      payload = None
    )

  class Test extends MockIfsConnector {
    val service = new IfsService(mockIfsConnector, mockCurrentDateTime)
  }

  "service using acknowledged generated" when {

    "submitGenerateReportMessage" must {

      "return the expected result" in new Test {

        MockCurrentDateTime.getDateTime()
        MockIfsConnector.submit(expectedPayload = expectedReportGenerationPayload)
          .returns(Future.successful(Right(IfsResponse())))

        await(
          service.submitGenerateReportMessage(rdsReport, mockCurrentDateTime.getDateTime().toLocalDateTime, assessmentRequestForSelfAssessment, CommonTestData.simpleAcknowledgeNewRdsAssessmentReport)
        ) shouldBe Right(IfsResponse())
      }

    }

    "service call unsuccessful" must {

      "return the expected result" in new Test {

        MockCurrentDateTime.getDateTime()
        MockIfsConnector.submit(expectedPayload = expectedReportGenerationPayload)
          .returns(Future.successful(Left(ErrorWrapper(rdsReport.rdsCorrelationId, DownstreamError))))

        await(
          service.submitGenerateReportMessage(rdsReport, mockCurrentDateTime.getDateTime().toLocalDateTime, assessmentRequestForSelfAssessment, CommonTestData.simpleAcknowledgeNewRdsAssessmentReport)
        ) shouldBe Left(ErrorWrapper(rdsReport.rdsCorrelationId, DownstreamError))
      }

    }

    "submitAcknowledgementMessage" must {

      "return the expected result" in new Test {

        MockCurrentDateTime.getDateTime()
        MockIfsConnector.submit(expectedPayload = expectedAcknowledgementPayload)
          .returns(Future.successful(Right(IfsResponse())))

        await(
          service.submitAcknowledgementMessage(CommonTestData.simpleAcknowledgeReportRequest, CommonTestData.simpleAcknowledgeNewRdsAssessmentReport, CommonTestData.simpleIndividualUserDetails)
        ) shouldBe Right(IfsResponse())
      }
    }
  }
}
