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

package uk.gov.hmrc.selfassessmentassist.v1.services.testData

import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.selfassessmentassist.api.TestData.CommonTestData
import uk.gov.hmrc.selfassessmentassist.v1.models.request.nrs.NrsSubmission

object NrsTestData {

  val correctJson: JsObject = Json.obj(
    "payload"  -> "XXX-base64checksum-XXX",
    "metadata" -> CommonTestData.metaDataCorrectJson
  )

  val correctJsonString: String = correctJson.toString

  val correctModel: NrsSubmission = NrsSubmission(
    "XXX-base64checksum-XXX",
    CommonTestData.metaDataCorrectModel
  )

}
