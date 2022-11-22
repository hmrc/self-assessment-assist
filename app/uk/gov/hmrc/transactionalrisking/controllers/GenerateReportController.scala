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

package uk.gov.hmrc.transactionalrisking.controllers

import cats.data.EitherT
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.transactionalrisking.models.auth.AffinityGroupType
import uk.gov.hmrc.transactionalrisking.models.domain._
import uk.gov.hmrc.transactionalrisking.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.models.errors.{CalculationIdFormatError, ErrorWrapper, ForbiddenDownstreamError, MatchingResourcesNotFoundError, RdsAuthError}
import uk.gov.hmrc.transactionalrisking.services.cip.InsightService
import uk.gov.hmrc.transactionalrisking.services.eis.IntegrationFrameworkService
import uk.gov.hmrc.transactionalrisking.services.nrs.NrsService
import uk.gov.hmrc.transactionalrisking.services.nrs.models.request.{AssistReportGenerated, RequestBody, RequestData}
import uk.gov.hmrc.transactionalrisking.services.rds.RdsService
import uk.gov.hmrc.transactionalrisking.services.{EnrolmentsAuthService, ServiceOutcome}
import uk.gov.hmrc.transactionalrisking.utils.{CurrentDateTime, IdGenerator, Logging}

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

    implicit val correlationID: String = idGenerator.getUid
    logger.info(s"$correlationID::[generateReportInternal] Received request to generate an assessment report")

    authorisedAction(nino, correlationID, nrsRequired = true).async { implicit request =>
      val customerType = deriveCustomerType(request)
      val submissionTimestamp = currentDateTime.getDateTime

      toId(calculationId).map { calculationIDUuid =>

        val responseData: EitherT[Future, ErrorWrapper, ResponseWrapper[AssessmentReport]] = for {
          calculationInfo                     <- EitherT(getCalculationInfo(calculationIDUuid, nino))
          assessmentRequestForSelfAssessment  = AssessmentRequestForSelfAssessment(calculationIDUuid,
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
              assessmentReportResponse.reportID.toString))

            nonRepudiationService.submit(requestData = rdsReportContent,
              submissionTimestamp,
              notableEventType = AssistReportGenerated,
              assessmentReportResponse.taxYear)
            assessmentReportResponse
          }
        }

        responseData.fold(
          errorWrapper => errorHandler(errorWrapper, correlationID), report =>
            Future(Ok(Json.toJson[AssessmentReport](report.responseData)).withApiHeaders(correlationID))
        ).flatten

      }.getOrElse(Future(BadRequest(Json.toJson(CalculationIdFormatError)).withApiHeaders(correlationID)))
    }
  }

  def errorHandler(errorWrapper: ErrorWrapper,correlationId:String): Future[Result] = errorWrapper.error match {
    case MatchingResourcesNotFoundError => Future(NotFound(Json.toJson(MatchingResourcesNotFoundError)).withApiHeaders(correlationId))
    case RdsAuthError => Future(InternalServerError(Json.toJson(ForbiddenDownstreamError)).withApiHeaders(correlationId))
    case _ => Future(BadRequest(Json.toJson(MatchingResourcesNotFoundError)).withApiHeaders(correlationId))
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
