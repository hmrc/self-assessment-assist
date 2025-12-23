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

package uk.gov.hmrc.selfassessmentassist.v1.services

import uk.gov.hmrc.selfassessmentassist.api.TestData.CommonTestData
import uk.gov.hmrc.selfassessmentassist.api.TestData.CommonTestData.simpleTaxYear
import uk.gov.hmrc.selfassessmentassist.api.models.domain.CustomerType
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{ErrorWrapper, InternalError}
import uk.gov.hmrc.selfassessmentassist.mocks.utils.MockCurrentDateTime
import uk.gov.hmrc.selfassessmentassist.support.ServiceSpec
import uk.gov.hmrc.selfassessmentassist.v1.connectors.MockIfsConnector
import uk.gov.hmrc.selfassessmentassist.v1.models.domain.{AssessmentReport, AssessmentReportWrapper, Link, Risk}
import uk.gov.hmrc.selfassessmentassist.v1.models.request.ifs.*
import uk.gov.hmrc.selfassessmentassist.v1.models.response.ifs.IfsResponse
import uk.gov.hmrc.selfassessmentassist.v1.models.response.rds
import uk.gov.hmrc.selfassessmentassist.v1.models.response.rds.RdsAssessmentReport
import uk.gov.hmrc.selfassessmentassist.v1.models.response.rds.RdsAssessmentReport.{KeyValueWrapper, KeyValueWrapperInt, Output}
import uk.gov.hmrc.selfassessmentassist.v1.services.testData.RdsTestData.assessmentRequestForSelfAssessment

import java.util.UUID
import scala.concurrent.Future

class IfsServiceSpec extends ServiceSpec with MockCurrentDateTime {

  private val rdsReport: AssessmentReport = AssessmentReport(
    reportId = UUID.fromString("b50324cb-e5bd-4be1-88b1-4af1757ae4c7"),
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
    taxYear = simpleTaxYear,
    calculationId = UUID.fromString("99d758f6-c4be-4339-804e-f79cf0610d4f"),
    rdsCorrelationId = "e43264c5-5301-4ece-b3d3-1e8a8dd93b4b"
  )

  val calculationId: UUID          = UUID.randomUUID()
  val correlationsId: String       = "some-correlationId"
  val feedbackId: UUID             = UUID.randomUUID()
  val responseCode: Int            = 211
  val responseMessage: String      = "some-message"
  val calculationTimeStamp: String = "2022-01-01T12:00:00.000Z"

  val outputs: Seq[Output] = Seq(
    KeyValueWrapper("calculationId", Some(calculationId.toString)),
    KeyValueWrapper("correlationId", Some(correlationsId)),
    KeyValueWrapper("feedbackId", Some(feedbackId.toString)),
    KeyValueWrapperInt("responseCode", responseCode),
    KeyValueWrapper("responseMessage", Some(responseMessage)),
    KeyValueWrapper("calculationTimestamp", Some(calculationTimeStamp))
  )

  val rdsAssessmentReport: RdsAssessmentReport = rds.RdsAssessmentReport(
    links = Seq("https://google.com"),
    version = 2,
    moduleId = "HMRC_ASSIST_ITSA_FINSUB_FEEDBACK_ACK",
    stepId = "execute",
    executionState = "completed",
    outputs = outputs)

  private def assessmentReportWrapper =
    AssessmentReportWrapper(mockCurrentDateTime.getDateTime.toLocalDateTime, rdsReport, CommonTestData.rdsNewSubmissionReport)

