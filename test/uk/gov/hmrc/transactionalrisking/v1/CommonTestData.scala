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

package uk.gov.hmrc.transactionalrisking.v1

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.transactionalrisking.models.domain._
import uk.gov.hmrc.transactionalrisking.models.request.AcknowledgeReportRawData
import uk.gov.hmrc.transactionalrisking.services.nrs.models.request._
import uk.gov.hmrc.transactionalrisking.services.nrs.models.response.NrsResponse
import uk.gov.hmrc.transactionalriskingsimulator.domain.WatchlistFlag

import java.time.{Month, OffsetDateTime, ZoneOffset}
import java.util.UUID

object CommonTestData {

  val simpleNino: String = "AA000000B"
  val simpleCalculationId: UUID = new UUID(0, 1)
  val simpleCorrelationId: String = "5fht738957jfjf845jgjf855"
  val simpleReportId = new UUID(0, 2)
  val simpleRiskTitle = "title"
  val simpleRiskBody = "body"
  val simpeRiskAction = "action"
  val simpleLinkTitle = "title"
  val simpleLinkUrl = "url"
  val simpePath = "path"
  val simpeTaxYear = "2021-22"

  val simpleExternalOrigin: Origin = External
  val simpleInternalOrigin: Origin = Internal
  implicit val correlationId: String = UUID.randomUUID().toString

  val simpleAssessmentRequestForSelfAssessment: AssessmentRequestForSelfAssessment = AssessmentRequestForSelfAssessment(
    calculationId = simpleCalculationId,
    nino = simpleNino,
    preferredLanguage = PreferredLanguage.English,
    customerType = CustomerType.TaxPayer,
    agentRef = None,
    taxYear = DesTaxYear.fromMtd(simpeTaxYear).toString)

  val simpleAssementReport = AssessmentReport(reportId = simpleReportId
    , risks = Seq(Risk(title = simpleRiskTitle, body = simpleRiskBody, action = simpeRiskAction
      , links = Seq(Link(simpleLinkTitle, simpleLinkUrl)), path = simpePath))
    , nino = simpleNino
    , taxYear = DesTaxYear.fromMtd(simpeTaxYear).toString
    , calculationId = simpleCalculationId,correlationID = simpleCorrelationId)

  val simpleAsssementReportMtdJson: JsValue = Json.toJson[AssessmentReport](simpleAssementReport)

  val simpleFraudRiskRequest: FraudRiskRequest = new FraudRiskRequest(nino = simpleNino, taxYear = simpeTaxYear, fraudRiskHeaders = Map.empty[String, String])
  val simpleFraudRiskReport: FraudRiskReport = new FraudRiskReport(decision = FraudDecision.Accept, 0, Set.empty[FraudRiskHeader], Set.empty[WatchlistFlag].empty)

  val simpleMetadata: Metadata = null
  val simplePayload: String = ""

  val simpleBody: RequestBody = null
  val simpleGenerateReportRequest = RequestData(nino = simpleNino, body = simpleBody)
  val simpleGeneratedNrsId: String = "537490b4-06e3-4fef-a555-6fd0877dc7ca"
  val simpleSubmissionTimestamp: OffsetDateTime = OffsetDateTime.of(2022, Month.JANUARY.getValue,1 ,12, 0, 0, 0, ZoneOffset.UTC)
  val simpeNotableEventType: NotableEventType = AssistReportGenerated

  val reportSubmissionId = (new UUID(0,3)).toString
  val simpleNRSResponse = new NrsResponse(reportSubmissionId)


//  val simpleFeedbackId:String = "a365cc12c845c057eb548febfa8048ba"
  val simpleAcknowledgeReportRawData:AcknowledgeReportRawData = AcknowledgeReportRawData(simpleNino, simpleReportId.toString, simpleCorrelationId)
  val simpeAcknowledgeReportRequest:AcknowledgeReportRequest = AcknowledgeReportRequest(simpleNino, simpleReportId.toString, simpleCorrelationId:String)
    //TODO: ask if rportId and simplefeedbcakId not the same. Do we get a diff reponse from ack to that in gen report

//  val simpleAcknowledgementReturn:JsValue = JsString("")
//  val simpleAcknowledgementMtdJson: JsValue = Json.toJson[JsValue](simpleAcknowledgementReturn)


}
