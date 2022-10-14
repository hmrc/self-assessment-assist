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
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transactionalrisking.config.AppConfig
import uk.gov.hmrc.transactionalrisking.models.errors.{DownstreamError, ErrorWrapper, MatchingResourcesNotFoundError, ServiceUnavailableError}
import uk.gov.hmrc.transactionalrisking.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.services.ServiceOutcome
import uk.gov.hmrc.transactionalrisking.services.rds.models.request.RdsRequest
import uk.gov.hmrc.transactionalrisking.services.rds.models.response.NewRdsAssessmentReport
import uk.gov.hmrc.transactionalrisking.utils.Logging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RdsConnector @Inject()(val wsClient: WSClient, //TODO revisit which client is recommended by HMRC
                              //val httpClient: HttpClient,
                             appConfig: AppConfig)(implicit val ec: ExecutionContext, correlationId:String) extends Logging{

  private def baseUrlForRdsAssessmentsSubmit = s"${appConfig.rdsBaseUrlForSubmit}"
  private def baseUrlToAcknowledgeRdsAssessments = s"${appConfig.rdsBaseUrlForAcknowledge}"


  def submit(request: RdsRequest)(implicit ec: ExecutionContext): Future[ServiceOutcome[NewRdsAssessmentReport]] = {
    logger.debug(s"$correlationId :: RDS request content ${request}")
    wsClient
      .url(baseUrlForRdsAssessmentsSubmit)
      .post(Json.toJson(request))
      .map(response =>
        response.status match {
          case Status.OK => Right(ResponseWrapper(correlationId, response.json.validate[NewRdsAssessmentReport].get))
          case Status.NOT_FOUND => Left(ErrorWrapper(correlationId, MatchingResourcesNotFoundError))
          case unexpectedStatus => Left(ErrorWrapper(correlationId, ServiceUnavailableError))
        }
      )
  }


  def acknowledgeRds(request: RdsRequest)(implicit hc: HeaderCarrier,
                                                  ec: ExecutionContext,
                                                  correlationId:String
                                          ): Future[ ServiceOutcome[ NewRdsAssessmentReport ]  ] =
    wsClient
      .url(baseUrlToAcknowledgeRdsAssessments)
      .post(Json.toJson(request))
      .map(response =>
        response.status match {
          case Status.OK => {
            logger.info(s"... submitting acknowledgement to RDS success")
            //no need to validate as we are interested only in OK response.if validation is required then
            // we need separate class, as the structure is different, ignore response as only report id needs to go into the body of nrs
            //            response.json.validate[RdsAcknowledgementResponse].getOrElse(throw new RuntimeException("failed to validate "))
            //Right(ResponseWrapper( correlationId, AcknowledgeReport( NO_CONTENT, 2022 ) ) )

            val ret = response.json.validate[NewRdsAssessmentReport] match {
              case  JsSuccess(newRdsAssessmentReport, _)  =>
                Right( ResponseWrapper(correlationId, newRdsAssessmentReport ) )
              case JsError(e) =>
                Left (ErrorWrapper (correlationId, DownstreamError) )
            }
            ret
          }

          case unexpectedStatus => {
            logger.error(s"... error during rds acknowledgement ")
            Left(ErrorWrapper(correlationId, DownstreamError ))
          }
        }
      )
}
