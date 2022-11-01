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

package uk.gov.hmrc.transactionalrisking.v1.TestData

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.transactionalrisking.v1.utils.StubResource.{loadAckResponseTemplate, loadSubmitResponseTemplate}
import uk.gov.hmrc.transactionalrisking.models.domain._
import uk.gov.hmrc.transactionalrisking.models.request.AcknowledgeReportRawData
import uk.gov.hmrc.transactionalrisking.services.nrs.models.request._
import uk.gov.hmrc.transactionalrisking.services.nrs.models.response.NrsResponse
import uk.gov.hmrc.transactionalrisking.services.rds.models.request.RdsRequest
import uk.gov.hmrc.transactionalrisking.services.rds.models.response.NewRdsAssessmentReport
import uk.gov.hmrc.transactionalriskingsimulator.domain.WatchlistFlag

import java.time.{Month, OffsetDateTime, ZoneOffset}
import java.util.UUID

class CommonTestData  {

  val simpleNino: String = "AA000000B"
  val simpleCalculationID: UUID = UUID.fromString("f2fb30e5-4ab6-4a29-b3c1-c00000000001")
  val simpleRDSCorrelationID: String = "5fht738957jfjf845jgjf855"
  val simpleReportID = UUID.fromString("f2fb30e5-4ab6-4a29-b3c1-c00000000101")
  val simpleRiskTitle = "title"
  val simpleRiskBody = "body"
  val simpeRiskAction = "action"
  val simpleLinkTitle = "title"
  val simpleLinkUrl = "url"
  val simpePath = "path"
  val simpleTaxYearEndInt=2022
  val simpeTaxYear = "2021-22"

  val simpleExternalOrigin: Origin = External
  val simpleInternalOrigin: Origin = Internal
  val internalCorrelationID: String = UUID.fromString("f2fb30e5-4ab6-4a29-b3c1-c00000000201").toString
  implicit val internalCorrelationIDImplicit: String = internalCorrelationID

  val simpleAssessmentRequestForSelfAssessment: AssessmentRequestForSelfAssessment = AssessmentRequestForSelfAssessment(
    calculationID = simpleCalculationID,
    nino = simpleNino,
    preferredLanguage = PreferredLanguage.English,
    customerType = CustomerType.TaxPayer,
    agentRef = None,
    taxYear = DesTaxYear.fromMtd(simpeTaxYear).toString)

  val simpleAssementReport = AssessmentReport(reportID = simpleReportID
    , risks = Seq(Risk(title = simpleRiskTitle, body = simpleRiskBody, action = simpeRiskAction
      , links = Seq(Link(simpleLinkTitle, simpleLinkUrl)), path = simpePath))
    , nino = simpleNino
    , taxYear = DesTaxYear.fromMtd(simpeTaxYear).toString
    , calculationID = simpleCalculationID,rdsCorrelationId = simpleRDSCorrelationID)

  val simpleAsssementReportMtdJson: JsValue = Json.toJson[AssessmentReport](simpleAssementReport)

  val simpleFraudRiskRequest: FraudRiskRequest = new FraudRiskRequest(nino = simpleNino, taxYear = simpeTaxYear, fraudRiskHeaders = Map.empty[String, String])
  val simpleFraudRiskReport: FraudRiskReport = new FraudRiskReport(decision = FraudDecision.Accept, 0, Set.empty[FraudRiskHeader], Set.empty[WatchlistFlag].empty)

  val simpleMetadata: Metadata = null
  val simplePayload: String = ""

  val simpleBody: RequestBody = null
  val simpleGenerateReportControllerRequest = RequestData(nino = simpleNino, body = simpleBody)

  val simpleGenerateReportControllerNrsID: String = "537490b4-06e3-4fef-a555-6fd0877dc7ca"
  val simpleSubmissionTimestamp: OffsetDateTime = OffsetDateTime.of(2022, Month.JANUARY.getValue,1 ,12, 0, 0, 0, ZoneOffset.UTC)
  val simpeNotableEventType: NotableEventType = AssistReportGenerated

  val reportSubmissionID = UUID.fromString("f2fb30e5-4ab6-4a29-b3c1-c0000000010").toString
  val simpleNRSResponseReportSubmission = new NrsResponse(reportSubmissionID)

  val simpleAcknowledgeNrsID: String = "537490b4-06e3-4fef-a555-6fd0877dc7ca"
  val simpleAcknowledgedSubmissionTimestamp: OffsetDateTime = OffsetDateTime.of(2022, Month.JANUARY.getValue, 1, 12, 0, 0, 0, ZoneOffset.UTC)
  val simpeAcknowledgedNotableEventType: NotableEventType = AssistReportAcknowledged

  val simpleBodyAcknowledge: RequestBody = null
  val simpleAcknowledgeReportRequest = RequestData(nino = simpleNino, body = simpleBody)

  val acknowledgeSubmissionIdString = ""
  val acknowledgeSubmissionID = UUID.fromString("f2fb30e5-4ab6-4a29-b3c1-c0000000011").toString
  val simpleNRSResponseAcknowledgeSubmission = new NrsResponse(acknowledgeSubmissionID)


  //  val simpleFeedbackID:String = "a365cc12c845c057eb548febfa8048ba"
  val simpleAcknowledgeReportRawData:AcknowledgeReportRawData = AcknowledgeReportRawData(simpleNino, simpleReportID.toString, simpleRDSCorrelationID)
  val simpeAcknowledgeReportRequest:AcknowledgeReportRequest = AcknowledgeReportRequest(simpleNino, simpleReportID.toString, simpleRDSCorrelationID:String)


  val rdsSubmissionReportJson = loadSubmitResponseTemplate(simpleCalculationID.toString, simpleReportID.toString, simpleRDSCorrelationID )
  val rdsSubmissionReport: String = rdsSubmissionReportJson.toString()  //as[String]
  val rdsNewSubmissionReport: NewRdsAssessmentReport = rdsSubmissionReportJson.as[NewRdsAssessmentReport]

  val rdsSubmitRequest: RdsRequest =
    RdsRequest(
      Seq()
    )

  val rdsAssessmentAckJson = loadAckResponseTemplate(simpleReportID.toString, replaceNino=simpleNino, replaceResponseCode="202")
  val rdsAssessmentAck: NewRdsAssessmentReport = rdsAssessmentAckJson.as[NewRdsAssessmentReport]
  val simpleAcknowledgeNewRdsAssessmentReport = rdsAssessmentAck


  val invalidUUID: UUID = new UUID(0, 1)
  val invalidUUIDString: String = invalidUUID.toString
    // Actually invalid type is not determined.

  val simpleCalculationIDStrangeCharsString: String = "f2fb30e5#4ab6#4a29-b3c1-c00000000001"
  val simpleReportaIDStrangeCharsString: String = "f2fb30e5#4ab6#4a29-b3c1-c00000000001"

  val simpleNinoInvalid: String = "AA000000Z"

}

object CommonTestData {
  val commonTestData = new CommonTestData
}