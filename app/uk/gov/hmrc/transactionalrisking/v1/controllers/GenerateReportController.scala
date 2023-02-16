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

package uk.gov.hmrc.transactionalrisking.v1.controllers

import cats.data.EitherT
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.transactionalrisking.config.AppConfig
import uk.gov.hmrc.transactionalrisking.utils.ErrorToJsonConverter.convertErrorAsJson
import uk.gov.hmrc.transactionalrisking.utils.{CurrentDateTime, IdGenerator, Logging}
import uk.gov.hmrc.transactionalrisking.v1.connectors.MtdIdLookupConnector
import uk.gov.hmrc.transactionalrisking.v1.controllers.requestParsers.GenerateReportRequestParser
import uk.gov.hmrc.transactionalrisking.v1.models.domain._
import uk.gov.hmrc.transactionalrisking.v1.models.errors._
import uk.gov.hmrc.transactionalrisking.v1.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.v1.models.request.GenerateReportRawData
import uk.gov.hmrc.transactionalrisking.v1.services.cip.InsightService
import uk.gov.hmrc.transactionalrisking.v1.services.cip.models.FraudRiskRequest
import uk.gov.hmrc.transactionalrisking.v1.services.ifs.IfsService
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.NrsService
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.request.AssistReportGenerated
import uk.gov.hmrc.transactionalrisking.v1.services.rds.RdsService
import uk.gov.hmrc.transactionalrisking.v1.services.EnrolmentsAuthService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GenerateReportController @Inject()(
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
                                        )(implicit ec: ExecutionContext) extends AuthorisedController(cc) with BaseController with Logging {

  def generateReportInternal(nino: String, taxYear: String, calculationId:String): Action[AnyContent] = {

    implicit val correlationId: String = idGenerator.getUid
    logger.info(s"$correlationId::[generateReportInternal] Received request to generate an assessment report")

    val retrievalRequiredSwitch = config.authRetrievalRequired

    authorisedAction(nino, retrievalRequired = retrievalRequiredSwitch).async { implicit request =>
      val customerType = request.userDetails.toCustomerType
      val submissionTimestamp = currentDateTime.getDateTime()
      val responseData: EitherT[Future, ErrorWrapper, ResponseWrapper[AssessmentReportWrapper]] = for {
        assessmentRequestForSelfAssessment <- EitherT(requestParser.parseRequest(GenerateReportRawData(calculationId, nino, PreferredLanguage.English, customerType, None, taxYear)))
        fraudRiskReport <- EitherT(insightService.assess(generateFraudRiskRequest(assessmentRequestForSelfAssessment, request.headers.toMap.map { h => h._1 -> h._2.head })))
        rdsAssessmentReportWrapper <- EitherT(rdsService.submit(assessmentRequestForSelfAssessment, fraudRiskReport.responseData, Internal))
        _ <- EitherT(ifService.submitGenerateReportMessage(rdsAssessmentReportWrapper.responseData.report, rdsAssessmentReportWrapper.responseData.calculationTimestamp, assessmentRequestForSelfAssessment, rdsAssessmentReportWrapper.responseData.rdsAssessmentReport))
      } yield {
        rdsAssessmentReportWrapper
      }


      responseData.fold(
        errorWrapper =>
          errorHandler(errorWrapper, correlationId),
        reportWrapper => {
            //TODO for txr015, calculationTimestamp can be retrieved from reportWrapper.responseData.calculationTimestamp
            //TODO for txr015, need to make sure if there are any format issues with timestamp then need to be fixed in txr015

          nonRepudiationService.buildNrsSubmission(reportWrapper.responseData.report.stringify, reportWrapper.responseData.report.reportId.toString, submissionTimestamp, request, AssistReportGenerated)
            .fold(
              error => Future.successful(InternalServerError(convertErrorAsJson(DownstreamError))),
              success => {
                logger.info(s"$correlationId::[submit] Request initiated to store ${AssistReportGenerated.value} content to NRS")
                nonRepudiationService.submit(success)
                logger.info(s"$correlationId::[generateReport] ... report submitted to NRS")
                Future.successful(Ok(Json.toJson[AssessmentReport](reportWrapper.responseData.report)))
              }
            )
        }
      ).flatten.map(_.withApiHeaders(correlationId))
    }
  }

  def errorHandler(errorWrapper: ErrorWrapper,correlationId:String): Future[Result] = (errorWrapper.error,errorWrapper.errors) match {
    case (ServerError | DownstreamError,_) => Future(InternalServerError(convertErrorAsJson(DownstreamError)))
    case (NinoFormatError,_) => Future(BadRequest(convertErrorAsJson(NinoFormatError)))
    case (NoAssessmentFeedbackFromRDS,_) => Future(NoContent)
    case (TaxYearRangeInvalid,_) => Future(BadRequest(convertErrorAsJson(TaxYearRangeInvalid)))
    case (TaxYearFormatError,_) => Future(BadRequest(convertErrorAsJson(TaxYearFormatError)))
    case (CalculationIdFormatError,_) => Future(BadRequest(convertErrorAsJson(CalculationIdFormatError)))
    case (MatchingResourcesNotFoundError,_) => Future(NotFound(convertErrorAsJson(MatchingResourcesNotFoundError)))
    case (ClientOrAgentNotAuthorisedError,_) => Future(Forbidden(convertErrorAsJson(ClientOrAgentNotAuthorisedError)))
    case (InvalidCredentialsError,_) => Future(Unauthorized(convertErrorAsJson(InvalidCredentialsError)))
    case (RdsAuthError,_) => Future(InternalServerError(convertErrorAsJson(ForbiddenDownstreamError)))
    case (ServiceUnavailableError,_) => Future(InternalServerError(convertErrorAsJson(ServiceUnavailableError)))
    case (BadRequestError,Some(errs)) => Future(BadRequest(Json.toJson(errs)))
    case (BadRequestError,None) => Future(BadRequest(convertErrorAsJson(BadRequestError)))
    case (_,Some(errs)) =>
      logger.error(s"$correlationId::[generateReportInternal] Error in general scenario with multiple errors $errs")
      Future(InternalServerError(Json.toJson(errs)))
    case (error@_,None) =>
      logger.error(s"$correlationId::[generateReportInternal] Error handled in general scenario $error")
      Future(InternalServerError(convertErrorAsJson(MatchingResourcesNotFoundError)))
  }

  private def generateFraudRiskRequest(request: AssessmentRequestForSelfAssessment,fraudRiskHeaders:FraudRiskRequest.FraudRiskHeaders): FraudRiskRequest = {
    val fraudRiskRequest =new FraudRiskRequest(
      nino= Some(request.nino),
      taxYear = Some(request.taxYear),
      fraudRiskHeaders=fraudRiskHeaders
    )
    fraudRiskRequest
  }
}
