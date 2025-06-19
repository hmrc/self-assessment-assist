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

import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{ErrorWrapper, InternalError}
import uk.gov.hmrc.selfassessmentassist.api.models.outcomes.ResponseWrapper
import uk.gov.hmrc.selfassessmentassist.config.AppConfig
import uk.gov.hmrc.selfassessmentassist.utils.Logging
import uk.gov.hmrc.selfassessmentassist.v1.models.request.cip.{FraudRiskReport, FraudRiskRequest}
import uk.gov.hmrc.selfassessmentassist.v1.services.ServiceOutcome

import java.util.Base64
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InsightConnector @Inject() (val httpClient: HttpClientV2, appConfig: AppConfig)(implicit val ec: ExecutionContext) extends Logging {

  private[connectors] def fraudRiskHeaders(): Seq[(String, String)] = {

    val username: String   = appConfig.cipFraudUsername
    val password: String   = appConfig.cipFraudToken
    val credentials        = s"$username:$password"
    val encodedCredentials = Base64.getEncoder.encodeToString(credentials.getBytes)
    Seq(
      "Authorization" -> s"Basic $encodedCredentials"
    )
  }

  def assess(fraudRiskRequest: FraudRiskRequest)(implicit hc: HeaderCarrier, correlationId: String): Future[ServiceOutcome[FraudRiskReport]] = {
    logger.info(s"$correlationId::[InsightConnector:assess] requesting fraud risk report")

    httpClient
      .post(url"${appConfig.cipFraudServiceBaseUrl}")
      .withBody(Json.toJson(fraudRiskRequest))
      .setHeader(fraudRiskHeaders(): _*)
      .execute[HttpResponse]
      .map { response =>
        logger.info(s"$correlationId::[InsightConnector:assess] FraudRiskReport status is ${response.status}")
        response.status match {
          case OK =>
            response.json
              .validate[FraudRiskReport]
              .fold(
                e => {
                  logger.error(s"$correlationId::[InsightConnector:assess] CIP failed during validate $e")
                  Left(ErrorWrapper(correlationId, InternalError))
                },
                report => Right(ResponseWrapper(correlationId, report))
              )
          case _ =>
            logger.error(s"$correlationId::[InsightConnector:assess] CIP FraudRisk report failed as unknown code returned ${response.status}")
            Left(ErrorWrapper(correlationId, InternalError))
        }
      }
      .recover { case ex @ _ =>
        logger.error(s"$correlationId::[InsightConnector:assess] CIP Unknown exception ", ex)
        Left(ErrorWrapper(correlationId, InternalError))
      }
  }

}
