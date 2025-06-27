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

import org.apache.pekko.actor.ActorSystem
import org.bson.types.ObjectId
import org.scalatest.concurrent.Eventually.eventually
import play.api.inject.ApplicationLifecycle
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing
import uk.gov.hmrc.selfassessmentassist.config.AppConfig
import uk.gov.hmrc.selfassessmentassist.stubs.NrsStub
import uk.gov.hmrc.selfassessmentassist.support.IntegrationBaseSpec
import uk.gov.hmrc.selfassessmentassist.utils.IdGenerator
import uk.gov.hmrc.selfassessmentassist.v1.connectors.NrsConnector
import uk.gov.hmrc.selfassessmentassist.v1.models.request.nrs.{NrsSubmission, NrsSubmissionWorkItem}
import uk.gov.hmrc.selfassessmentassist.v1.repositories.NrsSubmissionWorkItemRepository
import uk.gov.hmrc.selfassessmentassist.v1.services.testData.NrsTestData.correctModel

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NrsSubmissionSchedulerISpec extends IntegrationBaseSpec with LogCapturing {

  override def servicesConfig: Map[String, Any] =
    Map(
      "microservice.services.non-repudiation.initialDelay"            -> "500 milliseconds",
      "microservice.services.non-repudiation.numberOfRetries"         -> "1",
      "microservice.services.non-repudiation.scheduler.initial-delay" -> "10 seconds",
      "microservice.services.non-repudiation.scheduler.delay"         -> "30 seconds",
      "microservice.services.non-repudiation.failed-before-seconds"   -> "2"
    ) ++ super.servicesConfig

  private val nrsSubmitUrl: String         = "/submission"
  private val nrsSubmission: NrsSubmission = correctModel
  private val instant: Instant             = Instant.now().truncatedTo(ChronoUnit.MILLIS)

  private val workItem: WorkItem[NrsSubmissionWorkItem] = WorkItem(
    id = new ObjectId(),
    receivedAt = instant,
    updatedAt = instant,
    availableAt = instant,
    status = ProcessingStatus.ToDo,
    failureCount = 0,
    item = NrsSubmissionWorkItem(nrsSubmission)
  )

  private val actorSystem: ActorSystem                   = app.actorSystem
  private val appConfig: AppConfig                       = app.injector.instanceOf[AppConfig]
  private val nrsConnector: NrsConnector                 = app.injector.instanceOf[NrsConnector]
  private val idGenerator: IdGenerator                   = app.injector.instanceOf[IdGenerator]
  private val applicationLifecycle: ApplicationLifecycle = app.injector.instanceOf[ApplicationLifecycle]

  private val repository: NrsSubmissionWorkItemRepository = new NrsSubmissionWorkItemRepository(
    appConfig = appConfig,
    mongoComponent = mongoComponent
  )

  private val scheduler: NrsSubmissionScheduler = new NrsSubmissionScheduler(
    actorSystem = actorSystem,
    appConfig = appConfig,
    nrsSubmissionWorkItemRepository = repository,
    nrsConnector = nrsConnector,
    idGenerator = idGenerator,
    applicationLifecycle = applicationLifecycle
  )

  "NrsSubmissionScheduler" when {
    ".processFailedSubmissions" should {
      "successfully process a work item when the downstream service returns a successful response" in {
        await(repository.collection.insertOne(workItem).toFuture())

        val insertedWorkItem: Option[WorkItem[NrsSubmissionWorkItem]] = await(repository.findById(workItem.id))

        insertedWorkItem shouldBe Some(workItem)

        NrsStub.submit(nrsSubmitUrl)

        await(scheduler.processFailedSubmissions())

        val fetchedWorkItem: Option[WorkItem[NrsSubmissionWorkItem]] = await(repository.findById(workItem.id))

        fetchedWorkItem shouldBe None
      }

      "mark the work item as failed when the downstream service returns an error response" in {
        await(repository.collection.insertOne(workItem).toFuture())

        val insertedWorkItem: Option[WorkItem[NrsSubmissionWorkItem]] = await(repository.findById(workItem.id))

        insertedWorkItem shouldBe Some(workItem)

        NrsStub.submitFailure(nrsSubmitUrl)

        await(scheduler.processFailedSubmissions())

        val fetchedWorkItem: Option[WorkItem[NrsSubmissionWorkItem]] = await(repository.findById(workItem.id))

        val expectedWorkItem: WorkItem[NrsSubmissionWorkItem] = workItem.copy(
          status = ProcessingStatus.Failed,
          failureCount = 1,
          updatedAt = fetchedWorkItem.get.updatedAt
        )

        fetchedWorkItem shouldBe Some(expectedWorkItem)
      }

      "retry a failed work item and successfully process it when the downstream service returns a successful response" in {
        val failedWorkItem: WorkItem[NrsSubmissionWorkItem] = workItem.copy(status = ProcessingStatus.Failed, failureCount = 1)

        await(repository.collection.insertOne(failedWorkItem).toFuture())

        val insertedWorkItem: Option[WorkItem[NrsSubmissionWorkItem]] = await(repository.findById(failedWorkItem.id))

        insertedWorkItem shouldBe Some(failedWorkItem)

        NrsStub.submit(nrsSubmitUrl)

        await(scheduler.processFailedSubmissions())

        val fetchedWorkItem: Option[WorkItem[NrsSubmissionWorkItem]] = await(repository.findById(failedWorkItem.id))

        fetchedWorkItem shouldBe None
      }

      "do nothing when there are no work items in the database" in {
        await(scheduler.processFailedSubmissions())

        val fetchedWorkItem: Option[WorkItem[NrsSubmissionWorkItem]] = await(repository.findById(workItem.id))

        fetchedWorkItem shouldBe None
      }
    }

    ".scheduledTask" should {
      "execute the job to process the failed item" in {
        val failedWorkItem: WorkItem[NrsSubmissionWorkItem] = workItem.copy(status = ProcessingStatus.Failed, failureCount = 1)

        await(repository.collection.insertOne(failedWorkItem).toFuture())

        val insertedFailedWorkItem: Option[WorkItem[NrsSubmissionWorkItem]] = await(repository.findById(workItem.id))

        insertedFailedWorkItem shouldBe Some(failedWorkItem)

        NrsStub.submit(nrsSubmitUrl)

        eventually {
          scheduler.scheduledTask.run()
          await(repository.findById(failedWorkItem.id)) shouldBe None
        }
      }

      "log an error when the job execution fails due to an exception in processFailedSubmissions" in {
        val expectedLogMessage: String = "[NrsSubmissionScheduler][processFailedSubmissions] Error during NrsSubmissionScheduler" +
          " job execution: 'processFailedSubmissions' failed"

        val scheduler: NrsSubmissionScheduler = new NrsSubmissionScheduler(
          actorSystem = actorSystem,
          appConfig = appConfig,
          nrsSubmissionWorkItemRepository = repository,
          nrsConnector = nrsConnector,
          idGenerator = idGenerator,
          applicationLifecycle = applicationLifecycle
        ) {
          override def processFailedSubmissions(): Future[Unit] = Future.failed(new RuntimeException("Exception test"))
        }

        withCaptureOfLoggingFrom(scheduler.logger) { events =>
          eventually {
            scheduler.scheduledTask.run()
            events.map(_.getMessage) should contain only expectedLogMessage
          }
        }
      }
    }
  }

}
