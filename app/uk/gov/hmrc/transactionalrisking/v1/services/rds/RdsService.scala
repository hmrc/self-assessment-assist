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

package uk.gov.hmrc.transactionalrisking.v1.services.rds

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transactionalrisking.config.AppConfig
import uk.gov.hmrc.transactionalrisking.utils.Logging
import uk.gov.hmrc.transactionalrisking.v1.controllers.UserRequest
import uk.gov.hmrc.transactionalrisking.v1.models.auth.RdsAuthCredentials
import uk.gov.hmrc.transactionalrisking.v1.models.domain.PreferredLanguage.PreferredLanguage
import uk.gov.hmrc.transactionalrisking.v1.models.domain.{AssessmentReport, AssessmentRequestForSelfAssessment, DesTaxYear, Link, Origin, PreferredLanguage, Risk}
import uk.gov.hmrc.transactionalrisking.v1.models.errors.{DownstreamError, ErrorWrapper, FormatReportIdError}
import uk.gov.hmrc.transactionalrisking.v1.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.v1.services.ServiceOutcome
import uk.gov.hmrc.transactionalrisking.v1.services.cip.models.FraudRiskReport
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.request.AcknowledgeReportRequest
import uk.gov.hmrc.transactionalrisking.v1.services.rds.models.request.RdsRequest
import uk.gov.hmrc.transactionalrisking.v1.services.rds.models.request.RdsRequest.{DataWrapper, MetadataWrapper}
import uk.gov.hmrc.transactionalrisking.v1.services.rds.models.response.RdsAssessmentReport

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RdsService @Inject()(rdsAuthConnector: RdsAuthConnector[Future], connector: RdsConnector, appConfig: AppConfig) extends Logging {

//TODO Refactor this code
  def submit(request: AssessmentRequestForSelfAssessment,
             fraudRiskReport: FraudRiskReport,
             origin: Origin)(implicit hc: HeaderCarrier,
                             ec: ExecutionContext,
                             //logContext: EndpointLogContext,
                             userRequest: UserRequest[_],
                             correlationId: String): Future[ServiceOutcome[AssessmentReport]] = {

    def processRdsRequest(rdsAuthCredentials: Option[RdsAuthCredentials] = None) = {
      val rdsRequestSO: ServiceOutcome[RdsRequest] = generateRdsAssessmentRequest(request, fraudRiskReport)
      rdsRequestSO match {
        case Right(ResponseWrapper(_, rdsRequest)) =>
          logger.info(s"$correlationId::[RdsService submit ] RdsAssessmentRequest Created")

          connector.submit(rdsRequest, rdsAuthCredentials).map {
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
                logger.error(s"$correlationId::[RdsService][submit] RDS connector failed Unable to generate report ${errorWrapper.error}")
                Left(errorWrapper)
          }
        case Left(errorWrapper) =>
          logger.error(s"$correlationId::[RdsService][submit] generateRdsAssessmentRequest SO failed Unable to generate report request ${errorWrapper.error}")
          Future(Left(errorWrapper): ServiceOutcome[AssessmentReport])
      }
    }

    if (appConfig.rdsAuthRequiredForThisEnv) {
      logger.info(s"$correlationId::[submit]RDS Auth Required}")
      rdsAuthConnector
        .retrieveAuthorisedBearer()
        .foldF(
          error =>
            Future.successful(Left(ErrorWrapper(correlationId, error))),
          credentials => processRdsRequest(Some(credentials))
        )
    } else {
      logger.info(s"$correlationId::[submit]RDS Auth Not Required}")
      processRdsRequest()
    }
  }

  private def toAssessmentReport(report: RdsAssessmentReport, request: AssessmentRequestForSelfAssessment, correlationId: String): ServiceOutcome[AssessmentReport] = {
    logger.info(s"$correlationId::[toAssessmentReport]Generated assessment report")

    (report.calculationId, report.feedbackId) match {
      case (Some(calculationId), Some(reportId)) =>
          val rdsCorrelationIdOption = report.rdsCorrelationId
          rdsCorrelationIdOption match {
            case Some(rdsCorrelationID) =>
              logger.info(s"$correlationId::[toAssessmentReport]Successfully generated assessment report")
              Right(ResponseWrapper(correlationId,
                AssessmentReport(reportId = reportId,
                  risks = risks(report, request.preferredLanguage, correlationId), nino = request.nino,
                  taxYear = request.taxYear, //Todo check whether format is correct
                  calculationId = request.calculationId, rdsCorrelationID)))

            case None =>
              logger.warn(s"$correlationId::[RdsService][toAssessmentReport]Unable to find rdsCorrelationId")
              Left(ErrorWrapper(correlationId, DownstreamError)): ServiceOutcome[AssessmentReport]
          }

      case (Some(_), None) =>
        logger.warn(s"$correlationId::[RdsService][toAssessmentReport]Unable to find reportId")
        Left(ErrorWrapper(correlationId, FormatReportIdError)): ServiceOutcome[AssessmentReport]

      case (None, Some(_)) =>
        logger.warn(s"$correlationId::[RdsService][toAssessmentReport] calculationId missing in RDS response")
        Left(ErrorWrapper(correlationId, DownstreamError)): ServiceOutcome[AssessmentReport]
    }
  }

  private def risks(report: RdsAssessmentReport, preferredLanguage: PreferredLanguage, correlationId: String): Seq[Risk] = {
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

  private def generateRdsAssessmentRequest(request: AssessmentRequestForSelfAssessment,
                                           fraudRiskReport: FraudRiskReport)(implicit correlationId: String,userRequest: UserRequest[_]): ServiceOutcome[RdsRequest]
  = {
    logger.info(s"$correlationId::[generateRdsAssessmentRequest]Creating a generateRdsAssessmentRequest")
    Right(ResponseWrapper(correlationId, RdsRequest(
      Seq(
        RdsRequest.InputWithString("calculationId", request.calculationId.toString),
        RdsRequest.InputWithString("nino", request.nino),
        RdsRequest.InputWithString("taxYear", request.taxYear),
        RdsRequest.InputWithString("customerType", request.customerType.toString),
        RdsRequest.InputWithString("agentRef", request.agentRef.getOrElse("")),
        RdsRequest.InputWithString("preferredLanguage", request.preferredLanguage.toString),
        RdsRequest.InputWithInt("fraudRiskReportScore", fraudRiskReport.score),
        RdsRequest.InputWithObject("fraudRiskReportHeaders",
          Seq(
            MetadataWrapper(
              Seq(
                Map("KEY" -> "string"),
                Map("VALUE" -> "string")
              )),
            DataWrapper(userRequest.headers.toMap.map(header => Seq(header._1, header._2.head)).toSeq)
          )
        ),
        RdsRequest.InputWithObject("fraudRiskReportReasons",
          Seq(
            MetadataWrapper(
              Seq(
                Map("Reason" -> "string")
              )),
            DataWrapper(fraudRiskReport.reasons.map(value => Seq(value)))
          )
        )
      )
    )
    )
    )
  }

  def acknowledge(request: AcknowledgeReportRequest)(implicit hc: HeaderCarrier,
                                                     ec: ExecutionContext,
                                                     //logContext: EndpointLogContext,
                                                     userRequest: UserRequest[_],
                                                     correlationId: String): Future[ServiceOutcome[RdsAssessmentReport]] = {
    logger.info(s"$correlationId::[acknowledge]acknowledge")
    if (appConfig.rdsAuthRequiredForThisEnv) {
      rdsAuthConnector
        .retrieveAuthorisedBearer()
        .foldF(
          error =>
            Future.successful(Left(ErrorWrapper(correlationId, error))),
          credentials => connector.acknowledgeRds(generateRdsAcknowledgementRequest(request), Some(credentials))
        )
    } else {
      connector.acknowledgeRds(generateRdsAcknowledgementRequest(request))
    }

  }

  private def generateRdsAcknowledgementRequest(request: AcknowledgeReportRequest): RdsRequest
  = {
    RdsRequest(
      Seq(
        RdsRequest.InputWithString("feedbackId", request.feedbackId),
        RdsRequest.InputWithString("nino", request.nino),
        RdsRequest.InputWithString("correlationId", request.rdsCorrelationId)
      )
    )
  }
}
