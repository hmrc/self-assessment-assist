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

package uk.gov.hmrc.transactionalrisking.services.nrs.models.request

import play.api.libs.json.Json
import uk.gov.hmrc.transactionalrisking.models.domain.AssessmentReport

//TODO newRdsAssessmentReport will be populated using NewRdsAssessmentReport class, as of now String because common
// class is used for generate report and acknowledge
//TODO should this be called NRSRequestBody? is this left NRS specific?
//TODO reportID may be it should be UUID for type safety not String
case class RequestBody(bodyContent:String, reportId:String)
object RequestBody{
  implicit val formatter = Json.format[RequestBody]
}
