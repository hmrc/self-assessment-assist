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

package uk.gov.hmrc.transactionalrisking.utils

import java.time.OffsetDateTime
import javax.inject.{Inject, Singleton}

@Singleton
class CurrentDateTime @Inject()() {
  def getDateTime: OffsetDateTime = OffsetDateTime.now()
  def dateString(currentDatetime: OffsetDateTime): OffsetDateTime = {
    val formatted = currentDatetime.format(DateUtils.dateTimePattern)
//    val formatter = DateUtils.dateTimePattern.format(OffsetDateTime.parse(currentDatetime.toString))
    OffsetDateTime.parse(formatted)
  }
}