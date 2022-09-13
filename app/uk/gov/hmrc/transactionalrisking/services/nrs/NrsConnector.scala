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

package uk.gov.hmrc.transactionalrisking.services.nrs

import akka.actor.Scheduler
import play.api.http.Status
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.transactionalrisking.config.AppConfig
import uk.gov.hmrc.transactionalrisking.services.nrs.models.response.NrsResponse
import uk.gov.hmrc.transactionalrisking.services.nrs.models.request.NrsSubmission
import uk.gov.hmrc.transactionalrisking.services.nrs.models.response.{NrsFailure, NrsResponse}
import uk.gov.hmrc.transactionalrisking.utils.{Delayer, Logging, Retrying}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Success, Try}

@Singleton
class NrsConnector @Inject()(val httpClient: HttpClient,
                             appConfig: AppConfig)
                            (implicit val scheduler: Scheduler, val ec: ExecutionContext)
  extends Retrying
    with Delayer
    with Logging {

  private lazy val url: String    = appConfig.nrsBaseUrl
  private lazy val apiKey: String = appConfig.nrsApiKey

  def submit(nrsSubmission: NrsSubmission, reportId:String)(
    implicit hc: HeaderCarrier, correlationId: String): Future[NrsOutcome] = {

    val retryCondition: Try[NrsOutcome] => Boolean = {
      case Success(Left(failure)) => failure.
        retryable
      case _                      => false
    }

    retry(appConfig.nrsRetries, retryCondition) { attemptNumber =>
      logger.info(s"Attempt $attemptNumber NRS submission: sending POST request to $url")

      httpClient
        .POST[NrsSubmission, HttpResponse](s"$url", nrsSubmission, Seq("X-API-Key" -> apiKey))
        .map { response =>
          val status = response.status

          if (Status.isSuccessful(status)) {
            logger.info(s"NRS submission successful")
            Right(response.json.as[NrsResponse])
          } else {
            logger.warn(s".CorrelationId: $correlationId\tRequestId:${hc.requestId}\nNRS submission failed with error: ${response.body}")
            Left(NrsFailure.ErrorResponse(status))
          }
        }
        .recover {
          case NonFatal(e) =>
            logger.error(s".CorrelationId: $correlationId\tRequestId:${hc.requestId}\nNRS submission failed with exception", e)
            Left(NrsFailure.ExceptionThrown)
        }
    }
  }
}
