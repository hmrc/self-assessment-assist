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

package uk.gov.hmrc.selfassessmentassist.v1.repositories

import org.bson.types.ObjectId
import uk.gov.hmrc.selfassessmentassist.support.IntegrationBaseSpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}
import uk.gov.hmrc.selfassessmentassist.config.AppConfig
import uk.gov.hmrc.selfassessmentassist.v1.models.request.nrs.{NrsSubmission, NrsSubmissionWorkItem}
import uk.gov.hmrc.selfassessmentassist.v1.services.testData.NrsTestData.correctModel

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global

class NrsSubmissionWorkItemRepositoryISpec extends IntegrationBaseSpec {

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

  private val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  private val repository: NrsSubmissionWorkItemRepository = new NrsSubmissionWorkItemRepository(
    appConfig = appConfig,
    mongoComponent = mongoComponent
  )

  "NrsSubmissionWorkItemRepository" when {
    ".completeAndDelete" should {
      "delete a work item when it has Succeeded status" in {
        val succeededItem: WorkItem[NrsSubmissionWorkItem] = workItem.copy(status = ProcessingStatus.Succeeded)

        await(repository.collection.insertOne(succeededItem).toFuture())

        val fetchedItemBeforeDeletion: Option[WorkItem[NrsSubmissionWorkItem]] = await(repository.findById(succeededItem.id))

        fetchedItemBeforeDeletion shouldBe Some(succeededItem)

        val result: Boolean = await(repository.completeAndDelete(succeededItem.id))

        result shouldBe true

        val fetchedItemAfterDeletion: Option[WorkItem[NrsSubmissionWorkItem]] = await(repository.findById(succeededItem.id))

        fetchedItemAfterDeletion shouldBe None
      }

      "not delete a work item when it does not have Succeeded status" in {
        val failedItem: WorkItem[NrsSubmissionWorkItem] = workItem.copy(status = ProcessingStatus.Failed)

        await(repository.collection.insertOne(failedItem).toFuture())

        val fetchedItemBeforeDeletion: Option[WorkItem[NrsSubmissionWorkItem]] = await(repository.findById(failedItem.id))

        fetchedItemBeforeDeletion shouldBe Some(failedItem)

        val result: Boolean = await(repository.completeAndDelete(failedItem.id))

        result shouldBe false

        val fetchedItemAfterDeletion: Option[WorkItem[NrsSubmissionWorkItem]] = await(repository.findById(failedItem.id))

        fetchedItemAfterDeletion shouldBe Some(failedItem)
      }
    }

    ".findSucceededItems" should {
      "return all succeeded work items in the database" in {
        val succeededItems: Seq[WorkItem[NrsSubmissionWorkItem]] = Seq.fill(3) {
          workItem.copy(
            id = new ObjectId(),
            status = ProcessingStatus.Succeeded
          )
        }

        await(repository.collection.insertMany(succeededItems).toFuture())

        val result: Seq[WorkItem[NrsSubmissionWorkItem]] = await(repository.findSucceededItems())

        result should contain theSameElementsAs succeededItems
      }

      "return an empty list when there are no succeeded items" in {
        val result: Seq[WorkItem[NrsSubmissionWorkItem]] = await(repository.findSucceededItems())

        result shouldBe empty
      }
    }
  }

}
