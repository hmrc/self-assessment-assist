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
            case Right(_) =>
              nrsSubmissionWorkItemRepository
                .markAs(workItem.id, ProcessingStatus.Succeeded)
                .flatMap(_ => nrsSubmissionWorkItemRepository.completeAndDelete(workItem.id))
                .map(_ => ())
            case Left(_) => nrsSubmissionWorkItemRepository.markAs(workItem.id, ProcessingStatus.Failed).map(_ => ())
          }
      }
  }

  /** Cleans up work items that have successfully completed processing.
    *
    * This method ensures that items marked as `Succeeded` are properly deleted from the database. It was introduced as a precaution in case deletion
    * does not always succeed immediately after processing.
    *
    * @note
    *   It may be worth monitoring deletion behaviour over time to assess whether this cleanup step remains necessary. If considering removal:
    *   - Monitor logs for `[NrsSubmissionScheduler][cleanUpSucceededItems] Cleaning up X succeeded work items.` to verify whether any succeeded items
    *     are being cleaned up.
    *   - If the log consistently shows `Cleaning up 0 succeeded work items.`, this method may no longer be needed.
    *   - If removing, consider not marking items as `Succeeded` at all in [[processFailedSubmissions]] if deletion is always expected to happen
    *     immediately.
    *   - Revert the override of [[uk.gov.hmrc.selfassessmentassist.v1.repositories.NrsSubmissionWorkItemRepository.completeAndDelete]] that
    *     specifically handles `Succeeded` items.
    *   - Remove this method, [[uk.gov.hmrc.selfassessmentassist.v1.repositories.NrsSubmissionWorkItemRepository.findSucceededItems]] and any related
    *     scheduled cleanup tasks.
    */
  def cleanUpSucceededItems(): Future[Unit] =
    nrsSubmissionWorkItemRepository.findSucceededItems().flatMap { succeededItems =>
      logger.info(s"[NrsSubmissionScheduler][cleanUpSucceededItems] Cleaning up ${succeededItems.size} succeeded work items.")
      Future.traverse(succeededItems)(item => nrsSubmissionWorkItemRepository.completeAndDelete(item.id)).map(_ => ())
    }

  def logErrors(methodName: String)(future: Future[Unit]): Future[Unit] =
    future.recover { case e: Exception =>
      logger.error(
        s"[NrsSubmissionScheduler][$methodName] Error during NrsSubmissionScheduler job execution: '$methodName' failed",
        e
      )
    }

  private def executeJob(): Future[Unit] =
    logErrors("processFailedSubmissions")(processFailedSubmissions())
      .flatMap(_ => logErrors("cleanUpSucceededItems")(cleanUpSucceededItems()))

  val scheduledTask: Runnable = new Runnable() {
    override def run(): Unit = executeJob()
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
