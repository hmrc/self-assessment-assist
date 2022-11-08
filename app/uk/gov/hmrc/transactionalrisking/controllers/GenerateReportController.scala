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

import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.transactionalrisking.models.auth.AffinityGroupType
import uk.gov.hmrc.transactionalrisking.models.domain._
import uk.gov.hmrc.transactionalrisking.models.errors.{CalculationIdFormatError, MatchingResourcesNotFoundError}
import uk.gov.hmrc.transactionalrisking.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.models.errors.{CalculationIdFormatError, MatchingResourcesNotFoundError}
import uk.gov.hmrc.transactionalrisking.services.cip.InsightService
import uk.gov.hmrc.transactionalrisking.services.eis.IntegrationFrameworkService
import uk.gov.hmrc.transactionalrisking.services.nrs.NrsService
import uk.gov.hmrc.transactionalrisking.services.nrs.models.request.{AssistReportGenerated, RequestBody, RequestData}
import uk.gov.hmrc.transactionalrisking.services.rds.RdsService
import uk.gov.hmrc.transactionalrisking.services.{EnrolmentsAuthService, ServiceOutcome}
import uk.gov.hmrc.transactionalrisking.utils.{CurrentDateTime, Logging, IdGenerator}

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import uk.gov.hmrc.transactionalrisking.utils.Logging

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

    implicit val correlationID: String =idGenerator.getUid
    logger.info(s"$correlationID::[generateReportInternal] Received request to generate an assessment report")

    authorisedAction(nino, correlationID, nrsRequired = true).async { implicit request =>

      val customerType = deriveCustomerType(request)
      toId(calculationId).map { calculationIDUuid =>
        val calculationInfo = getCalculationInfo(calculationIDUuid, nino, correlationID)

        val ret: Future[Result] = calculationInfo match {
          case Left(errorWrapper) =>
            logger.warn(s"$correlationID::[generateReportInternal]Received request to generate an assessment report")
            Future(BadRequest(Json.toJson(errorWrapper.error)).withApiHeaders(correlationID))

          case Right(ResponseWrapper(correlationIdRes, assessmentReportResponse)) => {

            val assessmentRequestForSelfAssessment = new AssessmentRequestForSelfAssessment(calculationIDUuid,
              nino,
              PreferredLanguage.English,
              customerType,
              None,
              DesTaxYear.fromMtd(assessmentReportResponse.taxYear).toString)

            val fraudRiskReport: FraudRiskReport = insightService.assess(generateFraudRiskRequest(assessmentRequestForSelfAssessment))
            logger.debug(s"$correlationID::[generateReportInternal]Received response for fraudRiskReport")

            val rdsAssessmentReportResponse: Future[ServiceOutcome[AssessmentReport]] =
              rdsService.submit(assessmentRequestForSelfAssessment, fraudRiskReport, Internal)
            logger.debug(s"$correlationID::[generateReportInternal]Received RDS assessment response")

            val ret = Future {
              def assessmentReportFuture: Future[ServiceOutcome[AssessmentReport]] = rdsAssessmentReportResponse.map {
                assessmentReportServiceOutcome =>
                  assessmentReportServiceOutcome match {
                    case Right(ResponseWrapper(correlationIdRes, assessmentReportResponse)) =>
                      val rdsReportContent = RequestData(nino = nino,
                        RequestBody(assessmentReportResponse.toString, assessmentReportResponse.reportID.toString))
                      logger.debug(s"$correlationID::[generateReportInternal]RDS request content")

                      nonRepudiationService.submit(requestData = rdsReportContent,
                        submissionTimestamp = currentDateTime.getDateTime,
                        notableEventType = AssistReportGenerated, assessmentReportResponse.taxYear)
                      logger.info(s"$correlationID::[generateReportInternal]NRS report sent")
                      //TODO:DE Need   to deal with post NRS errors here.

                      Right(ResponseWrapper(correlationID, assessmentReportResponse))
                    case Left(errorWrapper) =>
                      logger.warn(s"$correlationID::[generateReportInternal]Error submitting report $errorWrapper")
                      Left(errorWrapper)
                  }
              }

              def ret: Future[Result] = assessmentReportFuture.flatMap {
                serviceOutcome =>
                  serviceOutcome match {
                    case Right(ResponseWrapper(correlationId, assessmentReport)) =>
                      serviceOutcome.right.get.flatMap {
                        assessmentReport =>
                          val jsValue = Json.toJson[AssessmentReport](assessmentReport)
                          logger.info(s"$correlationID::[generateReportInternal]Sending back correct response")
                          Future(Ok(jsValue).withApiHeaders(correlationId))

                      }
                    case Left(errorWrapper) =>
                      errorWrapper.error match {
                        case MatchingResourcesNotFoundError =>
                          logger.warn(s"$correlationID::[generateReportInternal]Matching resource not found")
                          Future(NotFound(Json.toJson(MatchingResourcesNotFoundError)).withApiHeaders(correlationID))
                        case _ =>
                          logger.error(s"$correlationID::[generateReportInternal]Unable to get report")
                          Future(BadRequest(Json.toJson(MatchingResourcesNotFoundError)).withApiHeaders(correlationID))
                      }
                  }
              }

              ret

            }.flatten
            ret
          }
        }
        ret
      }.getOrElse({
        logger.warn(s"$correlationID::[generateReportInternal]CalculationID format error")
        Future(BadRequest(Json.toJson(CalculationIdFormatError)).withApiHeaders(correlationID))
      }) //TODO Add RequestParser
    }
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

  //private def asError(message: String): JsObject = Json.obj("message" -> message)

  private def getCalculationInfo(id: UUID, nino: String, correlationID: String): ServiceOutcome[CalculationInfo] = {
    integrationFrameworkService.getCalculationInfo(id, nino, correlationID)
      .map { x =>
        x match {
          case right@(ResponseWrapper(correlationID, calcInfo)) =>
            right
          case errorWrapper =>
            errorWrapper
        }
      }
  }

}
