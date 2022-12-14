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
import uk.gov.hmrc.transactionalrisking.v1.controllers.requestParsers.AcknowledgeRequestParser
import uk.gov.hmrc.transactionalrisking.v1.models.errors._
import uk.gov.hmrc.transactionalrisking.v1.models.request.AcknowledgeReportRawData
import uk.gov.hmrc.transactionalrisking.v1.services.EnrolmentsAuthService
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.NrsService
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.request.{AssistReportAcknowledged, RequestBody, RequestData}
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

    val submissionTimestamp = currentDateTime.getDateTime
    val body = s"""{"reportId":"${reportId}"}"""
    val reportAcknowledgementContent = RequestData(nino, RequestBody(body, reportId))

    authorisedAction(nino, nrsRequired = true).async {
      implicit request =>

        val processRequest: EitherT[Future, ErrorWrapper, RdsAssessmentReport] = for {
          parsedRequest <- EitherT(requestParser.parseRequest(AcknowledgeReportRawData(nino, reportId, rdsCorrelationId)))
          serviceResponse <- EitherT(rdsService.acknowledge(parsedRequest))
        } yield {
          serviceResponse.responseData
        }

        val result = processRequest.fold(
          errorWrapper => errorHandler(errorWrapper, correlationId),
          assessmentReport => {
            assessmentReport.responseCode match {
              case code@Some(ACCEPTED) =>
                logger.debug(s"$correlationId::[acknowledgeReport] ... submitting acknowledgement to NRS")
                //Submit asynchronously to NRS
                nonRepudiationService.submit(reportAcknowledgementContent, submissionTimestamp, AssistReportAcknowledged)
                logger.info(s"$correlationId::[acknowledgeReport] ... report submitted to NRS")
                Future(NoContent.withApiHeaders(correlationId))

              case code@Some(NO_CONTENT) =>
                logger.warn(s"$correlationId::[acknowledgeReport] Place Holder: rds ack response is ${code}")
                Future(NoContent.withApiHeaders(correlationId))
              case code@Some(BAD_REQUEST) =>
                logger.warn(s"$correlationId::[acknowledgeReport] rds ack response is ${code}")
                Future(ServiceUnavailable(Json.toJson(DownstreamError)).withApiHeaders(correlationId))
              case None =>
                logger.error(s"$correlationId::[acknowledgeReport] rds ack response code is empty")
                Future(ServiceUnavailable(Json.toJson(DownstreamError)).withApiHeaders(correlationId))
            }
          })
        result.flatten
    }
  }

  def errorHandler(errorWrapper: ErrorWrapper, correlationId: String): Future[Result] = errorWrapper.error match {
    case ServerError => Future(InternalServerError(Json.toJson(DownstreamError)).withApiHeaders(correlationId))
    case ServiceUnavailableError => Future(InternalServerError(Json.toJson(DownstreamError)).withApiHeaders(correlationId))
    case FormatReportIdError => Future(BadRequest(Json.toJson(FormatReportIdError)).withApiHeaders(correlationId))
    case MatchingResourcesNotFoundError => Future(NotFound(Json.toJson(MatchingResourcesNotFoundError)).withApiHeaders(correlationId))
    case ResourceNotFoundError => Future(NotFound(Json.toJson(MatchingResourcesNotFoundError)).withApiHeaders(correlationId))
    case ClientOrAgentNotAuthorisedError => Future(Forbidden(Json.toJson(ClientOrAgentNotAuthorisedError)).withApiHeaders(correlationId)) //RDS 10 201 CREATED Rejected => 403 FOBBIDEN ( ClientOrAgentNotAuthorisedError)
    //    case InvalidCredentialsError => Future(Unauthorized(Json.toJson(InvalidCredentialsError)).withApiHeaders(correlationId))
    case NinoFormatError => Future(BadRequest(Json.toJson(NinoFormatError)).withApiHeaders(correlationId))
    case DownstreamError => Future(InternalServerError(Json.toJson(DownstreamError)).withApiHeaders(correlationId)) // RDS 11 (400 ) =>500(INTERNAL_SERVER_ERROR)(MatchingResourcesNotFoundError)
    case MatchingResourcesNotFoundError => Future(ServiceUnavailable(Json.toJson(ServiceUnavailableError)).withApiHeaders(correlationId)) // RDS 12 (404 NOT_FOUND)) =>503(ServiceUnavailableError)(ServiceUnavailableError)

    // case  => Future(ServiceUnavailable(Json.toJson(DownstreamError)).withApiHeaders(correlationId))                                       // RDS 13 ??? => 500 INTERNAL_SERVER_ERROR (INTERNAL_SERVER_ERROR)

    // case  => Future(ServiceUnavailable(Json.toJson(DownstreamError)).withApiHeaders(correlationId))                                       // RDS 14 408 => 500 INTERNAL_SERVER_ERROR (INTERNAL_SERVER_ERROR)

    // case  => Future(ServiceUnavailable(Json.toJson(ServiceUnavailable)).withApiHeaders(correlationId))                                       // RDS 15 ?  => 503 SERVICE_UNAVAILABLE (ServiceUnavailableError)

    case _ => Future(ServiceUnavailable(Json.toJson(DownstreamError)).withApiHeaders(correlationId))
  }
}
