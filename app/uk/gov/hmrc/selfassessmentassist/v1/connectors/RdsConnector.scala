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

package uk.gov.hmrc.selfassessmentassist.v1.connectors

import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpClient, HttpException, UpstreamErrorResponse}
import uk.gov.hmrc.selfassessmentassist.api.models.auth.RdsAuthCredentials
import uk.gov.hmrc.selfassessmentassist.api.models.auth.RdsAuthCredentials.rdsAuthHeader
import uk.gov.hmrc.selfassessmentassist.api.models.errors._
import uk.gov.hmrc.selfassessmentassist.api.models.outcomes.ResponseWrapper
import uk.gov.hmrc.selfassessmentassist.config.AppConfig
import uk.gov.hmrc.selfassessmentassist.utils.Logging
import uk.gov.hmrc.selfassessmentassist.v1.models.request.rds.RdsRequest
import uk.gov.hmrc.selfassessmentassist.v1.models.response.rds.RdsAssessmentReport
import uk.gov.hmrc.selfassessmentassist.v1.services.ServiceOutcome

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RdsConnector @Inject() (@Named("external-http-client") val httpClient: HttpClient, appConfig: AppConfig)(implicit val ec: ExecutionContext)
    extends Logging {

  def submit(request: RdsRequest, rdsAuthCredentials: Option[RdsAuthCredentials] = None)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      correlationId: String): Future[ServiceOutcome[RdsAssessmentReport]] = {
    logger.info(s"$correlationId::[RdsConnector:submit] Before requesting report")

    def rdsAuthHeaders: Seq[(String, String)] = rdsAuthCredentials.map(rdsAuthHeader).getOrElse(Seq.empty)

    httpClient
      .POST(appConfig.rdsBaseUrlForSubmit, Json.toJson(request), headers = rdsAuthHeaders)
      .map { response =>
        logger.info(s"$correlationId::[RdsConnector:submit]RDS http response status is ${response.status}")
        response.status match {
          case CREATED =>
            response.json
              .validate[RdsAssessmentReport]
              .fold(
                e => {
                  logger.error(s"$correlationId::[RdsConnector][submit] validation failed while transforming the response $e")
                  Left(ErrorWrapper(correlationId, InternalError, Some(Seq(MtdError(InternalError.code, "unexpected response from downstream")))))
                },
                assessmentReport =>
                  assessmentReport.responseCode match {
                    case Some(CREATED) =>
                      logger.info(s"$correlationId::[RdsConnector:submit] RDS response body status code is $CREATED")
                      Right(ResponseWrapper(correlationId, assessmentReport))
                    case Some(NO_CONTENT) =>
                      logger.info(s"$correlationId::[RdsConnector:submit] RDS response body status code is $NO_CONTENT")
                      Left(ErrorWrapper(correlationId, NoAssessmentFeedbackFromRDS)) // exceptional scenario to stop processing
                    case Some(NOT_FOUND) =>
                      logger.info(s"$correlationId::[RdsConnector:submit] RDS response calculationId Not Found, body status code is $NOT_FOUND")
                      val errorMessage = assessmentReport.responseMessage.getOrElse("CalculationId Not Found")
                      logger.info(s"$correlationId::[RdsService][submit] $errorMessage")
                      Left(ErrorWrapper(correlationId, MatchingCalculationIDNotFoundError, Some(Seq(MtdError("404", errorMessage)))))
                    case Some(_) | None =>
                      logger.error(
                        s"$correlationId::[RdsService][submit] RDS unexpected response, body status code is ${assessmentReport.responseCode}")
                      Left(
                        ErrorWrapper(
                          correlationId,
                          InternalError,
                          Some(Seq(MtdError(InternalError.code, "unexpected response from downstream")))))
                  }
              )

          case BAD_REQUEST =>
            logger.error(s"$correlationId::[RdsConnector:submit] RDS response : BAD request")
            Left(ErrorWrapper(correlationId, InternalError))
          case NOT_FOUND =>
            logger.error(s"$correlationId::[RdsConnector:submit] RDS not reachable")
            Left(ErrorWrapper(correlationId, InternalError))
          case REQUEST_TIMEOUT =>
            logger.error(s"$correlationId::[RdsConnector:submit] Rds request timeout")
            Left(ErrorWrapper(correlationId, InternalError))
          case UNAUTHORIZED =>
            logger.error(s"$correlationId::[RdsConnector:submit] Rds request failed as unauthorized")
            Left(ErrorWrapper(correlationId, ForbiddenDownstreamError))
          case SERVICE_UNAVAILABLE =>
            logger.error(s"$correlationId::[RdsConnector:submit] Rds returned service unavailable")
            Left(ErrorWrapper(correlationId, InternalError))
          case unexpectedStatus @ _ =>
            logger.error(
              s"$correlationId::[RdsConnector:submit] Rds unable to submit the report due to unexpected status code returned $unexpectedStatus")
            Left(ErrorWrapper(correlationId, InternalError))
        }
      }
      .recover {
        case ex: BadRequestException =>
          logger.error(s"$correlationId::[RdsConnector:submit] RDS BadRequestException $ex")
          Left(ErrorWrapper(correlationId, InternalError))

        case ex: UpstreamErrorResponse =>
          logger.error(s"$correlationId::[RdsConnector:submit] RDS UpstreamErrorResponse $ex")
          ex.statusCode match {
            case REQUEST_TIMEOUT => Left(ErrorWrapper(correlationId, InternalError))
            case UNAUTHORIZED => Left(ErrorWrapper(correlationId, ForbiddenDownstreamError))
            case FORBIDDEN => Left(ErrorWrapper(correlationId, ForbiddenDownstreamError))
            case _ => Left(ErrorWrapper(correlationId, InternalError))
          }

        case ex: HttpException =>
          logger.error(s"$correlationId::[RdsConnector:submit] RDS HttpException $ex")
          Left(ErrorWrapper(correlationId, ServiceUnavailableError))

        case ex @ _ =>
          logger.error(s"$correlationId::[RdsConnector:submit] RDS Unknown exception $ex")
          Left(ErrorWrapper(correlationId, ServiceUnavailableError))
      }
  }

  def acknowledgeRds(request: RdsRequest, rdsAuthCredentials: Option[RdsAuthCredentials] = None)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      correlationId: String): Future[ServiceOutcome[RdsAssessmentReport]] = {
    logger.info(s"$correlationId::[RdsConnector:acknowledgeRds] acknowledge the report ${appConfig.rdsBaseUrlForAcknowledge}")

    def rdsAuthHeaders = rdsAuthCredentials.map(rdsAuthHeader).getOrElse(Seq.empty)

    httpClient
      .POST(s"${appConfig.rdsBaseUrlForAcknowledge}", Json.toJson(request), headers = rdsAuthHeaders)
      .map { response =>
        logger.info(s"$correlationId::[RdsConnector:acknowledgeRds] RDS http response status is ${response.status}")
        response.status match {
          case CREATED =>
            response.json
              .validate[RdsAssessmentReport]
              .fold(
                e => {
                  logger.error(s"$correlationId::[RdsConnector][acknowledgeRds] validation failed while transforming the response $e")
                  Left(ErrorWrapper(correlationId, InternalError, Some(Seq(MtdError(InternalError.code, "unexpected response from downstream")))))
                },
                assessmentReport =>
                  assessmentReport.responseCode match {
                    case Some(ACCEPTED) => Right(ResponseWrapper(correlationId, assessmentReport))
                    case Some(UNAUTHORIZED) =>
                      logger.error(
                        s"$correlationId::[RdsConnector:acknowledgeRds] RDS body status code is $UNAUTHORIZED unauthorized with message  ${assessmentReport.responseMessage}")
                      Left(ErrorWrapper(correlationId, ForbiddenRDSCorrelationIdError))
                    case Some(_) | None =>
                      logger.error(
                        s"$correlationId::[RdsConnector:acknowledgeRds] unexpected response WITH body status code is ${assessmentReport.responseCode} and message {${assessmentReport.responseMessage}}")
                      Left(
                        ErrorWrapper(
                          correlationId,
                          InternalError,
                          Some(Seq(MtdError(InternalError.code, "unexpected response from downstream")))))

                  }
              )
          case BAD_REQUEST =>
            logger.error(s"$correlationId::[RdsConnector:acknowledgeRds] RDS response : BAD request")
            Left(ErrorWrapper(correlationId, InternalError))
          case NOT_FOUND =>
            logger.error(s"$correlationId::[RdsConnector:acknowledgeRds] RDS not reachable")
            Left(ErrorWrapper(correlationId, InternalError))
          case UNAUTHORIZED =>
            logger.error(s"$correlationId::[RdsConnector:acknowledgeRds] Rds request failed as unauthorized")
            Left(ErrorWrapper(correlationId, ForbiddenDownstreamError))
          case REQUEST_TIMEOUT =>
            logger.error(s"$correlationId::[RdsConnector:acknowledgeRds] Rds request timeout")
            Left(ErrorWrapper(correlationId, InternalError))
          case _ => Left(ErrorWrapper(correlationId, InternalError))
        }
      }
      .recover {
        case ex: BadRequestException =>
          logger.error(s"$correlationId::[RdsConnector:acknowledgeRds] RDS BadRequestException $ex")
          Left(ErrorWrapper(correlationId, InternalError))
        case ex: HttpException =>
          logger.error(s"$correlationId::[RdsConnector:submit] RDS HttpException $ex")
          Left(ErrorWrapper(correlationId, InternalError))
        case ex: UpstreamErrorResponse =>
          logger.error(s"$correlationId::[RdsConnector:acknowledgeRds] RDS UpstreamErrorResponse $ex")
          ex.statusCode match {
            case REQUEST_TIMEOUT => Left(ErrorWrapper(correlationId, InternalError))
            case UNAUTHORIZED => Left(ErrorWrapper(correlationId, ForbiddenDownstreamError))
            case FORBIDDEN => Left(ErrorWrapper(correlationId, ForbiddenDownstreamError))
            case NOT_FOUND => Left(ErrorWrapper(correlationId, InternalError))
            case _ => Left(ErrorWrapper(correlationId, InternalError))
          }
        case ex @ _ =>
          logger.error(s"$correlationId::[RdsConnector:acknowledgeRds] RDS Unknown exception $ex")
          Left(ErrorWrapper(correlationId, InternalError))
      }
  }

}
