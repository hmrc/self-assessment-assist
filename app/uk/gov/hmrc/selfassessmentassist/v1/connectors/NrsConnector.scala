/*
 * Copyright 2025 HM Revenue & Customs
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

import com.mongodb.MongoWriteException
import org.apache.pekko.actor.Scheduler
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.selfassessmentassist.config.AppConfig
import uk.gov.hmrc.selfassessmentassist.utils.{Delayer, Logging, Retrying}
import uk.gov.hmrc.selfassessmentassist.v1.models.request.nrs.{NrsSubmission, NrsSubmissionWorkItem}
import uk.gov.hmrc.selfassessmentassist.v1.models.response.nrs.{NrsFailure, NrsResponse}
import uk.gov.hmrc.selfassessmentassist.v1.repositories.NrsSubmissionWorkItemRepository
import uk.gov.hmrc.selfassessmentassist.v1.services.NrsOutcome

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Success, Try}

@Singleton
class NrsConnector @Inject() (val httpClient: HttpClientV2, appConfig: AppConfig, nrsSubmissionWorkItemRepository: NrsSubmissionWorkItemRepository)(
    implicit
    val scheduler: Scheduler,
    val ec: ExecutionContext)
    extends Retrying
    with Delayer
    with Logging {

  private lazy val url: String    = appConfig.nrsBaseUrl
  private lazy val apiKey: String = appConfig.nrsApiKey

  def submit(nrsSubmission: NrsSubmission)(implicit hc: HeaderCarrier, correlationId: String): Future[NrsOutcome] = {

    val retryCondition: Try[NrsOutcome] => Boolean = {
      case Success(Left(failure)) => failure.retryable
      case _                      => false
    }

    val mongoDuplicateKeyErrorCode = 11000

    retry(appConfig.nrsRetries, retryCondition) { attemptNumber =>
      logger.info(s"$correlationId::[NrsConnector:submit] Attempt $attemptNumber NRS submission: sending POST request to $url")
      httpClient
        .post(url"$url")
        .setHeader(Seq("X-API-Key" -> apiKey): _*)
        .withBody(Json.toJson(nrsSubmission))
        .execute[HttpResponse]
        .map { response =>
          val status = response.status

          if (Status.isSuccessful(status)) {
            val nrsResponse = response.json.as[NrsResponse]
            logger.info(s"$correlationId::[NrsConnector:submit] NRS submission successful status received from NRS $status")
            Right(nrsResponse)
          } else {
            logger.warn(s"$correlationId::[NrsConnector:submit] NRS submission failed with error: $status")
            Left(NrsFailure.ErrorResponse(status))
          }
        }
        .recover { case NonFatal(e) =>
          logger.error(s"$correlationId::[NrsConnector:submit] NRS submission failed with exception", e)
          Left(NrsFailure.ExceptionThrown)
        }
    }.flatMap {
      case Left(failure) if failure.retryable =>
        nrsSubmissionWorkItemRepository
          .pushNew(NrsSubmissionWorkItem(nrsSubmission))
          .map { _ =>
            logger.warn(s"$correlationId::[NrsConnector:submit] Storing new failed NRS submission in MongoDB")
            Left(failure)
          }
          .recover {
            case e: MongoWriteException if e.getCode == mongoDuplicateKeyErrorCode =>
              logger.warn(s"$correlationId::[NrsConnector:submit] NRS submission already exists in MongoDB, skipping insert")
              Left(failure)
          }
      // not retryable
      case Left(failure) =>
        logger.error(s"$correlationId::[NrsConnector:submit] NRS Submission not stored to database")
        Future.successful(Left(failure))
      case Right(nrsResponse) => Future.successful(Right(nrsResponse))
    }
  }

}
