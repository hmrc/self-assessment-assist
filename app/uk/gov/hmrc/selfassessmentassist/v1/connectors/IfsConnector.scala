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

import play.api.http.Status.NO_CONTENT
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.Writes
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException, HttpReads, HttpResponse}
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{ErrorWrapper, InternalError}
import uk.gov.hmrc.selfassessmentassist.config.AppConfig
import uk.gov.hmrc.selfassessmentassist.utils.Logging
import uk.gov.hmrc.selfassessmentassist.v1.models.request.ifs.IFRequest
import uk.gov.hmrc.selfassessmentassist.v1.models.response.ifs.IfsResponse
import uk.gov.hmrc.selfassessmentassist.v1.services.IfsOutcome

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IfsConnector @Inject() (val httpClient: HttpClient, appConfig: AppConfig)(implicit val ec: ExecutionContext) extends Logging {

  private lazy val url: String = appConfig.ifsBaseUrl

  def submit(ifRequest: IFRequest)(implicit hc: HeaderCarrier, correlationId: String): Future[IfsOutcome] = {

    logger.info(s"$correlationId::[IfsConnector:submit] submitting store interaction for action ${ifRequest.eventName}")

    httpClient
      .POST[IFRequest, HttpResponse](
        s"$url",
        ifRequest,
        Seq(
          "Environment"            -> appConfig.ifsEnv,
          "CorrelationId"          -> correlationId,
          HeaderNames.CONTENT_TYPE -> s"${MimeTypes.JSON};charset=UTF-8",
          "accept"                 -> "*/*",
          "Authorization"          -> s"Bearer ${appConfig.ifsToken}"
        )
      )(implicitly[Writes[IFRequest]], implicitly[HttpReads[HttpResponse]], hc.copy(authorization = None), ec)
      .map { response =>
        response.status match {
          case NO_CONTENT =>
            logger.info(s"$correlationId::[IfsConnector:submit]  ${ifRequest.eventName} interaction stored successfully")
            Right(IfsResponse())
          case unexpectedStatus @ _ =>
            logger.error(s"$correlationId::[IfsConnector:submit]Unable to submit the report due to unexpected status code returned $unexpectedStatus")
            Left(ErrorWrapper(correlationId, InternalError))
        }
      }
      .recover { case e: HttpException =>
        logger.error(s"$correlationId::[IfsConnector:submit] IFS response : failed with exception", e)
        Left(ErrorWrapper(correlationId, InternalError))
      }
  }

}
