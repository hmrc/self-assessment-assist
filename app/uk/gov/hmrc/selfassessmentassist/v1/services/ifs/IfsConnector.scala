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

package uk.gov.hmrc.selfassessmentassist.v1.services.ifs

import play.api.http.{HeaderNames, MimeTypes}
import play.api.http.Status.{NO_CONTENT, SERVICE_UNAVAILABLE}
import play.api.libs.json.Writes
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpClient, HttpReads, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.selfassessmentassist.config.AppConfig
import uk.gov.hmrc.selfassessmentassist.utils.Logging
import uk.gov.hmrc.selfassessmentassist.v1.models.errors.{DownstreamError, ErrorWrapper}
import uk.gov.hmrc.selfassessmentassist.v1.services.ifs.models.request.IFRequest
import uk.gov.hmrc.selfassessmentassist.v1.services.ifs.models.response.IfsResponse

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

@Singleton
class IfsConnector @Inject()(val httpClient: HttpClient, appConfig: AppConfig) (implicit val ec: ExecutionContext) extends Logging {

  private lazy val url: String    = appConfig.ifsBaseUrl

  def submit(ifRequest: IFRequest)(
    implicit hc: HeaderCarrier, correlationId: String): Future[IfsOutcome] = {

    logger.info(s"$correlationId::[IfsConnector:submit] submitting store interaction for action ${ifRequest.eventName}")
    //TODO remove me
    val headersPassed = s" Environment = ${appConfig.ifsEnv} , CorrelationId = $correlationId"

    logger.info(s"$correlationId::[IfsConnector:submit] url  $url headers = $headersPassed, " +
      s"${HeaderNames.CONTENT_TYPE} -> ${MimeTypes.JSON};charset=UTF-8" )

    logger.info(s"$correlationId::[IfsConnector:submit] request is   $ifRequest" )

      httpClient
        .POST[IFRequest, HttpResponse](s"$url", ifRequest,Seq(
          "Environment"   -> appConfig.ifsEnv,
          "CorrelationId" -> correlationId,
          HeaderNames.CONTENT_TYPE -> s"${MimeTypes.JSON};charset=UTF-8",
          "accept"-> "*/*",
          "Authorization" -> s"Bearer ${appConfig.ifsToken}"
        ))(implicitly[Writes[IFRequest]],
          implicitly[HttpReads[HttpResponse]],
          hc.copy(authorization = None), ec)
        .map { response =>
          response.status match {
            case NO_CONTENT => {
              logger.info(s"$correlationId::[IfsConnector:submit]  ${ifRequest.eventName} interaction stored successfully")
              Right(IfsResponse())
            }
            case unexpectedStatus@_ =>
              logger.error(s"$correlationId::[IfsConnector:submit]Unable to submit the report due to unexpected status code returned $unexpectedStatus $response")
              Left(ErrorWrapper(correlationId, DownstreamError))
          }
        }
        .recover {
          case e: BadRequestException => {
            logger.error(s"$correlationId::[IfsConnector:submit] IFS response : BAD request ${e.message}")
            Left(ErrorWrapper(correlationId, DownstreamError))
          }
          case e: UpstreamErrorResponse if e.statusCode == SERVICE_UNAVAILABLE => {
            logger.error(s"$correlationId::[IfsConnector:submit] IFS response : SERVICE_UNAVAILABLE request ${e.message}")
            Left(ErrorWrapper(correlationId, DownstreamError))
          }
          case NonFatal(e) => {
            logger.error(s"$correlationId::[submit] RequestId:${hc.requestId}\nIFS submission failed with exception", e)
            Left(ErrorWrapper(correlationId, DownstreamError))
          }
        }
  }
}
