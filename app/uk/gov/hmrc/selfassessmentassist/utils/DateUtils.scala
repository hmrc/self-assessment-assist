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

package uk.gov.hmrc.selfassessmentassist.utils

import play.api.libs.json.*

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, OffsetDateTime, ZoneOffset}
import scala.util.{Failure, Success, Try}

object DateUtils {

  val isoInstantDateTimePattern: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
  val dateTimePattern: DateTimeFormatter           = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  val datePattern: DateTimeFormatter               = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  implicit def dateTimeWrites: Writes[OffsetDateTime] = (localDateTime: OffsetDateTime) => JsString(localDateTime.format(dateTimePattern))

  implicit def dateTimeReads: Reads[OffsetDateTime] = (json: JsValue) => {
    Try(json.as[String]) match {
      case Success(value) => JsSuccess(OffsetDateTime.parse(value))
      case Failure(_)     => JsError()
    }
  }

  val dateTimeFormat: Format[OffsetDateTime] = Format[OffsetDateTime](
    dateTimeReads,
    dateTimeWrites
  )

  implicit def isoInstantDateTimeWrites: Writes[OffsetDateTime] = (localDateTime: OffsetDateTime) =>
    JsString(localDateTime.format(isoInstantDateTimePattern))

  implicit def isoInstantDateTimeReads: Reads[OffsetDateTime] = (json: JsValue) =>
    Try(JsSuccess(OffsetDateTime.parse(json.as[String], isoInstantDateTimePattern), JsPath)).getOrElse(JsError())

  implicit val offsetDateTimeFromLocalDateTimeFormatReads: Reads[OffsetDateTime] = { json =>
    json.as[String].parseOffsetDateTimeFromLocalDateTimeFormat() match {
      case Right(value) => JsSuccess(value)
      case Left(error)  => JsError("not a valid date " + error.errorMessage)
    }
  }

  implicit class LocalDateTimeExtensions(localDateTime: LocalDateTime) {
    def utcOffset: OffsetDateTime = localDateTime.atOffset(ZoneOffset.UTC)
  }

  implicit class StringExtensions(string: String) {

    def parseOffsetDateTimeFromLocalDateTimeFormat(
        formatter: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME): Either[DateError, OffsetDateTime] = {

      Try(LocalDateTime.parse(string, formatter).utcOffset) match {
        case Success(offsetDateTime) => Right(offsetDateTime)
        case Failure(exception)      => Left(DateParseError(exception.getMessage, string))
      }
    }

  }

  sealed trait DateError {
    val errorMessage: String
    val dateFailedToParse: String
  }

  case class DateParseError(e: String, dateFailed: String) extends DateError {
    val dateFailedToParse: String = dateFailed
    val errorMessage: String      = e
  }

  implicit def defaultDateTimeReads: Reads[OffsetDateTime] = (json: JsValue) =>
    Try(JsSuccess(OffsetDateTime.parse(json.as[String], isoInstantDateTimePattern), JsPath)).getOrElse(JsError())

  val defaultDateTimeFormat: Format[OffsetDateTime] = Format[OffsetDateTime](
    defaultDateTimeReads,
    dateTimeWrites
  )

  implicit def dateWrites: Writes[LocalDate] = (localDate: LocalDate) => JsString(localDate.format(datePattern))

  implicit def dateReads: Reads[LocalDate] = (json: JsValue) =>
    Try(JsSuccess(LocalDate.parse(json.as[String], datePattern), JsPath)).getOrElse(JsError())

}
