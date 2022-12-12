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

import play.api.http.Status
import play.api.http.Status.{CREATED, NOT_FOUND, OK}
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpClient, HttpException, UpstreamErrorResponse}
import uk.gov.hmrc.transactionalrisking.v1.models.auth.RdsAuthCredentials
import uk.gov.hmrc.transactionalrisking.v1.models.auth.RdsAuthCredentials.rdsAuthHeader
import uk.gov.hmrc.transactionalrisking.v1.models.errors.{DownstreamError, ForbiddenDownstreamError, ResourceNotFoundError}

import javax.inject.Named
//import uk.gov.hmrc.http.{HttpClient}
import uk.gov.hmrc.transactionalrisking.config.AppConfig
import uk.gov.hmrc.transactionalrisking.utils.Logging
import uk.gov.hmrc.transactionalrisking.v1.models.errors._
import uk.gov.hmrc.transactionalrisking.v1.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.v1.services.ServiceOutcome
import uk.gov.hmrc.transactionalrisking.v1.services.rds.models.request.RdsRequest
import uk.gov.hmrc.transactionalrisking.v1.services.rds.models.response.RdsAssessmentReport

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

//TODO Revisit implicits at class level, correaltionId is definetly not needed, its present in function
@Singleton
class RdsConnector @Inject()(@Named("nohook-auth-http-client") val httpClient: HttpClient,
                             appConfig: AppConfig)(implicit val ec: ExecutionContext) extends Logging {

  def submit(request: RdsRequest, rdsAuthCredentials: Option[RdsAuthCredentials]=None)(implicit hc: HeaderCarrier, ec: ExecutionContext, correlationId: String): Future[ServiceOutcome[RdsAssessmentReport]] = {
    logger.info(s"$correlationId::[RdsConnector:submit] Before requesting report")

    def rdsAuthHeaders = rdsAuthCredentials.map(rdsAuthHeader(_)).getOrElse(Seq.empty)
    logger.info(s"$correlationId::[RdsConnector:submit]======= invoking url ${appConfig.rdsBaseUrlForSubmit}")
    httpClient
      .POST(s"${appConfig.rdsBaseUrlForSubmit}", Json.toJson(request), headers = rdsAuthHeaders)
      .map { response =>
        response.status match {
          case Status.CREATED =>
            logger.debug(s"$correlationId::[RdsConnector:submit]Successfully submitted the report response is ${response.body}")
            Right(ResponseWrapper(correlationId, response.json.validate[RdsAssessmentReport].get))
          case Status.NOT_FOUND =>
            logger.warn(s"$correlationId::[RdsConnector:submit]Unable to submit the report - not found")
            Left(ErrorWrapper(correlationId, MatchingResourcesNotFoundError))
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
          Left(ErrorWrapper(correlationId, ForbiddenDownstreamError))

        case ex: HttpException =>
          logger.error(s"$correlationId::[RdsConnector:submit] HttpException $ex")
          Left(ErrorWrapper(correlationId, ServiceUnavailableError))

        case ex@_ =>
          logger.error(s"$correlationId::[RdsConnector:submit] Unknown exception $ex")
          Left(ErrorWrapper(correlationId, ServiceUnavailableError))
      }
  }


  def acknowledgeRds(request: RdsRequest, rdsAuthCredentials: Option[RdsAuthCredentials]=None)(implicit hc: HeaderCarrier,
                                                                                               ec: ExecutionContext,
                                                                                               correlationId: String
  ): Future[ServiceOutcome[RdsAssessmentReport]] = {
    logger.info(s"$correlationId::[acknowledgeRds]acknowledge the report ${appConfig.rdsBaseUrlForAcknowledge}")
    def rdsAuthHeaders = rdsAuthCredentials.map(rdsAuthHeader(_)).getOrElse(Seq.empty)

    httpClient
      .POST(s"${appConfig.rdsBaseUrlForAcknowledge}", Json.toJson(request), headers = rdsAuthHeaders)
      .map { response =>
        logger.info(s"$correlationId::[acknowledgeRds] response is")
        logger.info(s"$correlationId::[acknowledgeRds] response is $response")
        logger.info(s"$correlationId::[acknowledgeRds] response body ${response.headers}")
        response.status match {
          case code@OK =>
            logger.debug(s"$correlationId::[acknowledgeRds] acknowledgement OK response ")
            response.json.validate[RdsAssessmentReport] match {
              case JsSuccess(newRdsAssessmentReport, _) =>
                logger.info(s"$correlationId::[acknowledgeRds] OK")
                Right(ResponseWrapper(correlationId, newRdsAssessmentReport))
              case JsError(e) =>
                logger.warn(s"$correlationId::[acknowledgeRds] OK results validation failed with $e")
                Left(ErrorWrapper(correlationId, DownstreamError))
            }
          case code@CREATED =>
            logger.debug(s"$correlationId::[acknowledgeRds] acknowledgement to RDS successful with response $code")
            response.json.validate[RdsAssessmentReport] match {
              case JsSuccess(newRdsAssessmentReport, _) =>
                logger.info(s"$correlationId::[acknowledgeRds] response $code ")
                Right(ResponseWrapper(correlationId, newRdsAssessmentReport))
              case JsError(e) =>
                logger.warn(s"$correlationId::[acknowledgeRds]Unable to validate the returned results failed with $e")
                Left(ErrorWrapper(correlationId, DownstreamError))
            }
          case code@NOT_FOUND =>
            logger.error(s"$correlationId::[acknowledgeRds]not found error during rds acknowledgement $code")
            Left(ErrorWrapper(correlationId, ResourceNotFoundError))
          case _@errorCode =>
            logger.error(s"$correlationId::[acknowledgeRds]error during rds acknowledgement $errorCode")
            Left(ErrorWrapper(correlationId, DownstreamError))
        }
      }
      .recover {
        case ex: HttpException => Left(ErrorWrapper(correlationId, ServiceUnavailableError))
        case ex: UpstreamErrorResponse => Left(ErrorWrapper(correlationId, ForbiddenDownstreamError))
      }
  }
}
