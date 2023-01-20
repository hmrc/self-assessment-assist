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
import uk.gov.hmrc.transactionalrisking.v1.controllers.requestParsers.AcknowledgeRequestParser
import uk.gov.hmrc.transactionalrisking.v1.models.errors._
import uk.gov.hmrc.transactionalrisking.v1.models.request.AcknowledgeReportRawData
import uk.gov.hmrc.transactionalrisking.v1.services.EnrolmentsAuthService
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.NrsService
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.request.{AcknowledgeReportId, AssistReportAcknowledged}
import uk.gov.hmrc.transactionalrisking.v1.services.rds.RdsService
import uk.gov.hmrc.transactionalrisking.v1.services.rds.models.response.RdsAssessmentReport

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AcknowledgeReportController @Inject()(
                                             val cc: ControllerComponents,
                                             requestParser: AcknowledgeRequestParser,
                                             val authService: EnrolmentsAuthService,
                                             nonRepudiationService: NrsService,
                                             rdsService: RdsService,
                                             currentDateTime: CurrentDateTime,
                                             idGenerator: IdGenerator
                                           )(implicit ec: ExecutionContext) extends AuthorisedController(cc) with BaseController with Logging {
  def acknowledgeReportForSelfAssessment(nino: String, reportId: String, rdsCorrelationId: String): Action[AnyContent] = {
    implicit val correlationId: String = idGenerator.getUid
    logger.info(s"$correlationId::[acknowledgeReportForSelfAssessment]Received request to acknowledge assessment report")

    val submissionTimestamp = currentDateTime.getDateTime()

    authorisedAction(nino, nrsRequired = true).async {
      implicit request =>

        val processRequest: EitherT[Future, ErrorWrapper, RdsAssessmentReport] = for {
          parsedRequest <- EitherT(requestParser.parseRequest(AcknowledgeReportRawData(nino, reportId, rdsCorrelationId)))
          serviceResponse <- EitherT(rdsService.acknowledge(parsedRequest))
        } yield serviceResponse.responseData

        processRequest.fold(
          errorWrapper => errorHandler(errorWrapper, correlationId),
          assessmentReport => {
            logger.debug(s"$correlationId::[acknowledgeReport] ... RDS acknowledge status ${assessmentReport.responseCode}")
            assessmentReport.responseCode match {
              case Some(CREATED) | Some(ACCEPTED)=>
                nonRepudiationService.buildNrsSubmission(AcknowledgeReportId(reportId).stringify, reportId,
                  submissionTimestamp, request, AssistReportAcknowledged).
                  fold(
                    error => {
                      logger.error(s"$correlationId::[acknowledgeReport] NRS event generation failed")
                      Future.successful(InternalServerError(Json.toJson(DownstreamError)))
                    },
                    success => {
                      logger.debug(s"$correlationId::[submit] Request initiated to store ${AssistReportAcknowledged.value} content to NRS")
                      //Submit asynchronously to NRS
                      nonRepudiationService.submit(success)
                      logger.info(s"$correlationId::[generateReport] ... report submitted to NRS")
                      Future.successful(NoContent)
                    }
                  )
              case Some(NO_CONTENT) => Future.successful(NoContent)
              case Some(BAD_REQUEST) => Future.successful(ServiceUnavailable(Json.toJson(DownstreamError)))
              case Some(UNAUTHORIZED) =>
                logger.warn(s"$correlationId::[acknowledgeReport] rds ack response is $UNAUTHORIZED ${assessmentReport.responseMessage}")
                Future.successful(ServiceUnavailable(Json.toJson(DownstreamError)))
              case _ =>
                logger.error(s"$correlationId::[acknowledgeReport] unrecognised value for response code")
                Future.successful(ServiceUnavailable(Json.toJson(DownstreamError)))
            }
          }
        ).flatten.map(_.withApiHeaders(correlationId))

    }
  }

  def errorHandler(errorWrapper: ErrorWrapper, correlationId: String): Future[Result] = errorWrapper.error match {
    case ServerError | DownstreamError |ServiceUnavailableError |ForbiddenDownstreamError => Future.successful(InternalServerError(Json.toJson(DownstreamError)))
    case FormatReportIdError => Future.successful(BadRequest(Json.toJson(FormatReportIdError)))
    case ClientOrAgentNotAuthorisedError => Future.successful(Forbidden(Json.toJson(ClientOrAgentNotAuthorisedError)))
    case NinoFormatError => Future.successful(BadRequest(Json.toJson(NinoFormatError)))
    case MatchingResourcesNotFoundError | ResourceNotFoundError => Future.successful(ServiceUnavailable(Json.toJson(ServiceUnavailableError)))
    case error@_ =>
      logger.error(s"$correlationId::[AcknowledgeReportController] Error handled in general scenario $error")
      Future.successful(ServiceUnavailable(Json.toJson(DownstreamError)))
  }
}
