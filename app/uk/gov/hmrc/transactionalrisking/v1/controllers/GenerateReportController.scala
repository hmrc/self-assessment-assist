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
import uk.gov.hmrc.transactionalrisking.utils.{CurrentDateTime, IdGenerator, Logging}
import uk.gov.hmrc.transactionalrisking.v1.connectors.MtdIdLookupConnector
import uk.gov.hmrc.transactionalrisking.v1.models.domain._
import uk.gov.hmrc.transactionalrisking.v1.models.errors._
import uk.gov.hmrc.transactionalrisking.v1.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.v1.services.cip.InsightService
import uk.gov.hmrc.transactionalrisking.v1.services.cip.models.FraudRiskRequest
import uk.gov.hmrc.transactionalrisking.v1.services.eis.IntegrationFrameworkService
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.NrsService
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.request.AssistReportGenerated
import uk.gov.hmrc.transactionalrisking.v1.services.rds.RdsService
import uk.gov.hmrc.transactionalrisking.v1.services.{EnrolmentsAuthService, ServiceOutcome}

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class GenerateReportController @Inject()(
                                          val cc: ControllerComponents, //TODO add request parser
                                          val integrationFrameworkService: IntegrationFrameworkService,
                                          val authService: EnrolmentsAuthService,
                                          val lookupConnector: MtdIdLookupConnector,
                                          nonRepudiationService: NrsService,
                                          insightService: InsightService,
                                          rdsService: RdsService,
                                          currentDateTime: CurrentDateTime,
                                          idGenerator: IdGenerator
                                        )(implicit ec: ExecutionContext) extends AuthorisedController(cc) with BaseController with Logging {

  def generateReportInternal(nino: String, calculationId: String): Action[AnyContent] = {

    implicit val correlationId: String = idGenerator.getUid
    logger.info(s"$correlationId::[generateReportInternal] Received request to generate an assessment report")

    authorisedAction(nino, nrsRequired = true).async { implicit request =>
      val customerType = request.userDetails.toCustomerType
      val submissionTimestamp = currentDateTime.getDateTime()

      toId(calculationId).map { calculationIdUuid =>

        val responseData: EitherT[Future, ErrorWrapper, ResponseWrapper[AssessmentReportWrapper]] = for {
          calculationInfo                       <- EitherT(getCalculationInfo(calculationIdUuid, nino))
          assessmentRequestForSelfAssessment    = AssessmentRequestForSelfAssessment(calculationIdUuid,
                                                  nino,
                                                  PreferredLanguage.English,
                                                  customerType,
                                                  None,
                                                  DesTaxYear.fromMtd(calculationInfo.responseData.taxYear).toString)

          fraudRiskReport                       <- EitherT(insightService.assess(generateFraudRiskRequest(assessmentRequestForSelfAssessment,request.headers.toMap.map { h => h._1 -> h._2.head })))
          rdsAssessmentReportWrapper            <- EitherT(rdsService.submit(assessmentRequestForSelfAssessment, fraudRiskReport.responseData, Internal))
        } yield rdsAssessmentReportWrapper

        responseData.fold(
          errorWrapper =>
            errorHandler(errorWrapper, correlationId),
          reportWrapper => {
            //TODO for txr015, calculationTimestamp can be retrieved from reportWrapper.responseData.calculationTimestamp
            //TODO for txr015, need to make sure if there are any format issues with timestamp then need to be fixed in txr015
            nonRepudiationService.buildNrsSubmission(reportWrapper.responseData.report.stringify, reportWrapper.responseData.report.reportId.toString, submissionTimestamp, request, AssistReportGenerated)
              .fold(
                error => Future.successful(InternalServerError(Json.toJson(DownstreamError))),
                success => {
                  logger.info(s"$correlationId::[submit] Request initiated to store ${AssistReportGenerated.value} content to NRS")
                  nonRepudiationService.submit(success)
                  logger.info(s"$correlationId::[generateReport] ... report submitted to NRS")
                  Future.successful(Ok(Json.toJson[AssessmentReport](reportWrapper.responseData.report)))
                }
              )
          }
        ).flatten.map(_.withApiHeaders(correlationId))
      }.getOrElse(Future(BadRequest(Json.toJson(CalculationIdFormatError)).withApiHeaders(correlationId)))
    }
  }

  def errorHandler(errorWrapper: ErrorWrapper,correlationId:String): Future[Result] = errorWrapper.error match {
    case ServerError | DownstreamError => Future(InternalServerError(Json.toJson(DownstreamError)))
    case NinoFormatError => Future(BadRequest(Json.toJson(NinoFormatError)))
    case CalculationIdFormatError => Future(BadRequest(Json.toJson(CalculationIdFormatError)))
    case MatchingResourcesNotFoundError => Future(NotFound(Json.toJson(MatchingResourcesNotFoundError)))
    case ClientOrAgentNotAuthorisedError => Future(Forbidden(Json.toJson(ClientOrAgentNotAuthorisedError)))
    case InvalidCredentialsError => Future(Unauthorized(Json.toJson(InvalidCredentialsError)))
    case RdsAuthError => Future(InternalServerError(Json.toJson(ForbiddenDownstreamError)))
    case ServiceUnavailableError => Future(InternalServerError(Json.toJson(ServiceUnavailableError)))
    case error@_ =>
      logger.error(s"$correlationId::[generateReportInternal] Error handled in general scenario $error")
      Future(BadRequest(Json.toJson(MatchingResourcesNotFoundError)))
  }


  private def generateFraudRiskRequest(request: AssessmentRequestForSelfAssessment,fraudRiskHeaders:FraudRiskRequest.FraudRiskHeaders): FraudRiskRequest = {
    val fraudRiskRequest =new FraudRiskRequest(
      nino= Some(request.nino),
      taxYear = Some(request.taxYear),
      fraudRiskHeaders=fraudRiskHeaders
    )
    fraudRiskRequest
  }

  private def toId(rawId: String): Option[UUID] =
    Try(UUID.fromString(rawId)).toOption

  private def getCalculationInfo(id: UUID, nino: String)(implicit correlationId:String): Future[ServiceOutcome[CalculationInfo]] = {
    for{
      calculationInfo <- integrationFrameworkService.getCalculationInfo(id, nino)
    } yield calculationInfo
  }

}
