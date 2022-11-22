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

import play.api.http.Status
import play.api.http.Status.{CREATED, NOT_FOUND}
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException, UpstreamErrorResponse}
import uk.gov.hmrc.transactionalrisking.models.auth.RdsAuthCredentials
import uk.gov.hmrc.transactionalrisking.models.auth.RdsAuthCredentials.rdsAuthHeader
import uk.gov.hmrc.transactionalrisking.models.errors.{DownstreamError, ForbiddenDownstreamError, ResourceNotFoundError}
import javax.inject.Named
//import uk.gov.hmrc.http.{HttpClient}
import uk.gov.hmrc.transactionalrisking.config.AppConfig
import uk.gov.hmrc.transactionalrisking.models.errors.{ErrorWrapper, MatchingResourcesNotFoundError, ServiceUnavailableError}
import uk.gov.hmrc.transactionalrisking.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.services.ServiceOutcome
import uk.gov.hmrc.transactionalrisking.services.rds.models.request.RdsRequest
import uk.gov.hmrc.transactionalrisking.services.rds.models.response.NewRdsAssessmentReport
import uk.gov.hmrc.transactionalrisking.utils.Logging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RdsConnector @Inject()(@Named("nohook-auth-http-client") val httpClient: HttpClient,
                             appConfig: AppConfig)(implicit val ec: ExecutionContext, correlationID: String) extends Logging {

  def submit(request: RdsRequest, rdsAuthCredentials: Option[RdsAuthCredentials]=None)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ServiceOutcome[NewRdsAssessmentReport]] = {
    logger.info(s"$correlationID::[submit]submit the report")

    def rdsAuthHeaders = rdsAuthCredentials.map(rdsAuthHeader(_)).getOrElse(Seq.empty)

    httpClient
      .POST(s"${appConfig.rdsBaseUrlForSubmit}", Json.toJson(request), headers = rdsAuthHeaders)
      .map { response =>
        logger.info(s"======= response is $response")
        response.status match {
          case Status.OK =>
            logger.info(s"$correlationID::[submit]Successfully submitted the report")
            Right(ResponseWrapper(correlationID, response.json.validate[NewRdsAssessmentReport].get))
          case Status.NOT_FOUND =>
            logger.warn(s"$correlationID::[submit]Unable to submit the report")
            Left(ErrorWrapper(correlationID, MatchingResourcesNotFoundError))
          case unexpectedStatus =>
            logger.error(s"$correlationID::[submit]Unable to submit the report due to unexpected status code returned")
            Left(ErrorWrapper(correlationID, ServiceUnavailableError))
        }
      }
      .recover {
        case ex: HttpException => Left(ErrorWrapper(correlationID, ServiceUnavailableError))
        case ex: UpstreamErrorResponse => Left(ErrorWrapper(correlationID, ForbiddenDownstreamError))
      }
  }


  def acknowledgeRds(request: RdsRequest, rdsAuthCredentials: Option[RdsAuthCredentials]=None)(implicit hc: HeaderCarrier,
                                                                                  ec: ExecutionContext,
                                                                                  correlationID: String
  ): Future[ServiceOutcome[NewRdsAssessmentReport]] = {
    logger.info(s"$correlationID::[acknowledgeRds]acknowledge the report")
    def rdsAuthHeaders = rdsAuthCredentials.map(rdsAuthHeader(_)).getOrElse(Seq.empty)

    httpClient
      .POST(s"${appConfig.rdsBaseUrlForAcknowledge}", Json.toJson(request), headers = rdsAuthHeaders)
      .map { response =>
        response.status match {
          case code@CREATED =>
            logger.debug(s"$correlationID::[acknowledgeRds] submitting acknowledgement to RDS success")
            //no need to validate as we are interested only in OK response.if validation is required then
            // we need separate class, as the structure is different, ignore response as only report id needs to go into the body of nrs
            //            response.json.validate[RdsAcknowledgementResponse].getOrElse(throw new RuntimeException("failed to validate "))
            //Right(ResponseWrapper( correlationId, AcknowledgeReport( NO_CONTENT, 2022 ) ) )

            response.json.validate[NewRdsAssessmentReport] match {
              case JsSuccess(newRdsAssessmentReport, _) =>
                logger.info(s"$correlationID::[acknowledgeRds]the results return are ok")
                Right(ResponseWrapper(correlationID, newRdsAssessmentReport))
              case JsError(e) =>
                logger.warn(s"$correlationID::[acknowledgeRds]Unable to validate the returned results failed with $e")
                Left(ErrorWrapper(correlationID, DownstreamError))
            }
          case code@NOT_FOUND =>
            logger.error(s"[acknowledgeRds]not found error during rds acknowledgement $code")
            Left(ErrorWrapper(correlationID, ResourceNotFoundError))
          case _@errorCode =>
            logger.error(s"[acknowledgeRds]error during rds acknowledgement $errorCode")
            Left(ErrorWrapper(correlationID, DownstreamError))
        }
      }
      .recover {
        case ex: HttpException => Left(ErrorWrapper(correlationID, ServiceUnavailableError))
        case ex: UpstreamErrorResponse => Left(ErrorWrapper(correlationID, ForbiddenDownstreamError))
      }
  }
}
