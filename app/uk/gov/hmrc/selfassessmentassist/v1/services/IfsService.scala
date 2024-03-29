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
import uk.gov.hmrc.selfassessmentassist.v1.models.domain._
import uk.gov.hmrc.selfassessmentassist.v1.models.request.ifs._
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

    val englishActions               = risks(rdsAssessmentReport, PreferredLanguage.English)
    val welshActions                 = risks(rdsAssessmentReport, PreferredLanguage.Welsh)
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
          Some(englishActions.zipWithIndex.map { case (risk, index) =>
            val englishAction = IFRequestPayloadAction(
              title = risk.title,
              message = risk.body,
              action = risk.action,
              path = risk.path,
              links = mapToList(risk.links)
            )
            val welsh = IFRequestPayloadAction(
              title = welshActions(index).title,
              message = welshActions(index).body,
              action = welshActions(index).action,
              path = welshActions(index).path,
              links = mapToList(welshActions(index).links)
            )
            logger.info(s"processing risk with index $risk and $index")
            val messageIds = payloadMessageIds(index)
            logger.info(s"in ifs payloadMessageIds $messageIds}")

            IFRequestPayload(
              messageId = messageIds,
              englishAction = englishAction,
              welshAction = welsh
            )
          })
        ))
    )
  }

  private def mapToList(links: Seq[Link]): Option[Seq[IFRequestPayloadActionLinks]] = {
    if (links.nonEmpty) {
      val updatedLinks = links.map { e =>
        val titleList = if (isList(e.title)) parseList(e.title) else Seq(e.title)
        val urlList   = if (isList(e.url)) parseList(e.url) else Seq(e.url)
        titleList.zipAll(urlList, "", "").map { case (title, url) =>
          IFRequestPayloadActionLinks(title, url)
        }
      }
      Some(updatedLinks.flatten)
    } else {
      None
    }
  }

  private def isList(s: String): Boolean = s.startsWith("[") && s.endsWith("]")

  private def parseList(s: String): Seq[String] = s.stripPrefix("[").stripSuffix("]").split(",").map(_.trim).toSeq

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

  private def risks(report: RdsAssessmentReport, preferredLanguage: PreferredLanguage): Seq[Risk] = {
    report.outputs
      .collect {
        case elm: RdsAssessmentReport.MainOutputWrapper if isPreferredLanguage(elm.name, preferredLanguage) => elm
      }
      .flatMap(_.value.getOrElse(Seq.empty))
      .collect { case value: RdsAssessmentReport.DataWrapper =>
        value
      }
      .flatMap(_.data.getOrElse(Seq.empty))
      .flatMap(toRisk)
  }

  private def isPreferredLanguage(language: String, preferredLanguage: PreferredLanguage): Boolean = preferredLanguage match {
    case PreferredLanguage.English if language == "EnglishActions" => true
    case PreferredLanguage.Welsh if language == "WelshActions"     => true
    case _                                                         => false
  }

  private def toRisk(riskParts: Seq[String]): Option[Risk] = {
    if (riskParts.isEmpty) None
    else
      Some(
        Risk(title = riskParts(2), body = riskParts.head, action = riskParts(1), links = Seq(Link(riskParts(3), riskParts(4))), path = riskParts(5)))
  }

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
