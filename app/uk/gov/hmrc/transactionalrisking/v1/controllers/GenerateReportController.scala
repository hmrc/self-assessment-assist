/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.transactionalrisking.v1.models.auth.AffinityGroupType
import uk.gov.hmrc.transactionalrisking.v1.models.domain._
import uk.gov.hmrc.transactionalrisking.v1.models.errors._
import uk.gov.hmrc.transactionalrisking.v1.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.v1.services.cip.InsightService
import uk.gov.hmrc.transactionalrisking.v1.services.eis.IntegrationFrameworkService
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.NrsService
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.request.{AssistReportGenerated, RequestBody, RequestData}
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
      val customerType = deriveCustomerType(request)
      val submissionTimestamp = currentDateTime.getDateTime()

      toId(calculationId).map { calculationIdUuid =>

        val responseData: EitherT[Future, ErrorWrapper, ResponseWrapper[AssessmentReport]] = for {
          calculationInfo                     <- EitherT(getCalculationInfo(calculationIdUuid, nino))
          assessmentRequestForSelfAssessment  = AssessmentRequestForSelfAssessment(calculationIdUuid,
                                                nino,
                                                PreferredLanguage.English,
                                                customerType,
                                                None,
                                                DesTaxYear.fromMtd(calculationInfo.responseData.taxYear).toString)

          fraudRiskReport                     <- EitherT(insightService.assess(generateFraudRiskRequest(assessmentRequestForSelfAssessment)))
          rdsAssessmentReportResponse         <- EitherT(rdsService.submit(assessmentRequestForSelfAssessment, fraudRiskReport.responseData, Internal))
        } yield {
          rdsAssessmentReportResponse.map { assessmentReportResponse: AssessmentReport =>
            val rdsReportContent = RequestData(nino = nino, RequestBody(assessmentReportResponse.toString,
              assessmentReportResponse.reportId.toString))

            nonRepudiationService.submit(
              requestData = rdsReportContent,
              submissionTimestamp,
              notableEventType = AssistReportGenerated
            )

            assessmentReportResponse
          }
        }

        responseData.fold(
          errorWrapper => errorHandler(errorWrapper, correlationId), report =>
            Future(Ok(Json.toJson[AssessmentReport](report.responseData)).withApiHeaders(correlationId))
        ).flatten

      }.getOrElse(Future(BadRequest(Json.toJson(CalculationIdFormatError)).withApiHeaders(correlationId)))
    }
  }

  def errorHandler(errorWrapper: ErrorWrapper,correlationId:String): Future[Result] = errorWrapper.error match {
    case ServerError => Future(InternalServerError(Json.toJson(DownstreamError)).withApiHeaders(correlationId))
    case ServiceUnavailableError => Future(InternalServerError(Json.toJson(DownstreamError)).withApiHeaders(correlationId))
    case NinoFormatError => Future(BadRequest(Json.toJson(NinoFormatError)).withApiHeaders(correlationId))
    case CalculationIdFormatError => Future(BadRequest(Json.toJson(CalculationIdFormatError)).withApiHeaders(correlationId))
    case MatchingResourcesNotFoundError => Future(NotFound(Json.toJson(MatchingResourcesNotFoundError)).withApiHeaders(correlationId))      // RDS 3 (201 CREATED 404 NOT_FOUND) =>404 NOT_FOUND (MatchingResourcesNotFoundError)
    case MatchingResourcesNotFoundError => Future(ServiceUnavailable(Json.toJson(ServiceUnavailableError)).withApiHeaders(correlationId))   // RDS 5 (404 NOT_FOUND)) =>503(ServiceUnavailableError)(ServiceUnavailableError)

   // case  => Future(ServiceUnavailable(Json.toJson(DownstreamError)).withApiHeaders(correlationId))                                       // RDS 6 ??? => 500 INTERNAL_SERVER_ERROR (INTERNAL_SERVER_ERROR)

    // case  => Future(ServiceUnavailable(Json.toJson(DownstreamError)).withApiHeaders(correlationId))                                       // RDS 7 408 => 500 INTERNAL_SERVER_ERROR (INTERNAL_SERVER_ERROR)

    // case  => Future(ServiceUnavailable(Json.toJson(ServiceUnavailable)).withApiHeaders(correlationId))                                       // RDS 8 ?  => 503 SERVICE_UNAVAILABLE (ServiceUnavailableError)

    case ClientOrAgentNotAuthorisedError => Future(Forbidden(Json.toJson(ClientOrAgentNotAuthorisedError)).withApiHeaders(correlationId))
    case InvalidCredentialsError => Future(Unauthorized(Json.toJson(InvalidCredentialsError)).withApiHeaders(correlationId))
    case RdsAuthError => Future(InternalServerError(Json.toJson(ForbiddenDownstreamError)).withApiHeaders(correlationId))
    case DownstreamError => Future(InternalServerError(Json.toJson(DownstreamError)).withApiHeaders(correlationId))                         // RDS 4 (400 ) =>500(INTERNAL_SERVER_ERROR)(MatchingResourcesNotFoundError)
    case ServiceUnavailableError => Future(InternalServerError(Json.toJson(ServiceUnavailableError)).withApiHeaders(correlationId))
    case error@_ =>
      logger.error(s"$correlationId::[generateReportInternal] Error handled in general scenario $error")
      Future(BadRequest(Json.toJson(MatchingResourcesNotFoundError)).withApiHeaders(correlationId))

  }


  private def deriveCustomerType(request: Request[AnyContent]) = {
    request.asInstanceOf[UserRequest[_]].userDetails.userType match {
      case AffinityGroupType.individual => CustomerType.TaxPayer
      case AffinityGroupType.organisation => CustomerType.Agent
      case AffinityGroupType.agent => CustomerType.Agent
    }
  }

  //TODO Revisit Check headers as pending
  private def generateFraudRiskRequest(request: AssessmentRequestForSelfAssessment): FraudRiskRequest = {
    val fraudRiskHeaders = Map.empty[String, String]
    new FraudRiskRequest(
      request.nino,
      request.taxYear,
      fraudRiskHeaders
    )
  }

  private def toId(rawId: String): Option[UUID] =
    Try(UUID.fromString(rawId)).toOption

  private def getCalculationInfo(id: UUID, nino: String)(implicit correlationId:String): Future[ServiceOutcome[CalculationInfo]] = {
    for{
      calculationInfo <- integrationFrameworkService.getCalculationInfo(id, nino)
    } yield calculationInfo
  }

}
