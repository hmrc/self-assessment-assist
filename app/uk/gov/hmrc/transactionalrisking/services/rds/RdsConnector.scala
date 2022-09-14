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
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.transactionalrisking.config.AppConfig
import uk.gov.hmrc.transactionalrisking.services.rds.models.request.RdsRequest
import uk.gov.hmrc.transactionalrisking.services.rds.models.response.NewRdsAssessmentReport
import uk.gov.hmrc.transactionalrisking.utils.Logging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RdsConnector @Inject()(val wsClient: WSClient, //TODO revisit which client is recommended by HMRC
                              //val httpClient: HttpClient,
                             appConfig: AppConfig)(implicit val ec: ExecutionContext) extends Logging{

  private def baseUrlForRdsAssessmentsSubmit = s"${appConfig.rdsBaseUrlForSubmit}"
  private def baseUrlToAcknowledgeRdsAssessments = s"${appConfig.rdsBaseUrlForAcknowledge}"

  //TODO move this to RDS connector
  def submit(request: RdsRequest)(implicit ec: ExecutionContext): Future[NewRdsAssessmentReport] =
    wsClient
      .url(baseUrlForRdsAssessmentsSubmit)
      .post(Json.toJson(request))
      .map(response =>
        response.status match {
          case Status.OK => response.json.validate[NewRdsAssessmentReport].get
          case unexpectedStatus => throw new RuntimeException(s"Unexpected status when attempting to get the assessment report from RDS: [$unexpectedStatus]")
        }
      )

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