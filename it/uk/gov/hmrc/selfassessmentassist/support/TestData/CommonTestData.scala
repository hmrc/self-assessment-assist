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

package uk.gov.hmrc.selfassessmentassist.support.TestData

import java.time.{LocalDateTime, Month, OffsetDateTime, ZoneOffset}
import play.api.libs.json.{Json, JsValue}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.selfassessmentassist.support.utils.StubResource.{loadAckResponseTemplate, loadSubmitResponseTemplate}
import uk.gov.hmrc.selfassessmentassist.utils.DateUtils
import uk.gov.hmrc.selfassessmentassist.v1.models.auth.UserDetails
import uk.gov.hmrc.selfassessmentassist.v1.models.domain.{CustomerType, _}
import uk.gov.hmrc.selfassessmentassist.v1.models.domain.PreferredLanguage.PreferredLanguage
import uk.gov.hmrc.selfassessmentassist.v1.models.request.GenerateReportRawData
import uk.gov.hmrc.selfassessmentassist.v1.services.cip.models.{FraudRiskReport, FraudRiskRequest}
import uk.gov.hmrc.selfassessmentassist.v1.services.nrs.models.request._
import uk.gov.hmrc.selfassessmentassist.v1.services.nrs.models.response.NrsResponse
import uk.gov.hmrc.selfassessmentassist.v1.services.rds.models.response.RdsAssessmentReport

import java.util.UUID

object CommonTestData  {

  val simpleNino: String = "AA000000B"
  val simpleCalculationId: UUID = UUID.fromString("f2fb30e5-4ab6-4a29-b3c1-c00000000001")
  val calculationIdWithNoFeedback: UUID = UUID.fromString("201204b4-06e3-4fef-a555-6fd0877dc7ca")
  val noCalculationFound: UUID = UUID.fromString("201404b4-06e3-4fef-a555-6fd0877dc7ca")
  val simpleRDSCorrelationId: String = "5fht738957jfjf845jgjf855"
  val simpleCIPCorrelationId: String = "5fht738957jfjf845jgjf855"
  val correlationId: String = "f2fb30e5-4ab6-4a29-b3c1-c00000011111"
  val simpleReportId: UUID = UUID.fromString(correlationId)

  val simpleRiskTitle = "title"
  val simpleRiskBody = "body"
  val simpleRiskAction = "action"
  val simpleLinkTitle = "title"
  val simpleLinkUrl = "url"
  val simplePath = "path"
  val simpleTaxYearEndInt=2022
  val simpleTaxYear = "2021-22"

  val simpleExternalOrigin: Origin = External
  val simpleInternalOrigin: Origin = Internal

  val simpleIndividualUserDetails: UserDetails = UserDetails(
    userType = AffinityGroup.Individual,
    agentReferenceNumber = None,
    clientID = "clientId",
    identityData = None
  )

  val simpleCustomerType: CustomerType.Value = CustomerType.TaxPayer
  val simplePreferredLanguage: PreferredLanguage = PreferredLanguage.English
  val simpleAgentRef: Option[Nothing] = None

  val simpleAssessmentRequestForSelfAssessment: AssessmentRequestForSelfAssessment = AssessmentRequestForSelfAssessment(
    calculationId = simpleCalculationId,
    nino = simpleNino,
    preferredLanguage = PreferredLanguage.English,
    customerType = CustomerType.TaxPayer,
    agentRef = None,
    taxYear = DesTaxYear.fromMtd(simpleTaxYear).toString)

  val simpleAssessmentReport: AssessmentReport = AssessmentReport(reportId = simpleReportId
    , risks = Seq(Risk(title = simpleRiskTitle, body = simpleRiskBody, action = simpleRiskAction
      , links = Seq(Link(simpleLinkTitle, simpleLinkUrl)), path = simplePath))
    , nino = simpleNino
    , taxYear = DesTaxYear.fromMtd(simpleTaxYear).toString
    , calculationId = simpleCalculationId,rdsCorrelationId = simpleRDSCorrelationId)

  val simpleCalculationTimestamp: LocalDateTime = LocalDateTime.parse("2019-02-15T09:35:15.094Z",DateUtils.dateTimePattern)

  val simpleGenerateReportRawData: GenerateReportRawData = GenerateReportRawData(simpleCalculationId.toString,simpleNino, PreferredLanguage.English, CustomerType.TaxPayer, None, simpleTaxYear)

  val simpleAssessmentReportMtdJson: JsValue = Json.toJson[AssessmentReport](simpleAssessmentReport)

  val simpleFraudRiskRequest: FraudRiskRequest = new FraudRiskRequest(nino = Some(simpleNino), taxYear = Some(simpleTaxYear), fraudRiskHeaders = Map.empty[String, String])
  val simpleFraudRiskReport: FraudRiskReport = new FraudRiskReport(0, simpleCIPCorrelationId, Seq.empty)

  val simpleGenerateReportControllerNrsID: String = "537490b4-06e3-4fef-a555-6fd0877dc7ca"
  val simpleSubmissionTimestamp: OffsetDateTime = OffsetDateTime.of(2022, Month.JANUARY.getValue,1 ,12, 0, 0, 0, ZoneOffset.UTC)


  val reportSubmissionId: String = UUID.fromString("f2fb30e5-4ab6-4a29-b3c1-c0000000010").toString
  val simpleNRSResponseReportSubmission: NrsResponse = NrsResponse(reportSubmissionId)

  val acknowledgeSubmissionId: String = UUID.fromString("f2fb30e5-4ab6-4a29-b3c1-c0000000011").toString
  val simpleNRSResponseAcknowledgeSubmission: NrsResponse = NrsResponse(acknowledgeSubmissionId)

  val simpleAcknowledgeReportRequest:AcknowledgeReportRequest = AcknowledgeReportRequest(simpleNino, simpleReportId.toString, simpleRDSCorrelationId)

  val rdsSubmissionReportJson: JsValue = loadSubmitResponseTemplate(simpleCalculationId.toString, simpleReportId.toString, simpleRDSCorrelationId)
  val rdsNewSubmissionReport: RdsAssessmentReport = rdsSubmissionReportJson.as[RdsAssessmentReport]
  
  val simpleAssessmentReportWrapper:AssessmentReportWrapper = AssessmentReportWrapper(simpleCalculationTimestamp,simpleAssessmentReport, rdsNewSubmissionReport)

  val rdsAssessmentAckJson: JsValue = loadAckResponseTemplate(simpleReportId.toString, nino=simpleNino, responseCode=202)
  val simpleAcknowledgeNewRdsAssessmentReport: RdsAssessmentReport = rdsAssessmentAckJson.as[RdsAssessmentReport]


  val invalidUUID: UUID = new UUID(0, 1)
  val invalidUUIDString: String = invalidUUID.toString

  val simpleCalculationIdStrangeCharsString: String = "f2fb30e5#4ab6#4a29-b3c1-c00000000001"
  val simpleReportIdStrangeCharsString: String = "f2fb30e5#4ab6#4a29-b3c1-c00000000001"

  val simpleNinoInvalid: String = "AA000000Z"
  val simpleTaxYearInvalid1: String = "2020-25"
  val simpleTaxYearInvalid2: String = "200-1"
  val simpleTaxYearInvalid3: String = "sjdhakjd"
}