  private def expectedReportGenerationPayload = IFRequest(
    "self-assessment-assist",
    "GenerateReport",
    mockCurrentDateTime.getDateTime,
    "f2fb30e5-4ab6-4a29-b3c1-c00000011111",
    List(
      Map("nino"                 -> "nino"),
      Map("taxYear"              -> "2021-22"),
      Map("calculationId"        -> "99d758f6-c4be-4339-804e-f79cf0610d4f"),
      Map("customerType"         -> "Individual"),
      Map("calculationTimestamp" -> "2022-01-01T12:00:00.000Z")
    ),
    Some(
      Messages(
        Some(
          List(
            IFRequestPayload(
              "001",
              IFRequestPayloadAction(
                "Non-Business Income Source",
                "You have declared family loan as a source of your income. There have been changes to the rules around non-business sources you may declare, please check the appropriate guidance to see how this impacts you.",
                "Check guidance",
                "general/non_business_income_sources/income_source",
                Some(
                  List(
                    IFRequestPayloadActionLinks(
                      "ITSA Guidance",
                      "www.itsa.gov.uk"
                    ),
                    IFRequestPayloadActionLinks(
                      "Income Source Guidance",
                      "www.itsa/incomesources.gov.uk"
                    )
                  )
                )
              ),
              IFRequestPayloadAction(
                "Ffynhonnell Incwm Di-Fusnes",
                "Rydych wedi datgan benthyciad teulu fel ffynhonnell eich incwm. Bu newidiadau i'r rheolau ynghylch ffynonellau nad ydynt yn ymwneud â busnes y gallwch eu datgan, darllenwch y canllawiau priodol i weld sut mae hyn yn effeithio arnoch chi.",
                "Gwirio Canllawiau",
                "general/non_business_income_sources/income_source",
                Some(
                  List(
                    IFRequestPayloadActionLinks(
                      "Canllawiau ITSA",
                      "www.itsa/cym.gov.uk"
                    ),
                    IFRequestPayloadActionLinks(
                      "Arweiniad i Ffynonellau Incwm",
                      "www.itsa/incomesources.gov.uk"
                    )
                  )
                )
              )
            ),
            IFRequestPayload(
              "002",
              IFRequestPayloadAction(
                "Turnover",
                "Your declared turnover of £80,000 appears to be lower than expected based on your income sources, please confirm all turnover is accounted for before submission.",
                "Check turnover",
                "general/total_declared_turnover",
                Some(
                  List(
                    IFRequestPayloadActionLinks(
                      "Accounting for Income",
                      "www.itsa/incomecompliance.gov.uk"
                    )
                  )
                )
              ),
              IFRequestPayloadAction(
                "Trosiant",
                "Mae'n ymddangos bod eich trosiant datganedig o £80,000 yn is na'r disgwyl yn seiliedig ar eich ffynonellau incwm, cadarnhewch y cyfrifir am yr holl drosiant cyn cyflwyno.",
                "Gwiriwch y trosiant",
                "general/total_declared_turnover",
                Some(
                  List(
                    IFRequestPayloadActionLinks(
                      "Cyfrifo am Incwm",
                      "www.itsa/incomecompliance.gov.uk"
                    )
                  )
                )
              )
            )
          )
        )
      )
    )
  )

  private def expectedAcknowledgementPayload =
    IFRequest(
      serviceRegime = "self-assessment-assist",
      eventName = "AcknowledgeReport",
      eventTimestamp = mockCurrentDateTime.getDateTime,
      feedbackId = CommonTestData.simpleAcknowledgeNewRdsAssessmentReport.feedbackId.get.toString,
      metaData = List(
        Map("nino"         -> CommonTestData.simpleAcknowledgeReportRequest.nino),
        Map("customerType" -> "Individual")
      ),
      payload = None
    )

  class Test extends MockIfsConnector {
    val service = new IfsService(mockIfsConnector, mockCurrentDateTime)
  }

  "service using acknowledged generated" when {

    "submitGenerateReportMessage" must {

      "return the expected result" in new Test {

        MockCurrentDateTime.getDateTime
        MockIfsConnector
          .submit(expectedPayload = expectedReportGenerationPayload)
          .returns(Future.successful(Right(IfsResponse())))

        await(
          service.submitGenerateReportMessage(assessmentReportWrapper, assessmentRequestForSelfAssessment)
        ) shouldBe Right(IfsResponse())
      }

    }

    "service call unsuccessful" must {

      "return the expected result" in new Test {

        MockCurrentDateTime.getDateTime
        MockIfsConnector
          .submit(expectedPayload = expectedReportGenerationPayload)
          .returns(Future.successful(Left(ErrorWrapper(rdsReport.rdsCorrelationId, InternalError))))

        await(
          service.submitGenerateReportMessage(assessmentReportWrapper, assessmentRequestForSelfAssessment)
        ) shouldBe Left(ErrorWrapper(rdsReport.rdsCorrelationId, InternalError))
      }

    }

    "submitAcknowledgementMessage" must {

      "return the expected result" in new Test {

        MockCurrentDateTime.getDateTime
        MockIfsConnector
          .submit(expectedPayload = expectedAcknowledgementPayload)
          .returns(Future.successful(Right(IfsResponse())))

        await(
          service.submitAcknowledgementMessage(
            CommonTestData.simpleAcknowledgeReportRequest,
            CommonTestData.simpleAcknowledgeNewRdsAssessmentReport,
            CommonTestData.simpleIndividualUserDetails)
        ) shouldBe Right(IfsResponse())
      }
    }

    "customerTypeString" should {
      "return the expected result" in new Test {
        service.customerTypeString(CustomerType.TaxPayer) shouldBe "Individual"
        service.customerTypeString(CustomerType.Agent) shouldBe "Agent"
        an[IllegalStateException] shouldBe thrownBy(service.customerTypeString(CustomerType.Unknown))
      }
    }
  }

}
