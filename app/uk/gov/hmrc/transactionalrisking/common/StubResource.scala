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

package uk.gov.hmrc.transactionalrisking.common

import play.api.Logging
import play.api.http.ContentTypes
import play.api.libs.json.Json
import play.api.mvc.Results

import java.io.{File, FileInputStream}

trait StubResourceBase extends Results with ContentTypes with Logging {

  def loadSubmitResponseTemplate(calculationID: String, replaceFeedbackID: String, replaceCorrelationID: String) = {
    val fileName = s"conf/response/submit/$calculationID-response.json"
    val templateCotent =
      findResource(fileName).map(
        _.replace("replaceFeedbackID", replaceFeedbackID)
          .replace("replaceCalculationID", calculationID)
          .replace("replaceCorrelationID", replaceCorrelationID))


    val parsedContent = templateCotent
      .map(Json.parse).getOrElse(throw new IllegalStateException("Response template parsing failed"))
    parsedContent
  }

  def loadAckResponseTemplate(replaceFeedbackID: String, replaceNino: String) = {
    val fileName = s"conf/response/acknowledge/feedback-ack.json"
    val templateCotent =
      findResource(fileName).map(
        _.replace("replaceFeedbackID", replaceFeedbackID)
          .replace("replaceNino", replaceNino))

    val parsedContent = templateCotent
      .map(Json.parse).getOrElse(throw new IllegalStateException("Acknowledge template parsing failed"))
    parsedContent
  }

  def findResource(path: String): Option[String] = {
    val file = new File(path)
    val absolutePath = file.getAbsolutePath
    val stream = new FileInputStream(absolutePath)
    val json = try {
      Json.parse(stream)
    } finally {
      stream.close()
    }
    Some(json.toString)
  }
}

object StubResource extends StubResourceBase