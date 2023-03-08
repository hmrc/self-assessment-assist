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
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpClient,HttpResponse, UpstreamErrorResponse}
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
  //private lazy val requestHeader: Seq[(String, String)] = Seq("Authorization" -> s"Bearer ${appConfig.ifsToken}")
  private val jsonContentTypeHeader = HeaderNames.CONTENT_TYPE -> MimeTypes.JSON
  private def getBackendHeaders[Resp](hc: HeaderCarrier,
                                      correlationId: String,
                                      additionalHeaders: (String, String)*): HeaderCarrier = {


    val passThroughHeaders = hc
      .headers(appConfig.ifsEnvironmentHeaders.getOrElse(Seq.empty))
      .filterNot(hdr => additionalHeaders.exists(_._1.equalsIgnoreCase(hdr._1)))

    HeaderCarrier(
      extraHeaders = hc.extraHeaders ++
        // Contract headers
        Seq(
          "Authorization" -> s"Bearer ${appConfig.ifsToken}",
          "Environment"   -> appConfig.ifsEnv,
          "CorrelationId" -> correlationId
        ) ++
        additionalHeaders ++
        passThroughHeaders
    )
  }

  def submit(ifRequest: IFRequest)(
    implicit hc: HeaderCarrier, correlationId: String): Future[IfsOutcome] = {

    logger.info(s"$correlationId::[IfsConnector:submit] submitting store interaction for action ${ifRequest.eventName}")
    //TODO remove me

    val backEndHeaders: HeaderCarrier = getBackendHeaders(hc, correlationId, jsonContentTypeHeader)
    logger.info(s"$correlationId::[IfsConnector:submit] url and data  $url header = $backEndHeaders")
    doPost(ifRequest)(backEndHeaders,correlationId)
  }

  private def doPost(ifRequest: IFRequest)(implicit hc: HeaderCarrier, correlationId: String) = {
    httpClient
      .POST[IFRequest, HttpResponse](s"$url", ifRequest)
      .map { response =>
        response.status match {
          case NO_CONTENT => {
            logger.info(s"$correlationId::[IfsConnector:submit]  ${ifRequest.eventName} interaction stored successfully")
            Right(IfsResponse())
          }
          case unexpectedStatus@_ =>
            logger.error(s"$correlationId::[IfsConnector:submit]Unable to submit the report due to unexpected status code returned $unexpectedStatus")
            Left(ErrorWrapper(correlationId, DownstreamError))
        }
      }
      .recover {
        case _: BadRequestException =>
          logger.warn(s"$correlationId::[IfsConnector:submit] IFS response : BAD request")
          Left(ErrorWrapper(correlationId, DownstreamError))

        case e: UpstreamErrorResponse if e.statusCode == SERVICE_UNAVAILABLE =>
          logger.warn(s"$correlationId::[IfsConnector:submit] IFS response : SERVICE_UNAVAILABLE request")
          Left(ErrorWrapper(correlationId, DownstreamError))

        case NonFatal(e) =>
          logger.error(s"$correlationId::[submit] RequestId:${hc.requestId}\nIFS submission failed with exception", e)
          Left(ErrorWrapper(correlationId, DownstreamError))
      }
  }
}
