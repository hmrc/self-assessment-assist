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

package uk.gov.hmrc.transactionalrisking.v1.services.cip

import play.api.http.Status.{OK,BAD_REQUEST, CREATED, NOT_FOUND, REQUEST_TIMEOUT}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpClient, HttpException, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.transactionalrisking.config.AppConfig
import uk.gov.hmrc.transactionalrisking.utils.Logging
import uk.gov.hmrc.transactionalrisking.v1.models.errors._
import uk.gov.hmrc.transactionalrisking.v1.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.v1.services.ServiceOutcome
import uk.gov.hmrc.transactionalrisking.v1.services.cip.models.{FraudRiskReport, FraudRiskRequest}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InsightConnector @Inject()(val httpClient: HttpClient,
                                 appConfig: AppConfig)(implicit val ec: ExecutionContext) extends Logging {

  def assess(fraudRiskRequest: FraudRiskRequest)(implicit hc: HeaderCarrier, correlationId: String): Future[ServiceOutcome[FraudRiskReport]] = {
    logger.info(s"$correlationId::[InsightConnector:submit] Before requesting report")

    httpClient
      .POST[FraudRiskRequest, HttpResponse](s"${appConfig.cipFraudServiceBaseUrl}", fraudRiskRequest)
      .map { response =>
        response.status match {
          case OK =>
            logger.debug(s"$correlationId::[InsightConnector:submit]Successfully submitted the report response status is ${response.status}")
            logger.info(s"####### response is ${response.json}")
            val fraudRiskReport = response.json.validate[FraudRiskReport].get
            Right(ResponseWrapper(correlationId,fraudRiskReport))
          case BAD_REQUEST =>
            logger.warn(s"$correlationId::[InsightConnector:submit] RDS response : BAD request")
            Left(ErrorWrapper(correlationId, DownstreamError))
          case NOT_FOUND =>
            logger.warn(s"$correlationId::[InsightConnector:submit] RDS not reachable")
            Left(ErrorWrapper(correlationId, ServiceUnavailableError))
          case REQUEST_TIMEOUT =>  Left(ErrorWrapper(correlationId, DownstreamError))
          case unexpectedStatus@_ =>
            logger.error(s"$correlationId::[InsightConnector:submit]Unable to submit the report due to unexpected status code returned $unexpectedStatus")
            Left(ErrorWrapper(correlationId, ServiceUnavailableError))
        }
      }
      .recover {
        case ex: BadRequestException =>
          logger.error(s"$correlationId::[InsightConnector:submit] BadRequestException $ex")
          Left(ErrorWrapper(correlationId, DownstreamError))

        case ex: UpstreamErrorResponse =>
          logger.error(s"$correlationId::[InsightConnector:submit] UpstreamErrorResponse $ex")
          ex.statusCode match {
            case 408 => Left(ErrorWrapper(correlationId, ServiceUnavailableError))
            case 401 => Left(ErrorWrapper(correlationId, ForbiddenDownstreamError))
            case 403 => Left(ErrorWrapper(correlationId, ForbiddenDownstreamError))
            case _ => Left(ErrorWrapper(correlationId, DownstreamError))
          }

        case ex: HttpException =>
          logger.error(s"$correlationId::[InsightConnector:submit] HttpException $ex")
          Left(ErrorWrapper(correlationId, ServiceUnavailableError))

        case ex@_ =>
          logger.error(s"$correlationId::[InsightConnector:submit] Unknown exception $ex")
          Left(ErrorWrapper(correlationId, ServiceUnavailableError))
      }
  }

}
