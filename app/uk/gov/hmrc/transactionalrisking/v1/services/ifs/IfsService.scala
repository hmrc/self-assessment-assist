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

package uk.gov.hmrc.transactionalrisking.v1.services.ifs

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transactionalrisking.utils.{CurrentDateTime, Logging}
import uk.gov.hmrc.transactionalrisking.v1.models.domain.{AssessmentReport, AssessmentRequestForSelfAssessment, Link, PreferredLanguage, Risk}
import uk.gov.hmrc.transactionalrisking.v1.services.ifs.models.request.{IFRequest, IFRequestPayload, IFRequestPayloadAction, IFRequestPayloadActionLinks, Messages}

import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import uk.gov.hmrc.transactionalrisking.utils.DateUtils
import uk.gov.hmrc.transactionalrisking.v1.models.auth.UserDetails
import uk.gov.hmrc.transactionalrisking.v1.models.domain.CustomerType.{Agent, CustomerType, TaxPayer}
import uk.gov.hmrc.transactionalrisking.v1.models.domain.PreferredLanguage.PreferredLanguage
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.request.AcknowledgeReportRequest
import uk.gov.hmrc.transactionalrisking.v1.services.rds.models.response.RdsAssessmentReport

@Singleton
class IfsService @Inject()(connector: IfsConnector, currentDateTime: CurrentDateTime) extends Logging {

  def submitGenerateReportMessage(assessmentReport: AssessmentReport, calculationTimestamp: LocalDateTime, request: AssessmentRequestForSelfAssessment, rdsAssessmentReport: RdsAssessmentReport)(implicit hc: HeaderCarrier, correlationId: String): Future[IfsOutcome] = {
    val req = buildIfsGenerateReportSubmission(
      assessmentReport,
      calculationTimestamp,
      request,
      rdsAssessmentReport
    )
    connector.submit(req)
  }

  def submitAcknowledgementMessage(acknowledgeReportRequest: AcknowledgeReportRequest, rdsAssessmentReport: RdsAssessmentReport, userDetails: UserDetails)(implicit hc: HeaderCarrier, correlationId: String): Future[IfsOutcome] = {
    val req = buildIfsAcknowledgementSubmission(acknowledgeReportRequest, rdsAssessmentReport, userDetails)
    connector.submit(req)
  }

  private def buildIfsGenerateReportSubmission(assessmentReport: AssessmentReport, calculationTimestamp: LocalDateTime, request: AssessmentRequestForSelfAssessment, rdsAssessmentReport: RdsAssessmentReport): IFRequest = {
    val englishActions = risks(rdsAssessmentReport, PreferredLanguage.English)
    val welshActions = risks(rdsAssessmentReport, PreferredLanguage.Welsh)
    val payloadMessageIds = typeIds(rdsAssessmentReport)
    IFRequest(
        serviceRegime = "self-assessment-assist",
        "GenerateReport",
        eventTimestamp = currentDateTime.getDateTime(),
        feedbackId = rdsAssessmentReport.feedbackId.fold("")(_.toString),
        metadata = List(
          Map("nino" -> assessmentReport.nino),
          Map("taxYear" -> assessmentReport.taxYear),
          Map("calculationId" -> assessmentReport.calculationId.toString),
          Map("customerType" -> customerTypeString(request.customerType)),
          Map("calculationTimestamp" -> calculationTimestamp.format(DateUtils.dateTimePattern))
        ) ++ request.agentRef.fold(List.empty[Map[String, String]])(e => List(Map("agentReferenceNumber" -> e))),
        payload = Some(Messages(
          Some(englishActions.zipWithIndex.map{
          case (risk, index) =>
            val englishAction = IFRequestPayloadAction(
              title = risk.title,
              message = risk.body,
              action = risk.action,
              path = risk.path,
              links = if(risk.links.nonEmpty) Some(risk.links.map(e => IFRequestPayloadActionLinks(e.title, e.url))) else None
            )
            val welsh = IFRequestPayloadAction(
              title = welshActions(index).title,
              message = welshActions(index).body,
              action = welshActions(index).action,
              path = welshActions(index).path,
              links = if(welshActions(index).links.nonEmpty) Some(welshActions(index).links.map(e => IFRequestPayloadActionLinks(e.title, e.url))) else None
            )
            IFRequestPayload(
              messageId = payloadMessageIds(index),
              englishAction = englishAction,
              welshAction = welsh
            )
        })
      ))
    )
  }

  private def buildIfsAcknowledgementSubmission(acknowledgeReportRequest: AcknowledgeReportRequest, rdsAssessmentReport: RdsAssessmentReport, userDetails: UserDetails): IFRequest = {
    IFRequest(
      serviceRegime = "self-assessment-assist",
      "AcknowledgeReport",
      eventTimestamp = currentDateTime.getDateTime(),
      feedbackId = rdsAssessmentReport.feedbackId.fold("")(_.toString),
      metadata = List(
        Map("nino" -> acknowledgeReportRequest.nino),
        Map("customerType" -> customerTypeString(userDetails.toCustomerType)),
      ) ++ userDetails.agentReferenceNumber.fold(List.empty[Map[String, String]])(e => List(Map("agentReferenceNumber" -> e))),
      payload = None,
    )
  }

  private def typeIds(report: RdsAssessmentReport): Seq[String] = {
    report.outputs.collect {
      case elm: RdsAssessmentReport.MainOutputWrapper if elm.name == "typeId" => elm
    }.flatMap(_.value).collect {
      case value: RdsAssessmentReport.DataWrapper => value
    }.flatMap(_.data).flatten
  }

  private def risks(report: RdsAssessmentReport, preferredLanguage: PreferredLanguage): Seq[Risk] = {
    report.outputs.collect {
      case elm: RdsAssessmentReport.MainOutputWrapper if isPreferredLanguage(elm.name, preferredLanguage) => elm
    }.flatMap(_.value).collect {
      case value: RdsAssessmentReport.DataWrapper => value
    }.flatMap(_.data)
      .flatMap(toRisk)
  }

  private def isPreferredLanguage(language: String, preferredLanguage: PreferredLanguage) = preferredLanguage match {
    case PreferredLanguage.English if language == "EnglishActions" => true
    case PreferredLanguage.Welsh if language == "WelshActions" => true
    case _ => false
  }

  private def toRisk(riskParts: Seq[String]):Option[Risk] = {
    if(riskParts.isEmpty) None
    else
      Some(Risk(title = riskParts(2),
        body = riskParts.head, action = riskParts(1),
        links = Seq(Link(riskParts(3), riskParts(4))), path = riskParts(5)))
  }

  private def customerTypeString(customerType: CustomerType) = customerType match {
    case TaxPayer => "Individual"
    case Agent => "Agent"
  }

}
