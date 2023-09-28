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

package uk.gov.hmrc.selfassessmentassist.v1.utils

import play.api.Logging
import play.api.http.ContentTypes
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results

import java.io.{File, FileInputStream}
import java.net.URLDecoder

trait StubResourceBase extends Results with ContentTypes with Logging {

  def loadSubmitResponseTemplate(calculationId: String, replaceFeedbackId: String, replaceCorrelationId: String): JsValue = {
    val fileName = s"response/submit/$calculationId-response.json"
    val templateContent =
      findResource(fileName).map(
        _.replace("replaceFeedbackId", replaceFeedbackId)
          .replace("replaceCalculationId", calculationId)
          .replace("replaceCorrelationId", replaceCorrelationId)
      )

    val parsedContent = templateContent
      .map(Json.parse)
      .getOrElse(throw new IllegalStateException("Response template parsing failed"))
    parsedContent
  }

  def loadAckResponseTemplate(feedbackId: String, nino: String, responseCode: Int): JsValue = {
    val fileName = s"response/acknowledge/feedback-ack-$responseCode.json"
    val templateContent: Option[String] =
      findResource(fileName).map(
        _.replace("replaceFeedbackId", feedbackId)
          .replace("replaceNino", nino))

    val parsedContent: JsValue = templateContent
      .map(Json.parse)
      .getOrElse(throw new IllegalStateException("Acknowledge template parsing failed"))
    parsedContent
  }

  def findResource(path: String): Option[String] = {
    val classLoader  = getClass.getClassLoader
    val resourcePath = classLoader.getResource(path)

    val decodedPath = URLDecoder.decode(resourcePath.getFile, "UTF-8")

    val file         = new File(decodedPath)
    val absolutePath = file.getPath
    val stream       = new FileInputStream(absolutePath)
    val json =
      try {
        Json.parse(stream)
      } finally {
        stream.close()
      }
    Some(json.toString)
  }

}

object StubResource extends StubResourceBase
