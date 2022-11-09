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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transactionalrisking.controllers.requestParsers.AcknowledgeRequestParser
import uk.gov.hmrc.transactionalrisking.models.domain.{AssessmentReport, DesTaxYear, Internal, Origin}
import uk.gov.hmrc.transactionalrisking.models.errors.{DownstreamError, ErrorWrapper, MatchingResourcesNotFoundError, ServiceUnavailableError}
import uk.gov.hmrc.transactionalrisking.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.models.request.AcknowledgeReportRawData
import uk.gov.hmrc.transactionalrisking.services.nrs.NrsService
import uk.gov.hmrc.transactionalrisking.services.nrs.models.request.{AcknowledgeReportRequest, AssistReportAcknowledged, RequestBody, RequestData}
import uk.gov.hmrc.transactionalrisking.services.rds.RdsService
import uk.gov.hmrc.transactionalrisking.services.rds.models.response.NewRdsAssessmentReport
import uk.gov.hmrc.transactionalrisking.services.{EnrolmentsAuthService, ParseOutcome, ServiceOutcome}
import uk.gov.hmrc.transactionalrisking.utils.{CurrentDateTime, IdGenerator, Logging}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AcknowledgeReportController @Inject()(
                                             val cc: ControllerComponents,
                                             requestParser: AcknowledgeRequestParser,
                                             val authService: EnrolmentsAuthService,
                                             nonRepudiationService: NrsService,
                                             rdsService: RdsService,
                                             currentDateTime: CurrentDateTime,
                                             idGenerator:IdGenerator
                                           )(implicit ec: ExecutionContext) extends AuthorisedController(cc) with BaseController with Logging {
  def acknowledgeReportForSelfAssessment(nino: String, reportId: String, rdsCorrelationId: String): Action[AnyContent] = {
    implicit val correlationID: String = idGenerator.getUid
    logger.info(s"$correlationID::[acknowledgeReportForSelfAssessment]Received request to acknowledge assessment report")

    val submissionTimestamp = currentDateTime.getDateTime
    val body = s"""{"reportID":"${reportId}"}"""
    val reportAcknowledgementContent = RequestData(nino, RequestBody(body, reportId))

    authorisedAction(nino, correlationID, nrsRequired = true).async { implicit request =>

      val processRequest: EitherT[Future, ErrorWrapper, NewRdsAssessmentReport] = for {
        parsedRequest   <- EitherT(requestParser.parseRequest(AcknowledgeReportRawData(nino, reportId, rdsCorrelationId)))
        serviceResponse <- EitherT(rdsService.acknowledge(parsedRequest))
      } yield {
        serviceResponse.responseData
      }

      val result = processRequest.fold(
        errorWrapper => errorHandler(errorWrapper, correlationID),
        assessmentReport => {
          assessmentReport.responseCode match {
            case code@Some(ACCEPTED) =>
              assessmentReport.taxYear match {
                case Some(taxYear) =>
                  val taxYearFromResponse: String = DesTaxYear.fromDesIntToString(taxYear)
                  logger.debug(s"$correlationID::[acknowledgeReport] ... submitting acknowledgement to NRS")
                  //Submit asynchronously to NRS
                  nonRepudiationService.submit(reportAcknowledgementContent, submissionTimestamp, AssistReportAcknowledged, taxYearFromResponse)

                  logger.info(s"$correlationID::[acknowledgeReport] ... report submitted to NRS")
                  Future(NoContent.withApiHeaders(correlationID))
                case None =>
                  logger.error(s"$correlationID::[acknowledgeReport]Unable to get tax year")
                  Future(ServiceUnavailable(Json.toJson(DownstreamError)).withApiHeaders(correlationID))
              }
            case code@Some(NO_CONTENT) =>
              logger.warn(s"$correlationID::[acknowledgeReport] Place Holder: rds ack response is ${code}")
              Future(NoContent.withApiHeaders(correlationID))
            case code@Some(BAD_REQUEST) =>
              logger.warn(s"$correlationID::[acknowledgeReport] rds ack response is ${code}")
              Future(ServiceUnavailable(Json.toJson(DownstreamError)).withApiHeaders(correlationID))
            case None =>
              logger.error(s"$correlationID::[acknowledgeReport] rds ack response code is empty")
              Future(ServiceUnavailable(Json.toJson(DownstreamError)).withApiHeaders(correlationID))
          }
        })
      result.flatten
    }
  }

  def errorHandler(errorWrapper: ErrorWrapper,correlationId:String): Future[Result] = errorWrapper.error match {
    case MatchingResourcesNotFoundError => Future(NotFound(Json.toJson(MatchingResourcesNotFoundError)).withApiHeaders(correlationId))
    case _ => Future(ServiceUnavailable(Json.toJson(DownstreamError)).withApiHeaders(correlationId))
  }
}
