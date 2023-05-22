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

package uk.gov.hmrc.selfassessmentassist.v1.controllers

import cats.data.EitherT
import play.api.mvc._
import uk.gov.hmrc.selfassessmentassist.api.connectors.MtdIdLookupConnector
import uk.gov.hmrc.selfassessmentassist.api.controllers.{ApiBaseController, AuthorisedController}
import uk.gov.hmrc.selfassessmentassist.api.models.errors._
import uk.gov.hmrc.selfassessmentassist.config.AppConfig
import uk.gov.hmrc.selfassessmentassist.utils.ErrorToJsonConverter.convertErrorAsJson
import uk.gov.hmrc.selfassessmentassist.utils.{CurrentDateTime, IdGenerator, Logging}
import uk.gov.hmrc.selfassessmentassist.v1.models.request.AcknowledgeReportRawData
import uk.gov.hmrc.selfassessmentassist.v1.models.request.nrs.{AcknowledgeReportId, AssistReportAcknowledged}
import uk.gov.hmrc.selfassessmentassist.v1.models.response.rds.RdsAssessmentReport
import uk.gov.hmrc.selfassessmentassist.v1.requestParsers.AcknowledgeRequestParser
import uk.gov.hmrc.selfassessmentassist.v1.services.{EnrolmentsAuthService, IfsService, NrsService, RdsService}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AcknowledgeReportController @Inject() (
    val cc: ControllerComponents,
    requestParser: AcknowledgeRequestParser,
    val authService: EnrolmentsAuthService,
    val lookupConnector: MtdIdLookupConnector,
    nonRepudiationService: NrsService,
    rdsService: RdsService,
    currentDateTime: CurrentDateTime,
    idGenerator: IdGenerator,
    ifsService: IfsService,
    config: AppConfig
)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc)
    with ApiBaseController
    with BaseController
    with Logging {

  def acknowledgeReportForSelfAssessment(nino: String, reportId: String, rdsCorrelationId: String): Action[AnyContent] = {
    implicit val correlationId: String = idGenerator.getUid
    logger.debug(s"$correlationId::[acknowledgeReportForSelfAssessment]Received request to acknowledge assessment report")

    val submissionTimestamp = currentDateTime.getDateTime

    authorisedAction(nino).async { implicit request =>
      val processRequest: EitherT[Future, ErrorWrapper, RdsAssessmentReport] = for {
        parsedRequest   <- EitherT(requestParser.parseRequest(AcknowledgeReportRawData(nino, reportId, rdsCorrelationId)))
        serviceResponse <- EitherT(rdsService.acknowledge(parsedRequest))
        _               <- EitherT(ifsService.submitAcknowledgementMessage(parsedRequest, serviceResponse.responseData, request.userDetails))
      } yield {
        serviceResponse.responseData
      }

      processRequest
        .fold(
          errorWrapper => errorHandler(errorWrapper, correlationId),
          assessmentReport => {
            logger.debug(s"$correlationId::[acknowledgeReport] ... RDS acknowledge status ${assessmentReport.responseCode}")
            nonRepudiationService
              .buildNrsSubmission(AcknowledgeReportId(reportId).stringify, reportId, submissionTimestamp, request, AssistReportAcknowledged)
              .fold(
                _ => {
                  logger.error(s"$correlationId::[acknowledgeReport] NRS event generation failed")
                  Future.successful(InternalServerError(convertErrorAsJson(DownstreamError)))
                },
                success => {
                  logger.debug(s"$correlationId::[acknowledgeReport] Request initiated to store ${AssistReportAcknowledged.value} content to NRS")
                  nonRepudiationService.submit(success)
                  logger.debug(s"$correlationId::[acknowledgeReport] ... report submitted to NRS")
                  Future.successful(NoContent)
                }
              )
          }
        )
        .flatten
        .map(_.withApiHeaders(correlationId))
    }
  }

  def errorHandler(errorWrapper: ErrorWrapper, correlationId: String): Future[Result] = (errorWrapper.error, errorWrapper.errors) match {
    case (ServerError | DownstreamError | ServiceUnavailableError, _) => Future.successful(InternalServerError(convertErrorAsJson(DownstreamError)))
    case (ForbiddenDownstreamError, _)                                => Future.successful(Forbidden(convertErrorAsJson(ForbiddenDownstreamError)))
    case (ForbiddenRDSCorrelationIdError, _)  => Future.successful(Forbidden(convertErrorAsJson(ForbiddenRDSCorrelationIdError)))
    case (FormatReportIdError, _)             => Future.successful(BadRequest(convertErrorAsJson(FormatReportIdError)))
    case (ClientOrAgentNotAuthorisedError, _) => Future.successful(Forbidden(convertErrorAsJson(ClientOrAgentNotAuthorisedError)))
    case (NinoFormatError, _)                 => Future.successful(BadRequest(convertErrorAsJson(NinoFormatError)))
    case (MatchingResourcesNotFoundError | ResourceNotFoundError, _) =>
      Future.successful(ServiceUnavailable(convertErrorAsJson(ServiceUnavailableError)))
    case (_, Some(errs)) =>
      logger.error(s"$correlationId::[AcknowledgeReportController] Error handled in general scenario with multiple errors $errs")
      Future.successful(ServiceUnavailable(convertErrorAsJson(DownstreamError)))
    case (error @ _, None) =>
      logger.error(s"$correlationId::[AcknowledgeReportController] Error handled in general scenario $error")
      Future.successful(ServiceUnavailable(convertErrorAsJson(DownstreamError)))
  }

}
