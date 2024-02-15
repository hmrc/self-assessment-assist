/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.selfassessmentassist.api.connectors.httpParsers

import play.api.libs.json._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{DownstreamError, DownstreamErrorCode, DownstreamErrors, InternalError, OutboundError}
import uk.gov.hmrc.selfassessmentassist.utils.Logging

import scala.util.Try

trait HttpParser extends Logging {

  implicit class KnownJsonResponse(response: HttpResponse) {

    def validateJson[T](implicit reads: Reads[T]): Option[T] = {
      Try(response.json).fold(_ => None, json => parseResult(json))
    }

    def parseResult[T](json: JsValue)(implicit reads: Reads[T]): Option[T] = json.validate[T] match {
      case JsSuccess(value, _) => Some(value)
      case JsError(error) =>
        logger.warn(s"[KnownJsonResponse][validateJson] Unable to parse JSON: $error")
        None
    }

  }

  private val multipleErrorReads: Reads[Seq[DownstreamErrorCode]] = (__ \ "failures").read[Seq[DownstreamErrorCode]]

  def retrieveCorrelationId(response: HttpResponse): String = response.header("CorrelationId").getOrElse("")

  def parseErrors(response: HttpResponse): DownstreamError = {
    val singleError         = response.validateJson[DownstreamErrorCode].map(err => DownstreamErrors(List(err)))
    lazy val multipleErrors = response.validateJson(multipleErrorReads).map(errs => DownstreamErrors(errs))

    lazy val unableToParseJsonError = {
      logger.warn(s"unable to parse errors from response: ${response.body}")
      OutboundError(InternalError)
    }

    singleError orElse multipleErrors getOrElse unableToParseJsonError
  }

}
