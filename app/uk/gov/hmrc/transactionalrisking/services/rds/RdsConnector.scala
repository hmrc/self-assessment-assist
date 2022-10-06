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
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.{HeaderCarrier}
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
class RdsConnector @Inject()(val wsClient: WSClient, //TODO revisit which client is recommended by HMRC
                              //val httpClient: HttpClient,
                             appConfig: AppConfig)(implicit val ec: ExecutionContext, correlationId:String) extends Logging{

  private def baseUrlForRdsAssessmentsSubmit = s"${appConfig.rdsBaseUrlForSubmit}"
  private def baseUrlToAcknowledgeRdsAssessments = s"${appConfig.rdsBaseUrlForAcknowledge}"

  //TODO move this to RDS connector
  def submit( requestSO: ServiceOutcome[RdsRequest])(implicit ec: ExecutionContext): Future[ServiceOutcome[NewRdsAssessmentReport]] = {
    requestSO match {
      case Right(ResponseWrapper(correlationId,request)) =>
        logger.debug(s"$correlationId :: RDS request content ${request}")
        wsClient
          .url(baseUrlForRdsAssessmentsSubmit)
          .post(Json.toJson(request))
          .map(response =>
            response.status match {
              case Status.OK => Right(ResponseWrapper(correlationId,response.json.validate[NewRdsAssessmentReport].get))
              case Status.NOT_FOUND => Left(ErrorWrapper(correlationId,MatchingResourcesNotFoundError))
              case unexpectedStatus => Left(ErrorWrapper(correlationId,ServiceUnavailableError))
              //TODO:DE Must get rid of throw and convert tp new error system
            }
          )
      case Left(er) => Future(Left(er):ServiceOutcome[NewRdsAssessmentReport])
    }
  }

  def acknowledgeRds(request: RdsRequest)(implicit hc: HeaderCarrier,
                                                  ec: ExecutionContext): Future[Int] =
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
            NO_CONTENT
          }

          case unexpectedStatus => {
            logger.error(s"... error during rds acknowledgement ")
            INTERNAL_SERVER_ERROR
            //            throw new RuntimeException(s"Unexpected status when attempting to mark the report as acknowledged with RDS: [$unexpectedStatus]")}
          }
        }
      )
}
