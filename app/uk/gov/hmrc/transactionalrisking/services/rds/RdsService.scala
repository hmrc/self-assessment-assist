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

package uk.gov.hmrc.transactionalrisking.services.rds

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transactionalrisking.config.AppConfig
import uk.gov.hmrc.transactionalrisking.controllers.UserRequest
import uk.gov.hmrc.transactionalrisking.models.domain.PreferredLanguage.PreferredLanguage
import uk.gov.hmrc.transactionalrisking.models.domain.{AssessmentReport, AssessmentRequestForSelfAssessment, FraudRiskReport, Link, Origin, PreferredLanguage, Risk}
import uk.gov.hmrc.transactionalrisking.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.services.ServiceOutcome
import uk.gov.hmrc.transactionalrisking.services.nrs.models.request.AcknowledgeReportRequest
import uk.gov.hmrc.transactionalrisking.services.rds.models.request.RdsRequest
import uk.gov.hmrc.transactionalrisking.services.rds.models.request.RdsRequest.{DataWrapper, MetadataWrapper}
import uk.gov.hmrc.transactionalrisking.services.rds.models.response.NewRdsAssessmentReport
import uk.gov.hmrc.transactionalrisking.utils.Logging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RdsService @Inject()(connector: RdsConnector) extends Logging {


  //TODO Pending
  def submit(request: AssessmentRequestForSelfAssessment,
             fraudRiskReport: FraudRiskReport,
             origin: Origin)(implicit hc: HeaderCarrier,
                             ec: ExecutionContext,
                             //logContext: EndpointLogContext,
                             userRequest: UserRequest[_],
                             correlationId: String): Future[ServiceOutcome[AssessmentReport]] = {
    connector.submit(generateRdsAssessmentRequest(request, fraudRiskReport))
      .map { _ match {
          case Right(ResponseWrapper(correlationId,rdsResponse)) =>
            val assessmentReport = toAssessmentReport(rdsResponse, request)
            logger.info("... returning it.")
            // TODO: Should we also audit an explicit event for actually generating the assessment?
            Right(ResponseWrapper(correlationId, assessmentReport)): ServiceOutcome[AssessmentReport]
           //TODO:DE deal with Errors.
          case Left(errorWrapper) => Left(errorWrapper): ServiceOutcome[AssessmentReport]
        }
      }
  }

  private def toAssessmentReport(report: NewRdsAssessmentReport, request: AssessmentRequestForSelfAssessment) = {
    //TODO check should this be calculationId or feedbackId?
    AssessmentReport(reportId = report.calculationId,
      risks = risks(report, request.preferredLanguage), nino = request.nino, taxYear = request.taxYear,
      calculationId = request.calculationId,report.rdsCorrelationID.toString)
  }

  private def risks(report: NewRdsAssessmentReport, preferredLanguage: PreferredLanguage): Seq[Risk] = {
    report.outputs.collect {
      case elm: NewRdsAssessmentReport.MainOutputWrapper if isPreferredLanguage(elm.name, preferredLanguage) => elm
    }.flatMap(_.value).collect {
      case value: NewRdsAssessmentReport.DataWrapper => value
    }.flatMap(_.data)
      .map(toRisk)
  }

  private def isPreferredLanguage(language: String, preferredLanguage: PreferredLanguage) = preferredLanguage match {
    case PreferredLanguage.English if language == "englishActions" => true
    case PreferredLanguage.Welsh if language == "welshActions" => true
    case _ => false
  }

  private def toRisk(riskParts: Seq[String]) =
    Risk(title = riskParts(0),
      body = riskParts(1), action = riskParts(2),
      links = Seq(Link(riskParts(3), riskParts(4))), path = riskParts(5))

  private def generateRdsAssessmentRequest(request: AssessmentRequestForSelfAssessment,
                                           fraudRiskReport: FraudRiskReport)(implicit correlationId: String): ServiceOutcome[RdsRequest]
  = {
    Right(ResponseWrapper(correlationId,RdsRequest(
      Seq(
        RdsRequest.InputWithString("calculationId", request.calculationId.toString),
        RdsRequest.InputWithString("nino", request.nino),
        RdsRequest.InputWithString("taxYear", request.taxYear),
        RdsRequest.InputWithString("customerType", request.customerType.toString),
        RdsRequest.InputWithString("agentRef", request.agentRef.getOrElse("")),
        RdsRequest.InputWithString("preferredLanguage", request.preferredLanguage.toString),
        RdsRequest.InputWithString("fraudRiskReportDecision", fraudRiskReport.decision.toString),
        RdsRequest.InputWithInt("fraudRiskReportScore", fraudRiskReport.score),
        RdsRequest.InputWithObject("fraudRiskReportHeaders",
          Seq(
            MetadataWrapper(
              Seq(
                Map("KEY" -> "string"),
                Map("VALUE" -> "string")
              )),
            DataWrapper(fraudRiskReport.headers.map(header => Seq(header.key, header.value)).toSeq)
          )
        ),
        RdsRequest.InputWithObject("fraudRiskReportWatchlistFlags",
          Seq(
            MetadataWrapper(
              Seq(
                Map("NAME" -> "string")
              )),
            DataWrapper(fraudRiskReport.watchlistFlags.map(flag => Seq(flag.name)).toSeq)
          )
        )
      )
    )
    )
    ): ServiceOutcome[RdsRequest]
  }

  def acknowlege(request: AcknowledgeReportRequest)(implicit hc: HeaderCarrier,
                                                    ec: ExecutionContext,
                                                    //logContext: EndpointLogContext,
                                                    userRequest: UserRequest[_],
                                                    correlationId: String): Future[Int] =
    connector.acknowledgeRds(generateRdsAcknowledgementRequest(request))

  private def generateRdsAcknowledgementRequest(request: AcknowledgeReportRequest): RdsRequest
  = RdsRequest(
    Seq(
      RdsRequest.InputWithString("feedbackId", request.feedbackId),
      RdsRequest.InputWithString("nino", request.nino)
    )
  )
}
