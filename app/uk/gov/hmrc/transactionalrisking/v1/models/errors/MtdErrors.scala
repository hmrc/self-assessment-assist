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

package uk.gov.hmrc.transactionalrisking.v1.models.errors

import play.api.libs.json._

case class MtdError(code: String, message: String, customJson: Option[JsValue] = None){
  lazy val toJson: JsValue = Json.obj(
    "code" -> this.code,
    "message" -> this.message
  )
}

object MtdError {
  implicit val writes: Writes[MtdError] = {
    case o@MtdError(_, _, None) => o.toJson
    case MtdError("INVALID_REQUEST", _, Some(customJson)) => BadRequestError.toJson.as[JsObject] + ("errors" -> Json.toJson(Seq(customJson)))
    case MtdError(_, _, Some(customJson)) => customJson
  }

  implicit def genericWrites[T <: MtdError]: Writes[T] =
    writes.contramap[T](c => c: MtdError)

  implicit val reads: Reads[MtdError] = Json.reads[MtdError]
}

case class MtdErrorWrapper(code: String, message: String, path: Option[String], errors: Option[Seq[MtdErrorWrapper]] = None)

object MtdErrorWrapper {
  implicit val writes: OWrites[MtdErrorWrapper] = Json.writes[MtdErrorWrapper]

  implicit def genericWrites[T <: MtdErrorWrapper]: OWrites[T] =
    writes.contramap[T](c => c: MtdErrorWrapper)

  implicit val reads: Reads[MtdErrorWrapper] = Json.reads[MtdErrorWrapper]
}

//NRS error
object NrsError extends MtdError("NRS_SUBMISSION_FAILURE", "The submission to NRS from MDTP failed")

object FormatReportIdError extends MtdError("FORMAT_REPORT_ID", "The provided Report ID is invalid")

// Format Errors
object NinoFormatError extends MtdError("FORMAT_NINO", "The provided NINO is invalid")
object NinoFormatErrorDes extends MtdError("NINO_INVALID", "The provided NINO is invalid")
object NinoNotFound extends MtdError("NINO_NOT_FOUND", "The provided NINO was not found")
object CalculationIdFormatError
  extends MtdError(code = "FORMAT_CALC_ID",
  message = "The provided Calculation ID is invalid"
  )
object TaxYearFormatError extends MtdError("FORMAT_TAX_YEAR", "The provided tax year is invalid")
object TaxYearRangeInvalid extends MtdError("RULE_TAX_YEAR_RANGE_INVALID", "Tax year range invalid. A tax year range of one year is required.")

// Standard Errors
object ResourceNotFoundError extends MtdError("RESOURCE_NOT_FOUND", "Matching resource not found")
object DownstreamError extends MtdError("INTERNAL_SERVER_ERROR", "An internal server error occurred")
object BadRequestError extends MtdError("INVALID_REQUEST", "Invalid request")
object ServiceUnavailableError extends MtdError("SERVICE_UNAVAILABLE", "Internal server error")
object InvalidJson extends MtdError("INVALID_JSON", "Invalid JSON received")
object UnexpectedFailure {
  def trError(status: Int, body: String): MtdError = MtdError("UNEXPECTED_FAILURE", s"Unexpected failure. Status $status, body $body")
}

// Authorisation Errors
object UnauthorisedError extends MtdError("CLIENT_OR_AGENT_NOT_AUTHORISED", "The client and/or agent is not authorised")
object InvalidBearerTokenError extends MtdError("UNAUTHORIZED", "Bearer token is missing or not authorized")
object InvalidCredentialsError extends MtdError("INVALID_CREDENTIALS", "Invalid Authentication information provided")
object BearerTokenExpiredError extends MtdError("INVALID_CREDENTIALS", "Invalid Authentication information provided")

// Legacy Authorisation Errors
object LegacyUnauthorisedError extends MtdError("CLIENT_OR_AGENT_NOT_AUTHORISED", "The client and/or agent is not authorised.")
object ClientOrAgentNotAuthorisedError extends MtdError("CLIENT_OR_AGENT_NOT_AUTHORISED", "The client or agent is not authorised")
object ForbiddenDownstreamError extends MtdError(
  code = "INTERNAL_SERVER_ERROR",
  message = "An internal server error occurred",
  customJson = Some(
    Json.parse(
      """
        |{
        |  "code": "INTERNAL_SERVER_ERROR",
        |  "message": "An internal server error occurred, forbidden"
        |}
      """.stripMargin
    )
  )
)

// Accept header Errors
object InvalidAcceptHeaderError extends MtdError("ACCEPT_HEADER_INVALID", "The accept header is missing or invalid")
object UnsupportedVersionError extends MtdError("NOT_FOUND", "The requested resource could not be found")
object InvalidBodyTypeError extends MtdError("INVALID_BODY_TYPE", "Expecting text/json or application/json body")

object MatchingResourcesNotFoundError extends MtdError("MATCHING_RESOURCE_NOT_FOUND", "The Calculation ID was not found at this time, try again later")
object RdsAuthError extends MtdError("RDS_AUTH_ERROR", "RDS authorisation could not be accomplished")

object ServerError extends MtdError("SERVER_ERROR", "Server error")
object NoAssessmentFeedbackFromRDS extends MtdError("204", "No Content")
