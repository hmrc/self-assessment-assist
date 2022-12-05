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

package uk.gov.hmrc.transactionalrisking.v1.services.rds

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transactionalrisking.config.AppConfig
import uk.gov.hmrc.transactionalrisking.utils.Logging
import uk.gov.hmrc.transactionalrisking.v1.controllers.UserRequest
import uk.gov.hmrc.transactionalrisking.v1.models.auth.RdsAuthCredentials
import uk.gov.hmrc.transactionalrisking.v1.models.domain.PreferredLanguage.PreferredLanguage
import uk.gov.hmrc.transactionalrisking.v1.models.domain.{AssessmentReport, AssessmentRequestForSelfAssessment, DesTaxYear, FraudRiskReport, Link, Origin, PreferredLanguage, Risk}
import uk.gov.hmrc.transactionalrisking.v1.models.errors.{ErrorWrapper, FormatReportIdError, ResourceNotFoundError}
import uk.gov.hmrc.transactionalrisking.v1.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.v1.services.ServiceOutcome
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.request.AcknowledgeReportRequest
import uk.gov.hmrc.transactionalrisking.v1.services.rds.models.request.RdsRequest
import uk.gov.hmrc.transactionalrisking.v1.services.rds.models.request.RdsRequest.{DataWrapper, MetadataWrapper}
import uk.gov.hmrc.transactionalrisking.v1.services.rds.models.response.NewRdsAssessmentReport

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RdsService @Inject()(rdsAuthConnector: RdsAuthConnector[Future], connector: RdsConnector, appConfig: AppConfig) extends Logging {


  def submit(request: AssessmentRequestForSelfAssessment,
             fraudRiskReport: FraudRiskReport,
             origin: Origin)(implicit hc: HeaderCarrier,
                             ec: ExecutionContext,
                             //logContext: EndpointLogContext,
                             userRequest: UserRequest[_],
                             correlationID: String): Future[ServiceOutcome[AssessmentReport]] = {

    def processRdsRequest(rdsAuthCredentials: Option[RdsAuthCredentials] = None) = {
      val rdsRequestSO: ServiceOutcome[RdsRequest] = generateRdsAssessmentRequest(request, fraudRiskReport)
      rdsRequestSO match {
        case Right(ResponseWrapper(correlationIdResponse, rdsRequest)) =>
          logger.info(s"$correlationID::[RdsService submit ] RdsAssessmentRequest Created")
          val submit = connector.submit(rdsRequest, rdsAuthCredentials)
          val ret = submit.map {
            _ match {
              case Right(ResponseWrapper(correlationIdResponse, rdsResponse)) =>
                val assessmentReportSO = toAssessmentReport(rdsResponse, request, correlationID)
                assessmentReportSO match {
                  case Right(ResponseWrapper(correlationIdResponse, assessmentReport)) =>
                    logger.info(s"$correlationID::[submit]submit request for report successful returning it")
                    Right(ResponseWrapper(correlationID, assessmentReport))

                  case Left(errorWrapper) =>
                    logger.warn(s"$correlationID::[RdsService][submit]submit request for report error from service ${errorWrapper.error}")
                    Left(errorWrapper)
                }
              case Left(errorWrapper) =>
                logger.warn(s"$correlationID::[RdsService][submit] RDS connector failed Unable to generate report ${errorWrapper.error}")
                Left(errorWrapper)
            }
          }
          ret
        case Left(errorWrapper) =>
          logger.warn(s"$correlationID::[RdsService][submit] generateRdsAssessmentRequest SO failed Unable to generate report request ${errorWrapper.error}")
          Future(Left(errorWrapper): ServiceOutcome[AssessmentReport])
      }
    }

    logger.info(s"$correlationID::[submit]submit request for report}")

    if (appConfig.rdsAuthRequiredForThisEnv) {
      logger.info(s"$correlationID::[submit]RDS Auth Required}")
      rdsAuthConnector
        .retrieveAuthorisedBearer()
        .foldF(
          error =>
            Future.successful(Left(ErrorWrapper(correlationID, error))),
          credentials => processRdsRequest(Some(credentials))
        )
    } else {
      logger.info(s"$correlationID::[submit]RDS Auth Not Required}")
      processRdsRequest()
    }
  }

  private def toAssessmentReport(report: NewRdsAssessmentReport, request: AssessmentRequestForSelfAssessment, correlationID: String): ServiceOutcome[AssessmentReport] = {
    logger.info(s"$correlationID::[toAssessmentReport]Generated assessment report")

    val feedbackIDOption = report.feedbackId
    feedbackIDOption match {
      case Some(reportID) =>
        val rdsCorrelationIdOption = report.rdsCorrelationId
        rdsCorrelationIdOption match {
          case Some(rdsCorrelationID) =>
            logger.info(s"$correlationID::[toAssessmentReport]Successfully generated assessment report")
            Right(ResponseWrapper(correlationID,
              AssessmentReport(reportID = reportID,
                risks = risks(report, request.preferredLanguage, correlationID), nino = request.nino,
                taxYear = DesTaxYear.fromDesIntToString(request.taxYear.toInt),
                calculationID = request.calculationID, rdsCorrelationID)))

          case None =>
            logger.warn(s"$correlationID::[RdsService][toAssessmentReport]Unable to find rdsCorrelationId")
            Left(ErrorWrapper(correlationID, ResourceNotFoundError)): ServiceOutcome[AssessmentReport]
        }

      case None =>
        logger.warn(s"$correlationID::[RdsService][toAssessmentReport]Unable to find reportId")
        Left(ErrorWrapper(correlationID, FormatReportIdError)): ServiceOutcome[AssessmentReport]
    }
  }

  private def risks(report: NewRdsAssessmentReport, preferredLanguage: PreferredLanguage, correlationID: String): Seq[Risk] = {
    logger.info(s"$correlationID::[risks]Create risk for $preferredLanguage.Value")
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
                                           fraudRiskReport: FraudRiskReport)(implicit correlationID: String): ServiceOutcome[RdsRequest]
  = {
    logger.info(s"$correlationID::[generateRdsAssessmentRequest]Creating a generateRdsAssessmentRequest")
    //TODO Errors need to be dealt looked at.
    Right(ResponseWrapper(correlationID, RdsRequest(
      Seq(
        RdsRequest.InputWithString("calculationID", request.calculationID.toString),
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
    )
  }

  def acknowledge(request: AcknowledgeReportRequest)(implicit hc: HeaderCarrier,
                                                     ec: ExecutionContext,
                                                     //logContext: EndpointLogContext,
                                                     userRequest: UserRequest[_],
                                                     correlationId: String): Future[ServiceOutcome[NewRdsAssessmentReport]] = {
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
        RdsRequest.InputWithString("feedbackID", request.feedbackID),
        RdsRequest.InputWithString("nino", request.nino),
        RdsRequest.InputWithString("correlationID", request.rdsCorrelationID)
      )
    )
  }
}
