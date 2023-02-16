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

package uk.gov.hmrc.selfassessmentassist.v1.services.rds

import play.api.http.Status.{ACCEPTED, BAD_REQUEST, CREATED, FORBIDDEN, NOT_FOUND, NO_CONTENT, REQUEST_TIMEOUT, SERVICE_UNAVAILABLE, UNAUTHORIZED}
import play.api.libs.json.Json
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpClient, HttpException, UpstreamErrorResponse}
import uk.gov.hmrc.selfassessmentassist.v1.models.auth.RdsAuthCredentials
import uk.gov.hmrc.selfassessmentassist.v1.models.auth.RdsAuthCredentials.rdsAuthHeader
import uk.gov.hmrc.selfassessmentassist.v1.models.errors.{DownstreamError, ForbiddenDownstreamError}

import javax.inject.Named
import uk.gov.hmrc.selfassessmentassist.config.AppConfig
import uk.gov.hmrc.selfassessmentassist.utils.Logging
import uk.gov.hmrc.selfassessmentassist.v1.models.errors._
import uk.gov.hmrc.selfassessmentassist.v1.models.outcomes.ResponseWrapper
import uk.gov.hmrc.selfassessmentassist.v1.services.ServiceOutcome
import uk.gov.hmrc.selfassessmentassist.v1.services.rds.models.request.RdsRequest
import uk.gov.hmrc.selfassessmentassist.v1.services.rds.models.response.RdsAssessmentReport

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

