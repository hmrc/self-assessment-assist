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
import uk.gov.hmrc.transactionalrisking.v1.services.ifs.models.request.{IFRequest, IFRequestPayload, IFRequestPayloadAction, IFRequestPayloadActionLinks}

import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import uk.gov.hmrc.transactionalrisking.utils.DateUtils
import uk.gov.hmrc.transactionalrisking.v1.models.domain.PreferredLanguage.PreferredLanguage
import uk.gov.hmrc.transactionalrisking.v1.services.rds.models.response.RdsAssessmentReport

@Singleton
class IfsService @Inject()(connector: IfsConnector, currentDateTime: CurrentDateTime) extends Logging {

  def submitGenerateReportMessage(assessmentReport: AssessmentReport, calculationTimestamp: LocalDateTime, request: AssessmentRequestForSelfAssessment, rdsAssessmentReport: RdsAssessmentReport)(implicit hc: HeaderCarrier, correlationId: String): Future[IfsOutcome] = {
    connector.submit(buildIfsGenerateReportSubmission(assessmentReport, "GenerateReport", calculationTimestamp, request, rdsAssessmentReport))
  }

  private def buildIfsGenerateReportSubmission(assessmentReport: AssessmentReport, eventName: String, calculationTimestamp: LocalDateTime, request: AssessmentRequestForSelfAssessment, rdsAssessmentReport: RdsAssessmentReport)(implicit correlationId: String): IFRequest = {
    val englishActions = risks(rdsAssessmentReport, PreferredLanguage.English)
    val welshActions = risks(rdsAssessmentReport, PreferredLanguage.Welsh)
    IFRequest(
        serviceRegime = "self-assessment-assist",
        eventName,
        eventTimestamp = currentDateTime.getDateTime(),
        feedbackId = rdsAssessmentReport.feedbackId.fold("")(_.toString),
        metadata = List(
          Map("nino" -> assessmentReport.nino),
          Map("taxYear" -> assessmentReport.taxYear),
          Map("calculationId" -> assessmentReport.calculationId.toString),
          Map("customerType" -> request.customerType.toString),
          Map("agentReferenceNumber" -> request.agentRef.getOrElse("")),
          Map("calculationTimestamp" -> calculationTimestamp.format(DateUtils.dateTimePattern)) // What does comment mean here
        ),
        payload = Some(englishActions.zipWithIndex.map{
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
              messageId = risk.path, // what is the messageId
              englishAction = englishAction,
              welshAction = welsh
            )
        })
    )
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


}
