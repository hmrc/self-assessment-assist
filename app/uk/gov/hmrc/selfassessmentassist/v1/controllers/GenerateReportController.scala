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
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.selfassessmentassist.api.connectors.MtdIdLookupConnector
import uk.gov.hmrc.selfassessmentassist.api.controllers.{ApiBaseController, AuthorisedController}
import uk.gov.hmrc.selfassessmentassist.api.models.domain.PreferredLanguage
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{BadRequestError, CalculationIdFormatError, ClientOrAgentNotAuthorisedError, DownstreamError, ErrorWrapper, ForbiddenDownstreamError, InvalidCredentialsError, MatchingCalculationIDNotFoundError, MatchingResourcesNotFoundError, NinoFormatError, NoAssessmentFeedbackFromRDS, RdsAuthError, ServerError, ServiceUnavailableError, TaxYearFormatError, TaxYearRangeInvalid}
import uk.gov.hmrc.selfassessmentassist.api.models.outcomes.ResponseWrapper
import uk.gov.hmrc.selfassessmentassist.config.AppConfig
import uk.gov.hmrc.selfassessmentassist.utils.ErrorToJsonConverter.convertErrorAsJson
import uk.gov.hmrc.selfassessmentassist.utils.{CurrentDateTime, IdGenerator, Logging}
import uk.gov.hmrc.selfassessmentassist.v1.models.domain._
import uk.gov.hmrc.selfassessmentassist.v1.models.request.GenerateReportRawData
import uk.gov.hmrc.selfassessmentassist.v1.models.request.cip.FraudRiskRequest
import uk.gov.hmrc.selfassessmentassist.v1.models.request.nrs.AssistReportGenerated
import uk.gov.hmrc.selfassessmentassist.v1.requestParsers.GenerateReportRequestParser
import uk.gov.hmrc.selfassessmentassist.v1.services._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GenerateReportController @Inject() (
    val cc: ControllerComponents,
    requestParser: GenerateReportRequestParser,
    val authService: EnrolmentsAuthService,
    val lookupConnector: MtdIdLookupConnector,
    nonRepudiationService: NrsService,
    insightService: InsightService,
    rdsService: RdsService,
    ifService: IfsService,
    currentDateTime: CurrentDateTime,
    idGenerator: IdGenerator,
    config: AppConfig
)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc)
    with ApiBaseController
    with BaseController
    with Logging {

  def generateReportInternal(nino: String, taxYear: String, calculationId: String): Action[AnyContent] = {

    implicit val correlationId: String = idGenerator.getUid
    logger.info(s"$correlationId::[generateReportInternal] Received request to generate an assessment report")

    authorisedAction(nino).async { implicit request =>
      val customerType        = request.userDetails.toCustomerType
      val submissionTimestamp = currentDateTime.getDateTime
      val responseData: EitherT[Future, ErrorWrapper, ResponseWrapper[AssessmentReportWrapper]] =
        for {
          assessmentRequestForSelfAssessment <- EitherT(
            requestParser.parseRequest(GenerateReportRawData(calculationId, nino, PreferredLanguage.English, customerType, None, taxYear)))
          fraudRiskReport <- EitherT(
            insightService.assess(generateFraudRiskRequest(assessmentRequestForSelfAssessment, request.headers.toMap.map { h => h._1 -> h._2.head })))
          rdsAssessmentReportWrapper <- EitherT(rdsService.submit(assessmentRequestForSelfAssessment, fraudRiskReport.responseData))
          _ <- EitherT(
            ifService.submitGenerateReportMessage(
              rdsAssessmentReportWrapper.responseData.report,
              rdsAssessmentReportWrapper.responseData.calculationTimestamp,
              assessmentRequestForSelfAssessment,
              rdsAssessmentReportWrapper.responseData.rdsAssessmentReport
            ))
        } yield {
          rdsAssessmentReportWrapper
        }

      responseData
        .fold(
          errorWrapper => errorHandler(errorWrapper, correlationId),
          reportWrapper => {
            nonRepudiationService
              .buildNrsSubmission(
                reportWrapper.responseData.report.stringify,
                reportWrapper.responseData.report.reportId.toString,
                submissionTimestamp,
                request,
                AssistReportGenerated)
              .fold(
                _ => Future.successful(InternalServerError(convertErrorAsJson(DownstreamError))),
                success => {
                  logger.debug(s"$correlationId::[generateReport] Request initiated to store ${AssistReportGenerated.value} content to NRS")
                  nonRepudiationService.submit(success)
                  logger.debug(s"$correlationId::[generateReport] ... report submitted to NRS")
                  Future.successful(Ok(Json.toJson[AssessmentReport](reportWrapper.responseData.report)))
                }
              )
          }
        )
        .flatten
        .map(_.withApiHeaders(correlationId))
    }
  }

  def errorHandler(errorWrapper: ErrorWrapper, correlationId: String): Future[Result] = (errorWrapper.error, errorWrapper.errors) match {
    case (ServerError | DownstreamError, _)      => Future(InternalServerError(convertErrorAsJson(DownstreamError)))
    case (NinoFormatError, _)                    => Future(BadRequest(convertErrorAsJson(NinoFormatError)))
    case (NoAssessmentFeedbackFromRDS, _)        => Future(NoContent)
    case (TaxYearRangeInvalid, _)                => Future(BadRequest(convertErrorAsJson(TaxYearRangeInvalid)))
    case (TaxYearFormatError, _)                 => Future(BadRequest(convertErrorAsJson(TaxYearFormatError)))
    case (CalculationIdFormatError, _)           => Future(BadRequest(convertErrorAsJson(CalculationIdFormatError)))
    case (MatchingResourcesNotFoundError, _)     => Future(NotFound(convertErrorAsJson(MatchingResourcesNotFoundError)))
    case (MatchingCalculationIDNotFoundError, _) => Future(NotFound(convertErrorAsJson(MatchingCalculationIDNotFoundError)))
    case (ClientOrAgentNotAuthorisedError, _)    => Future(Forbidden(convertErrorAsJson(ClientOrAgentNotAuthorisedError)))
    case (InvalidCredentialsError, _)            => Future(Unauthorized(convertErrorAsJson(InvalidCredentialsError)))
    case (RdsAuthError, _)                       => Future(InternalServerError(convertErrorAsJson(ForbiddenDownstreamError)))
    case (ServiceUnavailableError, _)            => Future(InternalServerError(convertErrorAsJson(ServiceUnavailableError)))
    case (BadRequestError, Some(errs))           => Future(BadRequest(Json.toJson(errs)))
    case (BadRequestError, None)                 => Future(BadRequest(convertErrorAsJson(BadRequestError)))
    case (_, Some(errs)) =>
      logger.error(s"$correlationId::[generateReportInternal] Error in general scenario with multiple errors $errs")
      Future(InternalServerError(Json.toJson(errs)))
    case (error @ _, None) =>
      logger.error(s"$correlationId::[generateReportInternal] Error handled in general scenario $error")
      Future(InternalServerError(convertErrorAsJson(MatchingResourcesNotFoundError)))
  }

  private def generateFraudRiskRequest(request: AssessmentRequestForSelfAssessment,
                                       fraudRiskHeaders: FraudRiskRequest.FraudRiskHeaders): FraudRiskRequest = {
    val fraudRiskRequest = new FraudRiskRequest(
      nino = Some(request.nino),
      taxYear = Some(request.taxYear),
      fraudRiskHeaders = fraudRiskHeaders
    )
    fraudRiskRequest
  }

}
