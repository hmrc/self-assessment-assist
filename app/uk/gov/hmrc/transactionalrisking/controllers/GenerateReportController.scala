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
import uk.gov.hmrc.transactionalrisking.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.models.errors.{CalculationIdFormatError, MatchingResourcesNotFoundError}
import uk.gov.hmrc.transactionalrisking.services.cip.InsightService
import uk.gov.hmrc.transactionalrisking.services.eis.IntegrationFrameworkService
import uk.gov.hmrc.transactionalrisking.services.nrs.NrsService
import uk.gov.hmrc.transactionalrisking.services.nrs.models.request.{AssistReportGenerated, RequestBody, RequestData}
import uk.gov.hmrc.transactionalrisking.services.rds.RdsService
import uk.gov.hmrc.transactionalrisking.services.{EnrolmentsAuthService, ServiceOutcome}
import uk.gov.hmrc.transactionalrisking.utils.{CurrentDateTime, Logging, ProvideRandomCorrelationId}

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class GenerateReportController @Inject()(
                                          val cc: ControllerComponents,//TODO add request parser
                                          val integrationFrameworkService: IntegrationFrameworkService,
                                          val authService: EnrolmentsAuthService,
                                          nonRepudiationService: NrsService,
                                          insightService: InsightService,
                                          rdsService: RdsService,
                                          currentDateTime: CurrentDateTime,
                                          provideRandomCorrelationId: ProvideRandomCorrelationId
                                        )(implicit ec: ExecutionContext) extends AuthorisedController(cc) with BaseController with Logging {

  def generateReportInternal(nino: String, calculationID: String): Action[AnyContent] =
    authorisedAction(nino, nrsRequired = true).async { implicit request =>
      implicit val correlationId: String = provideRandomCorrelationId.getRandomCorrelationId()
      val customerType = deriveCustomerType(request)
      toId(calculationID).map { calculationIDUuid =>
        val calculationInfo = getCalculationInfo(calculationIDUuid, nino)
        val assessmentRequestForSelfAssessment = new AssessmentRequestForSelfAssessment(calculationIDUuid,
          nino,
          PreferredLanguage.English,
          customerType,
          None,
          DesTaxYear.fromMtd(calculationInfo.taxYear).toString)

        val fraudRiskReport: FraudRiskReport = insightService.assess(generateFraudRiskRequest(assessmentRequestForSelfAssessment))
        logger.info(s"$correlationId :: Received response for fraudRiskReport")
        val rdsAssessmentReportResponse: Future[ServiceOutcome[AssessmentReport]] =
          rdsService.submit(assessmentRequestForSelfAssessment, fraudRiskReport, Internal)
        logger.info(s"Received RDS assessment response")

        Future {
          def assementReportFuture: Future[ServiceOutcome[AssessmentReport]] = rdsAssessmentReportResponse.map {
            assementReportserviceOutcome =>
              assementReportserviceOutcome match {
                case Right(ResponseWrapper(correlationIdRes,assessmentReportResponse)) =>
                  val rdsReportContent = RequestData(nino = nino,
                    RequestBody(assessmentReportResponse.toString, assessmentReportResponse.reportID.toString))
                  logger.debug(s"RDS request content $rdsReportContent")
                  nonRepudiationService.submit(requestData = rdsReportContent,
                    submissionTimestamp = currentDateTime.getDateTime,
                    notableEventType = AssistReportGenerated,calculationInfo.taxYear)
                  //TODO:DE Need   to deal with post NRS errors here.
                  Right(ResponseWrapper(correlationId,assessmentReportResponse)): ServiceOutcome[AssessmentReport]
                case Left(errorWrapper) =>
                  Left(errorWrapper): ServiceOutcome[AssessmentReport]
              }
          }

          def ret: Future[Result] = assementReportFuture.flatMap {
            serviceOutcome =>
              serviceOutcome match {
                case Right(ResponseWrapper(correlationId,assessmentReport)) =>
                  serviceOutcome.right.get.flatMap {
                    assessmentReport =>
                      val jsValue = Json.toJson[AssessmentReport](assessmentReport)
                      Future(Ok(jsValue).withApiHeaders(correlationId))

                  }
                case Left(errorWrapper) =>
                  errorWrapper.error match {
                    case MatchingResourcesNotFoundError => Future(NotFound(Json.toJson(MatchingResourcesNotFoundError)).withApiHeaders(correlationId))
                    case _ => Future(BadRequest(Json.toJson(MatchingResourcesNotFoundError)).withApiHeaders(correlationId))
                  }
              }
          }

          ret

        }.flatten
      }.getOrElse(Future(BadRequest(Json.toJson(CalculationIdFormatError)).withApiHeaders(correlationId))) //TODO Add RequestParser
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


  private def getCalculationInfo(id: UUID, nino: String): CalculationInfo =
    integrationFrameworkService.getCalculationInfo(id, nino)
      .getOrElse(throw new RuntimeException(s"Unknown calculation for id [$id] and nino [$nino]"))

}
