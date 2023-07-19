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
import uk.gov.hmrc.selfassessmentassist.api.controllers.UserRequest
import uk.gov.hmrc.selfassessmentassist.api.models.auth.RdsAuthCredentials
import uk.gov.hmrc.selfassessmentassist.api.models.domain.PreferredLanguage
import uk.gov.hmrc.selfassessmentassist.api.models.domain.PreferredLanguage.PreferredLanguage
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{
  ErrorWrapper,
  InternalError,
  MatchingCalculationIDNotFoundError,
  NoAssessmentFeedbackFromRDS
}
import uk.gov.hmrc.selfassessmentassist.api.models.outcomes.ResponseWrapper
import uk.gov.hmrc.selfassessmentassist.config.AppConfig
import uk.gov.hmrc.selfassessmentassist.utils.{DateUtils, Logging}
import uk.gov.hmrc.selfassessmentassist.v1.connectors.{RdsAuthConnector, RdsConnector}
import uk.gov.hmrc.selfassessmentassist.v1.models.domain._
import uk.gov.hmrc.selfassessmentassist.v1.models.request.cip.FraudRiskReport
import uk.gov.hmrc.selfassessmentassist.v1.models.request.nrs.AcknowledgeReportRequest
import uk.gov.hmrc.selfassessmentassist.v1.models.request.rds.RdsRequest
import uk.gov.hmrc.selfassessmentassist.v1.models.request.rds.RdsRequest.{DataWrapper, MetadataWrapper}
import uk.gov.hmrc.selfassessmentassist.v1.models.response.rds.RdsAssessmentReport

