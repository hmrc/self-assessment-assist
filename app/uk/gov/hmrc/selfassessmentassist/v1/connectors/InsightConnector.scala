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
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.selfassessmentassist.config.AppConfig
import uk.gov.hmrc.selfassessmentassist.utils.Logging
import uk.gov.hmrc.selfassessmentassist.v1.models.errors._
import uk.gov.hmrc.selfassessmentassist.v1.models.outcomes.ResponseWrapper
import uk.gov.hmrc.selfassessmentassist.v1.services.ServiceOutcome
import uk.gov.hmrc.selfassessmentassist.v1.services.cip.models.{FraudRiskReport, FraudRiskRequest}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InsightConnector @Inject()(val httpClient: HttpClient,
                                 appConfig: AppConfig)(implicit val ec: ExecutionContext) extends Logging {

  def assess(fraudRiskRequest: FraudRiskRequest)(implicit hc: HeaderCarrier, correlationId: String): Future[ServiceOutcome[FraudRiskReport]] = {
    logger.info(s"$correlationId::[InsightConnector:assess] requesting fraud risk report")

    httpClient
      .POST[FraudRiskRequest, HttpResponse](s"${appConfig.cipFraudServiceBaseUrl}", fraudRiskRequest)
      .map { response =>
        logger.info(s"$correlationId::[InsightConnector:assess] FraudRiskreport status is ${response.status}")
        response.status match {
          case OK =>
            response.json.validate[FraudRiskReport].fold(
              e=> {
                logger.error(s"$correlationId::[InsightConnector:assess] failed during validate $e")
                Left(ErrorWrapper(correlationId, DownstreamError))
              },
              report => Right(ResponseWrapper(correlationId,report)))
          case _ =>
            logger.error(s"$correlationId::[InsightConnector:assess] Fraudrisk report failed as unknown code returned ${response.status}")
            Left(ErrorWrapper(correlationId, DownstreamError))
        }
      }
      .recover {
        case ex@_ =>
          logger.error(s"$correlationId::[InsightConnector:assess] Unknown exception $ex")
          Left(ErrorWrapper(correlationId, DownstreamError))
      }
  }

}
