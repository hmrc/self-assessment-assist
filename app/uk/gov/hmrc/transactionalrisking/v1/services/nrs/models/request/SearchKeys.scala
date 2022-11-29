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

package uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.request

import play.api.libs.json.{Json, OFormat, Reads, Writes}
import uk.gov.hmrc.transactionalrisking.utils.DateUtils

import java.time.LocalDate

case class SearchKeys(nino: String,
                      taxYear: String,
                      reportId: String
                     )

object SearchKeys {
  implicit val dateReads: Reads[LocalDate] = DateUtils.dateReads
  implicit val dateWrites: Writes[LocalDate] = DateUtils.dateWrites
  implicit val format: OFormat[SearchKeys] = Json.format[SearchKeys]
}