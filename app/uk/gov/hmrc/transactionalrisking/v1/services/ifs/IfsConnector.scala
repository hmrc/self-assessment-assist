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

package uk.gov.hmrc.transactionalrisking.v1.services.ifs

import play.api.http.Status
import play.api.http.Status.NO_CONTENT
import play.api.mvc.Results.NoContent
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.transactionalrisking.config.AppConfig
import uk.gov.hmrc.transactionalrisking.utils.Logging
import uk.gov.hmrc.transactionalrisking.v1.services.ifs.models.request.IFRequest
import uk.gov.hmrc.transactionalrisking.v1.services.ifs.models.response.{IfsFailure, IfsResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class IfsConnector @Inject()(val httpClient: HttpClient, appConfig: AppConfig) (implicit val ec: ExecutionContext) extends Logging {

  private lazy val url: String    = appConfig.ifsBaseUrl
  private lazy val apiKey: String = appConfig.ifsApiKey

  def submit(ifRequest: IFRequest)(
    implicit hc: HeaderCarrier, correlationId: String): Future[IfsOutcome] = {

      httpClient
        .POST[IFRequest, HttpResponse](s"$url", ifRequest, Seq("X-API-Key" -> apiKey))
        .map { response =>
          val status = response.status

          if (Status.isSuccessful(status)) {
            logger.info(s"$correlationId::[submit] IFS submission successful")
            if(response.status == NO_CONTENT) {
              Right(IfsResponse(""))
            } else {
              Right(response.json.as[IfsResponse])
            }
          } else {
            logger.warn(s"$correlationId::[submit] RequestId:${hc.requestId}\nIFS submission failed with error: ${response.body}")
            Left(IfsFailure.ErrorResponse(status))
          }
        }
        .recover {
          case NonFatal(e) =>
            logger.error(s"$correlationId::[submit] RequestId:${hc.requestId}\nIFS submission failed with exception", e)
            Left(IfsFailure.Exception(e.getMessage))
        }
  }
}
