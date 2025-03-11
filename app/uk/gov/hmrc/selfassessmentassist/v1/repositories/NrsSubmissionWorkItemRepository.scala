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

import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.workitem.{WorkItemFields, WorkItemRepository}
import uk.gov.hmrc.selfassessmentassist.config.AppConfig
import uk.gov.hmrc.selfassessmentassist.v1.models.request.nrs.NrsSubmissionWorkItem

import java.time.{Duration, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class NrsSubmissionWorkItemRepository @Inject() (appConfig: AppConfig, mongoComponent: MongoComponent)(implicit ec: ExecutionContext)
    extends WorkItemRepository[NrsSubmissionWorkItem](
      collectionName = "nrsSubmissionWorkItems",
      mongoComponent = mongoComponent,
      itemFormat = NrsSubmissionWorkItem.format,
      workItemFields = WorkItemFields.default,
      extraIndexes = Seq(
        IndexModel(
          keys = Indexes.ascending("item.nrsSubmission.metadata.payloadSha256Checksum"),
          indexOptions = IndexOptions().background(true).unique(true)
        )
      )
    ) {

  override def now(): Instant = Instant.now()

  override val inProgressRetryAfter: Duration = appConfig.nrsInProgressRetryAfter

}
