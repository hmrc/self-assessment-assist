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

package uk.gov.hmrc.selfassessmentassist.v1

import cats.data.EitherT
import com.google.inject.internal.ErrorsException
import play.api.http.Status.SERVICE_UNAVAILABLE
import play.api.mvc.{AnyContent, Result}
import play.api.mvc.Results.{BadRequest, Forbidden, InternalServerError, NoContent, ServiceUnavailable}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.selfassessmentassist.api.controllers.UserRequest
import uk.gov.hmrc.selfassessmentassist.api.models.errors._
import uk.gov.hmrc.selfassessmentassist.api.models.outcomes.ResponseWrapper
import uk.gov.hmrc.selfassessmentassist.utils.ErrorToJsonConverter.convertErrorAsJson
import uk.gov.hmrc.selfassessmentassist.utils.Logging
import uk.gov.hmrc.selfassessmentassist.v1.models.request.nrs.{AcknowledgeReportId, AcknowledgeReportRequest, AssistReportAcknowledged, NrsSubmission}
import uk.gov.hmrc.selfassessmentassist.v1.models.response.rds.RdsAssessmentReport
import uk.gov.hmrc.selfassessmentassist.v1.services.{IfsService, NrsService, RdsService, ServiceOutcome}

import java.time.OffsetDateTime
import scala.concurrent.{ExecutionContext, Future}

class Orchestrator(rdsService: RdsService, ifsService: IfsService, nrsService: NrsService)(implicit ec: ExecutionContext) extends Logging {

  def orchestrate(parsedRequest: AcknowledgeReportRequest,
                  rawRequest: UserRequest[AnyContent],
                  reportId: String,
                  submissionTimestamp: OffsetDateTime)(implicit hc: HeaderCarrier, correlationId: String): Future[ServiceOutcome[Unit]] = {

    for {
      rdsAssessmentReport <- handleRdsAcknowledgement(parsedRequest, rawRequest)
      response            <- Future(handleNrsSubmission(rdsAssessmentReport, reportId, submissionTimestamp, rawRequest))
    } yield response
  }

  private def handleRdsAcknowledgement(
      parsedRequest: AcknowledgeReportRequest,
      rawRequest: UserRequest[AnyContent]
  )(implicit hc: HeaderCarrier, correlationId: String): Future[Either[ErrorWrapper, RdsAssessmentReport]] = {

    val rdsResponse = for {
      serviceResponse <- EitherT(rdsService.acknowledge(parsedRequest))
      _               <- EitherT(ifsService.submitAcknowledgementMessage(parsedRequest, serviceResponse.responseData, rawRequest.userDetails))
    } yield {
      serviceResponse.responseData
    }

    rdsResponse.value.map {
      case Left(errorWrapper: ErrorWrapper) => Left(errorHandler(errorWrapper, correlationId))
      case Right(response)                  => Right(response)
    }
  }

  private def handleNrsSubmission(assessmentReport: Either[ErrorWrapper, RdsAssessmentReport],
                                  reportId: String,
                                  submissionTimestamp: OffsetDateTime,
                                  request: UserRequest[AnyContent])(implicit hc: HeaderCarrier, correlationId: String) = {
    for {
      report        <- assessmentReport
      nrsSubmission <- buildNrsSubmission(reportId, submissionTimestamp, request, report)
      _ = nrsService.submit(nrsSubmission)
    } yield ResponseWrapper(correlationId, ())

  }

  private def buildNrsSubmission(
      reportId: String,
      submissionTimestamp: OffsetDateTime,
      request: UserRequest[AnyContent],
      rdsReport: RdsAssessmentReport)(implicit hc: HeaderCarrier, correlationId: String): Either[ErrorWrapper, NrsSubmission] = {

    logger.debug(s"$correlationId::[acknowledgeReport] ... RDS acknowledge status ${rdsReport.responseCode}")
    nrsService
      .buildNrsSubmission(AcknowledgeReportId(reportId).stringify, reportId, submissionTimestamp, request, AssistReportAcknowledged) match {
      case Left(_)           => Left(ErrorWrapper(correlationId, InternalError))
      case Right(submission) => Right(submission)
    }
  }

  private def errorHandler(errorWrapper: ErrorWrapper, correlationId: String): ErrorWrapper = (errorWrapper.error, errorWrapper.errors) match {
    case (ServerError | InternalError | ServiceUnavailableError, _) => ErrorWrapper(correlationId, InternalError)
    case (ForbiddenDownstreamError, _)                              => ErrorWrapper(correlationId, ForbiddenDownstreamError)
    case (ForbiddenRDSCorrelationIdError, _)                        => ErrorWrapper(correlationId, ForbiddenRDSCorrelationIdError)
    case (FormatReportIdError, _)                                   => ErrorWrapper(correlationId, FormatReportIdError)
    case (ClientOrAgentNotAuthorisedError, _)                       => ErrorWrapper(correlationId, ClientOrAgentNotAuthorisedError)
    case (NinoFormatError, _)                                       => ErrorWrapper(correlationId, NinoFormatError)
    case (MatchingResourcesNotFoundError | ResourceNotFoundError, _) =>
      ErrorWrapper(correlationId, ServiceUnavailableError.copy(httpStatus = SERVICE_UNAVAILABLE))
    case (_, Some(errs)) =>
      logger.error(s"$correlationId::[AcknowledgeReportController] Error handled in general scenario with multiple errors $errs")
      ErrorWrapper(correlationId, InternalError.copy(httpStatus = SERVICE_UNAVAILABLE))
    case (error @ _, None) =>
      logger.error(s"$correlationId::[AcknowledgeReportController] Error handled in general scenario $error")
      ErrorWrapper(correlationId, InternalError.copy(httpStatus = SERVICE_UNAVAILABLE))
  }

}
