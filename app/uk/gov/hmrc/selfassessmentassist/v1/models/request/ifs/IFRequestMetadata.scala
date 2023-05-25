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

package uk.gov.hmrc.selfassessmentassist.v1.models.request.ifs

import play.api.libs.json.{Format, Json, Reads, Writes}
import uk.gov.hmrc.selfassessmentassist.utils.DateUtils

import java.time.OffsetDateTime

case class IFRequestMetadata(
    nino: String,
    taxYear: String,
    calculationId: String,
    customerType: String, // agent or individual
    agentReferenceNumber: Option[String],
    calculationTimestamp: OffsetDateTime
)

object IFRequestMetadata {
  implicit val dateReads: Reads[OffsetDateTime]   = DateUtils.offsetDateTimeFromLocalDateTimeFormatReads
  implicit val dateWrites: Writes[OffsetDateTime] = DateUtils.isoInstantDateTimeWrites
  implicit val formats: Format[IFRequestMetadata] = Json.format[IFRequestMetadata]
}