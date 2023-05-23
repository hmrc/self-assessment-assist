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

package uk.gov.hmrc.selfassessmentassist.api.models.errors

import play.api.http.Status._


//NRS error
object NrsError extends MtdError("NRS_SUBMISSION_FAILURE", "The submission to NRS from MDTP failed")

object FormatReportIdError extends MtdError("FORMAT_REPORT_ID", "The provided Report ID is invalid")

// Format Errors
object NinoFormatError extends MtdError("FORMAT_NINO", "The provided NINO is invalid")

object NinoFormatDesError extends MtdError("FORMAT_NINO", "The provided NINO is invalid", UNAUTHORIZED)

object NinoNotFound extends MtdError("NINO_NOT_FOUND", "The provided NINO was not found")

object CalculationIdFormatError extends MtdError(code = "FORMAT_CALC_ID", message = "The provided Calculation ID is invalid")

object TaxYearFormatError extends MtdError("FORMAT_TAX_YEAR", "The provided tax year is invalid")

object TaxYearRangeInvalid extends MtdError("RULE_TAX_YEAR_RANGE_INVALID", "Tax year range invalid. A tax year range of one year is required.")

// Standard Errors
object ResourceNotFoundError extends MtdError("RESOURCE_NOT_FOUND", "Matching resource not found", NOT_FOUND)

object InternalError extends MtdError("INTERNAL_SERVER_ERROR", "An internal server error occurred", INTERNAL_SERVER_ERROR)

object BadRequestError extends MtdError("INVALID_REQUEST", "Invalid request", BAD_REQUEST)

object ServiceUnavailableError extends MtdError("SERVICE_UNAVAILABLE", "Internal server error", INTERNAL_SERVER_ERROR)

object InvalidJson extends MtdError("INVALID_JSON", "Invalid JSON received")

// Authorisation Errors
object UnauthorisedError extends MtdError("CLIENT_OR_AGENT_NOT_AUTHORISED", "The client and/or agent is not authorised", FORBIDDEN)

object InvalidBearerTokenError extends MtdError("UNAUTHORIZED", "Bearer token is missing or not authorized", UNAUTHORIZED)

object InvalidCredentialsError extends MtdError("INVALID_CREDENTIALS", "Invalid Authentication information provided", FORBIDDEN)

object BearerTokenExpiredError extends MtdError("INVALID_CREDENTIALS", "Invalid Authentication information provided", FORBIDDEN)

// Legacy Authorisation Errors
object LegacyUnauthorisedError extends MtdError("CLIENT_OR_AGENT_NOT_AUTHORISED", "The client and/or agent is not authorised.", FORBIDDEN)

object ClientOrAgentNotAuthorisedError extends MtdError("CLIENT_OR_AGENT_NOT_AUTHORISED", "The client or agent is not authorised", FORBIDDEN)

object ForbiddenDownstreamError extends MtdError(code = "Forbidden", message = "Request not authorised, forbidden", FORBIDDEN)

object ForbiddenRDSCorrelationIdError
  extends MtdError(
    code = "CORRELATION_ID_NOT_AUTHORISED",
    message = "The Correlation ID is not the expected value for this report",
    INTERNAL_SERVER_ERROR
  )

// Accept header Errors
object InvalidAcceptHeaderError extends MtdError("ACCEPT_HEADER_INVALID", "The accept header is missing or invalid", NOT_ACCEPTABLE)

object UnsupportedVersionError extends MtdError("NOT_FOUND", "The requested resource could not be found", NOT_FOUND)

object InvalidBodyTypeError extends MtdError("INVALID_BODY_TYPE", "Expecting text/json or application/json body")

object MatchingCalculationIDNotFoundError
  extends MtdError("MATCHING_CALCULATION_ID_NOT_FOUND", "The Calculation ID was not found at this time. You can try again later.")

object MatchingResourcesNotFoundError extends MtdError("MATCHING_RESOURCE_NOT_FOUND", "A resource with the name in the request can not be found.")

object RdsAuthError extends MtdError("RDS_AUTH_ERROR", "RDS authorisation could not be accomplished", FORBIDDEN)

object ServerError extends MtdError("SERVER_ERROR", "Server error", INTERNAL_SERVER_ERROR)

object NoAssessmentFeedbackFromRDS extends MtdError("204", "No Content")