import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RdsService @Inject() (rdsAuthConnector: RdsAuthConnector[Future], connector: RdsConnector, appConfig: AppConfig) extends Logging {

  val requiredHeaderForRDS_with_Empty: Seq[(String, String)] = List(
    "Gov-Client-Connection-Method",
    "Gov-Client-Device-ID",
    "Gov-Client-Local-IPs",
    "Gov-Client-Local-IPs-Timestamp",
    "Gov-Client-MAC-Addresses",
    "Gov-Client-Multi-Factor",
    "Gov-Client-Screens",
    "Gov-Client-Timezone",
    "Gov-Client-User-Agent",
    "Gov-Client-User-IDs",
    "Gov-Client-Window-Size",
    "Gov-Vendor-License-IDs",
    "Gov-Vendor-Product-Name",
    "Gov-Vendor-Version",
    "Gov-Client-Public-IP",
    "Gov-Client-Public-IP-Timestamp",
    "Gov-Client-Public-Port",
    "Gov-Vendor-Public-IP",
    "Gov-Vendor-Forwarded",
    "Gov-Client-Browser-Do-Not-Track",
    "Gov-Client-Browser-JS-User-Agent"
  ).map(_ -> "")

//TODO Refactor this code
  def submit(request: AssessmentRequestForSelfAssessment, fraudRiskReport: FraudRiskReport)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      // logContext: EndpointLogContext,
      userRequest: UserRequest[_],
      correlationId: String): Future[ServiceOutcome[AssessmentReportWrapper]] = {
    val requestHeaders: Map[String, String] = userRequest.headers.toMap.map(h => h._1 -> h._2.head)
    val fraudRiskReportHeaders: Seq[(String, String)] =
      requiredHeaderForRDS_with_Empty.map(entry => entry._1 -> requestHeaders.getOrElse(entry._1, ""))

    def processRdsRequest(
        rdsAuthCredentials: Option[RdsAuthCredentials] = None): Future[Either[ErrorWrapper, ResponseWrapper[AssessmentReportWrapper]]] = {
      val rdsRequestSO: RdsRequest = generateRdsAssessmentRequest(request, fraudRiskReport, fraudRiskReportHeaders)
      connector.submit(rdsRequestSO, rdsAuthCredentials).map {
        case Right(ResponseWrapper(_, rdsResponse)) =>
          val assessmentReportSO = toAssessmentReport(rdsResponse, request, correlationId)
          assessmentReportSO match {
            case Right(ResponseWrapper(_, assessmentReport)) =>
              logger.debug(s"$correlationId::[submit]submit request for report successful returning it")
              Right(ResponseWrapper(correlationId, assessmentReport))

            case Left(errorWrapper) =>
              logger.error(s"$correlationId::[RdsService][submit]submit request for report error from service ${errorWrapper.error}")
              Left(errorWrapper)
          }
        case Left(errorWrapper) =>
          errorWrapper.error match {
            case NoAssessmentFeedbackFromRDS =>
              logger.info(s"$correlationId::[RdsService][submit] No feedback available in RDS")
              Left(errorWrapper)
            case MatchingCalculationIDNotFoundError =>
              logger.warn(s"$correlationId::[RdsService][submit] ${errorWrapper.error}")
              Left(errorWrapper)
            case _ =>
              logger.error(s"$correlationId::[RdsService][submit] RDS connector failed Unable to generate report ${errorWrapper.error}")
              Left(errorWrapper)
          }

      }
    }

    if (appConfig.rdsAuthRequiredForThisEnv) {
      logger.debug(s"$correlationId::[submit]RDS Auth Required}")
      rdsAuthConnector
        .retrieveAuthorisedBearer()
        .foldF(
          error => Future.successful(Left(ErrorWrapper(correlationId, error))),
          credentials => processRdsRequest(Some(credentials))
        )
    } else {
      logger.debug(s"$correlationId::[submit]RDS Auth Not Required}")
      processRdsRequest()
    }
  }

  private def toAssessmentReport(report: RdsAssessmentReport,
                                 request: AssessmentRequestForSelfAssessment,
                                 correlationId: String): ServiceOutcome[AssessmentReportWrapper] = {

    (report.calculationId, report.feedbackId, report.calculationTimestamp) match {
      case (Some(calculationId), Some(reportId), Some(calculationTimestamp)) =>
        val rdsCorrelationIdOption = report.rdsCorrelationId
        rdsCorrelationIdOption match {
          case Some(rdsCorrelationID) =>
            val parsedCalculationTimestamp = LocalDateTime.parse(calculationTimestamp, DateUtils.dateTimePattern)
            logger.info(s"$correlationId::[toAssessmentReport]Successfully recieved assessment report")

            Right(
              ResponseWrapper(
                correlationId,
                AssessmentReportWrapper(
                  parsedCalculationTimestamp,
                  AssessmentReport(
                    reportId = reportId,
                    risks = risks(report, request.preferredLanguage),
                    nino = request.nino,
                    taxYear = request.taxYear,
                    calculationId = calculationId,
                    rdsCorrelationID
                  ),
                  report
                )
              ))

          case None =>
            logger.warn(s"$correlationId::[RdsService][toAssessmentReport]Unable to find rdsCorrelationId")
            Left(ErrorWrapper(correlationId, InternalError))
        }

      case (_, _, _) =>
        logger.warn(
          s"$correlationId::[RdsService][toAssessmentReport] Either calculationId or feedbackId or calculationTimestamp missing in RDS response")
        Left(ErrorWrapper(correlationId, InternalError))
    }
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

  private def isPreferredLanguage(language: String, preferredLanguage: PreferredLanguage) = preferredLanguage match {
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

  private def generateRdsAssessmentRequest(request: AssessmentRequestForSelfAssessment,
                                           fraudRiskReport: FraudRiskReport,
                                           fraudRiskReportHeaders: Seq[(String, String)]): RdsRequest = RdsRequest(
    Seq(
      RdsRequest.InputWithString("calculationId", request.calculationId.toString),
      RdsRequest.InputWithString("nino", request.nino),
      RdsRequest.InputWithInt("taxYear", request.taxYear.asRds),
      RdsRequest.InputWithString("customerType", request.customerType.toString),
      RdsRequest.InputWithString("agentRef", request.agentRef.getOrElse("")),
      RdsRequest.InputWithString("preferredLanguage", request.preferredLanguage.toString),
      RdsRequest.InputWithInt("fraudRiskReportScore", fraudRiskReport.score),
      RdsRequest.InputWithObject(
        "fraudRiskReportHeaders",
        Seq(
          MetadataWrapper(
            Seq(
              Map("KEY"   -> "string"),
              Map("VALUE" -> "string")
            )),
          DataWrapper(fraudRiskReportHeaders.toMap.map(header => Seq(header._1, header._2)).toSeq)
        )
      ),
      RdsRequest.InputWithObject(
        "fraudRiskReportReasons",
        Seq(
          MetadataWrapper(
            Seq(
              Map("Reason" -> "string")
            )),
          DataWrapper(fraudRiskReport.reasons.map(value => Seq(value)))
        ))
    )
  )

  def acknowledge(request: AcknowledgeReportRequest)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      // logContext: EndpointLogContext,
      correlationId: String): Future[ServiceOutcome[RdsAssessmentReport]] = {
    logger.info(s"$correlationId::[acknowledge]acknowledge")
    if (appConfig.rdsAuthRequiredForThisEnv) {
      rdsAuthConnector
        .retrieveAuthorisedBearer()
        .foldF(
          error => Future.successful(Left(ErrorWrapper(correlationId, error))),
          credentials => connector.acknowledgeRds(generateRdsAcknowledgementRequest(request), Some(credentials))
        )
    } else {
      connector.acknowledgeRds(generateRdsAcknowledgementRequest(request))
    }

  }

  private def generateRdsAcknowledgementRequest(request: AcknowledgeReportRequest): RdsRequest = {
    RdsRequest(
      Seq(
        RdsRequest.InputWithString("feedbackId", request.feedbackId),
        RdsRequest.InputWithString("nino", request.nino),
        RdsRequest.InputWithString("correlationId", request.rdsCorrelationId)
      )
    )
  }

}
