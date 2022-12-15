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

import play.api.libs.json.Json

//TODO RdsAssessmentReport will be populated using RdsAssessmentReport class, as of now String because common
// class is used for generate report and acknowledge


trait RequestBody {
  def toOutput:String
}


object RequestBodyReport{
  implicit val formatter = Json.format[RequestBodyReport]
}


case class RequestBodyReport(bodyContent:String, reportId:String) extends RequestBody
{
  def toOutput:String = {

    Json.toJson(this).toString()
  }
}




case class RequestBodyAcknowledge(bodyContent:String)  extends RequestBody
{
  //import RequestBodyAcknowledge.formatter._
  def toOutput: String = {
    bodyContent
  }
}

//object RequestBodyAcknowledge{
//  implicit val formatter = Json.toString
//}