@Singleton
class RdsConnector @Inject()(@Named("external-http-client") val httpClient: HttpClient,
                             appConfig: AppConfig)(implicit val ec: ExecutionContext) extends Logging {

  def submit(request: RdsRequest, rdsAuthCredentials: Option[RdsAuthCredentials] = None)(implicit hc: HeaderCarrier, ec: ExecutionContext, correlationId: String): Future[ServiceOutcome[RdsAssessmentReport]] = {
    logger.info(s"$correlationId::[RdsConnector:submit] Before requesting report")

    def rdsAuthHeaders: Seq[(String, String)] = rdsAuthCredentials.map(rdsAuthHeader(_)).getOrElse(Seq.empty)

    httpClient
      .POST(appConfig.rdsBaseUrlForSubmit, Json.toJson(request), headers = rdsAuthHeaders)
      .map { response =>
        logger.info(s"$correlationId::[RdsConnector:submit]RDS http response status is ${response.status}")
        response.status match {
          case CREATED =>
            response.json.validate[RdsAssessmentReport].fold(
              e => {
                logger.warn(s"$correlationId::[RdsService][submit] validation failed while transforming the response")
                Left(ErrorWrapper(correlationId, DownstreamError, Some(Seq(MtdError(DownstreamError.code, "unexpected response from downstream")))))
              },
              assessmentReport =>  assessmentReport.responseCode match {
                case Some(CREATED)  => Right(ResponseWrapper(correlationId, assessmentReport))
                case Some(NO_CONTENT) => Left(ErrorWrapper(correlationId, NoAssessmentFeedbackFromRDS))//exceptional scenario to stop processing
                case Some(NOT_FOUND) =>
                  val errorMessage = assessmentReport.responseMessage.getOrElse("Calculation Not Found")
                  logger.info(s"$correlationId::[RdsService][submit] $errorMessage")
                  Left(ErrorWrapper(correlationId, MatchingResourcesNotFoundError, Some(Seq(MtdError("404", errorMessage)))))
                case Some(_) | None =>
                  logger.error(s"$correlationId::[RdsService][submit] unexpected response")
                  Left(ErrorWrapper(correlationId, DownstreamError, Some(Seq(MtdError(DownstreamError.code, "unexpected response from downstream")))))
              }
            )

          case BAD_REQUEST =>
            logger.error(s"$correlationId::[RdsConnector:submit] RDS response : BAD request")
            Left(ErrorWrapper(correlationId, DownstreamError))
          case NOT_FOUND =>
            logger.error(s"$correlationId::[RdsConnector:submit] RDS not reachable")
            Left(ErrorWrapper(correlationId, DownstreamError))
          case REQUEST_TIMEOUT =>  Left(ErrorWrapper(correlationId, DownstreamError))
          case UNAUTHORIZED => Left(ErrorWrapper(correlationId, ForbiddenDownstreamError))
          case SERVICE_UNAVAILABLE => Left(ErrorWrapper(correlationId, DownstreamError))
          case unexpectedStatus@_ =>
            logger.error(s"$correlationId::[RdsConnector:submit]Unable to submit the report due to unexpected status code returned $unexpectedStatus")
            Left(ErrorWrapper(correlationId, ServiceUnavailableError))
        }
      }
      .recover {
        case ex: BadRequestException =>
          logger.error(s"$correlationId::[RdsConnector:submit] BadRequestException $ex")
          Left(ErrorWrapper(correlationId, DownstreamError))

        case ex: UpstreamErrorResponse =>
          logger.error(s"$correlationId::[RdsConnector:submit] UpstreamErrorResponse $ex")
          ex.statusCode match {
            case REQUEST_TIMEOUT => Left(ErrorWrapper(correlationId, DownstreamError))
            case UNAUTHORIZED => Left(ErrorWrapper(correlationId, ForbiddenDownstreamError))
            case FORBIDDEN => Left(ErrorWrapper(correlationId, ForbiddenDownstreamError))
            case _ => Left(ErrorWrapper(correlationId, DownstreamError))
          }

        case ex: HttpException =>
          logger.error(s"$correlationId::[RdsConnector:submit] HttpException $ex")
          Left(ErrorWrapper(correlationId, ServiceUnavailableError))

        case ex@_ =>
          logger.error(s"$correlationId::[RdsConnector:submit] Unknown exception $ex")
          Left(ErrorWrapper(correlationId, ServiceUnavailableError))
      }
  }


  def acknowledgeRds(request: RdsRequest, rdsAuthCredentials: Option[RdsAuthCredentials] = None)(implicit hc: HeaderCarrier,
                                                                                                 ec: ExecutionContext,
                                                                                                 correlationId: String
  ): Future[ServiceOutcome[RdsAssessmentReport]] = {
    logger.info(s"$correlationId::[RdsConnector:acknowledgeRds] acknowledge the report ${appConfig.rdsBaseUrlForAcknowledge}")

    def rdsAuthHeaders = rdsAuthCredentials.map(rdsAuthHeader(_)).getOrElse(Seq.empty)

    httpClient
      .POST(s"${appConfig.rdsBaseUrlForAcknowledge}", Json.toJson(request), headers = rdsAuthHeaders)
      .map { response =>
        logger.info(s"$correlationId::[RdsConnector:acknowledgeRds] response is ${response.status}")
        response.status match {
          case CREATED =>
            val assessmentReport = response.json.validate[RdsAssessmentReport].get
            assessmentReport.responseCode match {
              case Some(ACCEPTED)       => Right(ResponseWrapper(correlationId, assessmentReport))
              case Some(UNAUTHORIZED)   => Left(ErrorWrapper(correlationId,ForbiddenDownstreamError))
              case Some(_) | None       =>
                logger.warn(s"$correlationId::[RdsConnector:acknowledgeRds] unexpected response")
                Left(ErrorWrapper(correlationId, DownstreamError, Some(Seq(MtdError(DownstreamError.code, "unexpected response from downstream")))))

            }
          case BAD_REQUEST      => Left(ErrorWrapper(correlationId,DownstreamError))
          case NOT_FOUND        => Left(ErrorWrapper(correlationId, DownstreamError))
          case UNAUTHORIZED     => Left(ErrorWrapper(correlationId, ForbiddenDownstreamError))
          case REQUEST_TIMEOUT  => Left(ErrorWrapper(correlationId, DownstreamError))
          case _                => Left(ErrorWrapper(correlationId, DownstreamError))
        }
      }
      .recover {
        case ex: BadRequestException =>
          logger.error(s"$correlationId::[RdsConnector:acknowledgeRds] BadRequestException $ex")
          Left(ErrorWrapper(correlationId, DownstreamError))
        case _: HttpException => Left(ErrorWrapper(correlationId, ServiceUnavailableError))
        case ex: UpstreamErrorResponse =>
          logger.error(s"$correlationId::[RdsConnector:acknowledgeRds] UpstreamErrorResponse $ex")
          ex.statusCode match {
            case REQUEST_TIMEOUT  => Left(ErrorWrapper(correlationId, DownstreamError))
            case UNAUTHORIZED     => Left(ErrorWrapper(correlationId, ForbiddenDownstreamError))
            case FORBIDDEN        => Left(ErrorWrapper(correlationId, ForbiddenDownstreamError))
            case NOT_FOUND        => Left(ErrorWrapper(correlationId, DownstreamError))
            case _                => Left(ErrorWrapper(correlationId, DownstreamError))
          }
        case ex@_ =>
          logger.error(s"$correlationId::[RdsConnector:acknowledgeRds] Unknown exception $ex")
          Left(ErrorWrapper(correlationId, ServiceUnavailableError))
      }
  }
}
