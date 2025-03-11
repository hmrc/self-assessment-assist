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

package uk.gov.hmrc.selfassessmentassist.v1.schedulers

import org.apache.pekko.actor.{ActorSystem, Cancellable}
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.workitem.ProcessingStatus
import uk.gov.hmrc.selfassessmentassist.config.AppConfig
import uk.gov.hmrc.selfassessmentassist.utils.{IdGenerator, Logging}
import uk.gov.hmrc.selfassessmentassist.v1.connectors.NrsConnector
import uk.gov.hmrc.selfassessmentassist.v1.repositories.NrsSubmissionWorkItemRepository

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NrsSubmissionScheduler @Inject() (actorSystem: ActorSystem,
                                        appConfig: AppConfig,
                                        nrsSubmissionWorkItemRepository: NrsSubmissionWorkItemRepository,
                                        nrsConnector: NrsConnector,
                                        idGenerator: IdGenerator,
                                        applicationLifecycle: ApplicationLifecycle)(implicit ec: ExecutionContext)
    extends Logging {

  def processFailedSubmissions(): Future[Unit] = {
    val now: Instant = Instant.now()

    implicit val correlationId: String = idGenerator.generateCorrelationId
    implicit val hc: HeaderCarrier     = HeaderCarrier()

    nrsSubmissionWorkItemRepository
      .pullOutstanding(
        failedBefore = now.minusSeconds(appConfig.nrsFailedBeforeSeconds),
        availableBefore = now
      )
      .flatMap {
        case None => Future.unit
        case Some(workItem) =>
          nrsConnector.submit(workItem.item.nrsSubmission).flatMap {
            case Right(_) => nrsSubmissionWorkItemRepository.completeAndDelete(workItem.id).map(_ => ())
            case Left(_)  => nrsSubmissionWorkItemRepository.markAs(workItem.id, ProcessingStatus.Failed).map(_ => ())
          }
      }
  }

  val scheduledTask: Runnable = new Runnable() {

    override def run(): Unit = processFailedSubmissions().recover { case e: Exception =>
      logger.error(
        "[NrsSubmissionScheduler][processFailedSubmissions] Error during NrsSubmissionScheduler job execution:" +
          " 'processFailedSubmissions' failed",
        e
      )
    }

  }

  private val scheduledJob: Cancellable =
    actorSystem.scheduler.scheduleWithFixedDelay(
      initialDelay = appConfig.nrsSchedulerInitialDelay,
      delay = appConfig.nrsSchedulerDelay
    )(scheduledTask)

  applicationLifecycle.addStopHook { () =>
    logger.info(
      "[NrsSubmissionScheduler] Application shutting down. Waiting for ongoing tasks to complete " +
        "and cancelling scheduled job...")
    scheduledJob.cancel()
    Future.unit
  }

}
