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

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfassessmentassist.api.models.auth.UserDetails
import uk.gov.hmrc.selfassessmentassist.api.models.domain.CustomerType.{Agent, CustomerType, TaxPayer}
import uk.gov.hmrc.selfassessmentassist.api.models.domain.PreferredLanguage
import uk.gov.hmrc.selfassessmentassist.api.models.domain.PreferredLanguage.PreferredLanguage
import uk.gov.hmrc.selfassessmentassist.utils.{CurrentDateTime, DateUtils, Logging}
import uk.gov.hmrc.selfassessmentassist.v1.connectors.IfsConnector
import uk.gov.hmrc.selfassessmentassist.v1.models.domain.*
import uk.gov.hmrc.selfassessmentassist.v1.models.request.ifs.*
import uk.gov.hmrc.selfassessmentassist.v1.models.request.nrs.AcknowledgeReportRequest
import uk.gov.hmrc.selfassessmentassist.v1.models.response.rds.RdsAssessmentReport

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class IfsService @Inject() (connector: IfsConnector, currentDateTime: CurrentDateTime) extends Logging {

  def submitGenerateReportMessage(rdsAssessmentReportWrapper: AssessmentReportWrapper, request: AssessmentRequestForSelfAssessment)(implicit
      hc: HeaderCarrier,
      correlationId: String): Future[IfsOutcome] = {
    val req = buildIfsGenerateReportSubmission(
      rdsAssessmentReportWrapper,
      request
    )
    connector.submit(req)
  }

  private def buildIfsGenerateReportSubmission(rdsAssessmentReportWrapper: AssessmentReportWrapper,
                                               request: AssessmentRequestForSelfAssessment): IFRequest = {

    val rdsAssessmentReport = rdsAssessmentReportWrapper.rdsAssessmentReport

    val englishRisks                 = risks(rdsAssessmentReport, PreferredLanguage.English)
    val welshRisks                   = risks(rdsAssessmentReport, PreferredLanguage.Welsh)
    val payloadMessageIds            = typeIds(rdsAssessmentReport)
    val assessmentReportId           = rdsAssessmentReportWrapper.report.reportId
    val assessmentCalculationId      = rdsAssessmentReportWrapper.report.calculationId
    val assessmentReportNino         = rdsAssessmentReportWrapper.report.nino
    val assessmentReportTaxYearAsMtd = rdsAssessmentReportWrapper.report.taxYear.asMtd
    val calculationTimestamp         = rdsAssessmentReportWrapper.calculationTimestamp

    logger.info(s"in ifs processing ${assessmentReportId} and ${assessmentCalculationId}")

    IFRequest(
      serviceRegime = "self-assessment-assist",
      "GenerateReport",
      eventTimestamp = currentDateTime.getDateTime,
      feedbackId = rdsAssessmentReport.feedbackId.fold("")(_.toString),
      metaData = List(
        Map("nino"                 -> assessmentReportNino),
        Map("taxYear"              -> assessmentReportTaxYearAsMtd),
        Map("calculationId"        -> assessmentCalculationId.toString),
        Map("customerType"         -> customerTypeString(request.customerType)),
        Map("calculationTimestamp" -> calculationTimestamp.format(DateUtils.dateTimePattern))
      ) ++ request.agentRef.fold(List.empty[Map[String, String]])(e => List(Map("agentReferenceNumber" -> e))),
      payload = Some(
        Messages(
          Some((englishRisks zip welshRisks).zipWithIndex.map { case ((englishRisk, welshRisk), index) =>
            logger.info(s"processing risk with index $index")

            val messageIds = payloadMessageIds(index)
            logger.info(s"in ifs payloadMessageIds $messageIds}")

            IFRequestPayload(
              messageId = messageIds,
              englishAction = ifsAction(englishRisk),
              welshAction = ifsAction(welshRisk)
            )
          })
        ))
    )
  }

  private def ifsAction(risk: Risk) = {
    def ifsLinks(links: Seq[Link]): Option[Seq[IFRequestPayloadActionLinks]] =
      if (links.nonEmpty) Some(links.map(e => IFRequestPayloadActionLinks(e.title, e.url))) else None

    IFRequestPayloadAction(
      title = risk.title,
      message = risk.body,
      action = risk.action,
      path = risk.path,
      links = ifsLinks(risk.links)
    )
  }

  private def typeIds(report: RdsAssessmentReport): Seq[String] = {
    report.outputs
      .collect {
        case elm: RdsAssessmentReport.MainOutputWrapper if elm.name == "typeId" => elm
      }
      .flatMap(_.value.getOrElse(Seq.empty))
      .collect { case value: RdsAssessmentReport.DataWrapper =>
        value
      }
      .flatMap(_.data.getOrElse(Seq.empty))
      .flatten
  }

  private def risks(report: RdsAssessmentReport, preferredLanguage: PreferredLanguage): Seq[Risk] =
    Risk.risksFromRdsReportOutputs(report.outputs, preferredLanguage)

  def customerTypeString(customerType: CustomerType): String = customerType match {
    case TaxPayer => "Individual"
    case Agent    => "Agent"
    case _        => throw new IllegalStateException(s"Invalid $customerType")
  }

  def submitAcknowledgementMessage(acknowledgeReportRequest: AcknowledgeReportRequest,
                                   rdsAssessmentReport: RdsAssessmentReport,
                                   userDetails: UserDetails)(implicit hc: HeaderCarrier, correlationId: String): Future[IfsOutcome] = {
    val req = buildIfsAcknowledgementSubmission(acknowledgeReportRequest, rdsAssessmentReport, userDetails)
    connector.submit(req)
  }

  private def buildIfsAcknowledgementSubmission(acknowledgeReportRequest: AcknowledgeReportRequest,
                                                rdsAssessmentReport: RdsAssessmentReport,
                                                userDetails: UserDetails): IFRequest = {
    IFRequest(
      serviceRegime = "self-assessment-assist",
      "AcknowledgeReport",
      eventTimestamp = currentDateTime.getDateTime,
      feedbackId = rdsAssessmentReport.feedbackId.fold("")(_.toString),
      metaData = List(
        Map("nino"         -> acknowledgeReportRequest.nino),
        Map("customerType" -> customerTypeString(userDetails.toCustomerType))
      ) ++ userDetails.agentReferenceNumber.fold(List.empty[Map[String, String]])(e => List(Map("agentReferenceNumber" -> e))),
      payload = None
    )
  }

}
