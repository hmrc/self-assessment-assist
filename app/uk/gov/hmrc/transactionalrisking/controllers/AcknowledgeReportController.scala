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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transactionalrisking.controllers.requestParsers.AcknowledgeRequestParser
import uk.gov.hmrc.transactionalrisking.models.domain.{DesTaxYear, Internal, Origin}
import uk.gov.hmrc.transactionalrisking.models.errors.{DownstreamError, ErrorWrapper, ServiceUnavailableError}
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
  //TODO revisit if reportId needs to be UUID instead of string? as regex validation is done anyway
  def acknowledgeReportForSelfAssessment(nino: String, reportId: String, rdsCorrelationId: String): Action[AnyContent] = {
    implicit val correlationID: String = idGenerator.getUid
    logger.info(s"$correlationID::[acknowledgeReportForSelfAssessment]Received request to acknowledge assessment report")

    authorisedAction(nino, correlationID, nrsRequired = true).async { implicit request =>

      val parsedRequest: ParseOutcome[AcknowledgeReportRequest] = requestParser.parseRequest(AcknowledgeReportRawData(nino, reportId, rdsCorrelationId))

      def response: Future[Result] =
        parsedRequest match {
          case Right(ResponseWrapper(responseCorrelationId, acknowledgeReportRequest: AcknowledgeReportRequest)) =>

            def acknowledge: Future[Result] = {
              val retAcknowledgeReport = acknowledgeReport(acknowledgeReportRequest, Internal).map {
                acknowledgeReport: ServiceOutcome[Int] =>
                  acknowledgeReport match {
                    case Right(ResponseWrapper(responseCorrelationId, returnCode)) => {
                      val retFuture: Future[Result] = returnCode match {
                        case OK =>
                          logger.info(s"$correlationID::[acknowledgeReportForSelfAssessment]Received acknowledgment of acknowledgement report has been recorded")
                          val retFutureOk: Future[Result] = Future(NoContent.withApiHeaders(correlationID))
                          retFutureOk
                        case NO_CONTENT =>
                          logger.warn(s"$correlationID::[acknowledgeReportForSelfAssessment](PlaceHolder)Received error code $NO_CONTENT")
                          val retFutureOk: Future[Result] = Future(NoContent.withApiHeaders(correlationID))
                          retFutureOk
                        // case x => other error codes from the response code.
                        case _ =>
                          logger.warn(s"$correlationID::[acknowledgeReportForSelfAssessment]Received error code $returnCode")
                          val retError: Future[Result] = Future(BadRequest(Json.toJson(DownstreamError)).withApiHeaders(correlationID))
                          retError
                      }
                      retFuture
                    }
                    case Left(errorWrapper) =>
                      logger.warn(s"$correlationID::[acknowledgeReportForSelfAssessment]Received error $errorWrapper.error")
                      val ret: Future[Result] = Future(BadRequest(Json.toJson(errorWrapper.error)).withApiHeaders(correlationID))
                      ret
                  }
              }
              retAcknowledgeReport
            }.flatten

            acknowledge

          case Left(errorWrapper) =>
            logger.warn(s"$correlationID::[acknowledgeReportForSelfAssessment]Received parsed error $errorWrapper.error")
            Future(BadRequest(Json.toJson(errorWrapper.error)).withApiHeaders(correlationID))
          case _ =>
            logger.error(s"$correlationID::[acknowledgeReportForSelfAssessment]Received unknown parsed error")
            Future(BadRequest(Json.toJson(ServiceUnavailableError)).withApiHeaders(correlationID))
        }

      response
    }
  }


  private def acknowledgeReport(request: AcknowledgeReportRequest, origin: Origin)(implicit hc: HeaderCarrier,
                                                                                   ec: ExecutionContext,
                                                                                   //  logContext: EndpointLogContext,
                                                                                   userRequest: UserRequest[_],
                                                                                   correlationID: String): Future[ServiceOutcome[Int]] = {
    logger.info(s"$correlationID::[acknowledgeReport] Received request to acknowledge assessment report for Self Assessment [${request.feedbackID}]")
    //    doImplicitAuditing()
    //    auditRequestToAcknowledge(request)

    Future {
      val ret: Future[ServiceOutcome[Int]] = rdsService.acknowledge(request).flatMap {
        acknowledgeReportSO: ServiceOutcome[NewRdsAssessmentReport] =>
          val ret: Future[ServiceOutcome[Int]] = acknowledgeReportSO match {
            case Right(ResponseWrapper(correlationIdResponse, acknowledgeReport)) => {
              val responseCode:Int = acknowledgeReport.responseCode
                .getOrElse(BAD_REQUEST)
              val ret: Future[ServiceOutcome[Int]] = responseCode match {
                //TODO This status code doesn't look right, need to check the response code from RDS it might be 202 (ACCEPTED)
                case ACCEPTED => {
                  logger.debug(s"$correlationID::[acknowledgeReport] rds ack response is ${responseCode}}")


                  //TODO submissionTimestamp should this be current time?
                  val submissionTimestamp = currentDateTime.getDateTime
                  val body = s"""{"reportID":"${request.feedbackID}"}"""

                  val ret = acknowledgeReport.taxYear match {
                    case Some(taxYear) =>
                      val taxYearFromResponse: String = DesTaxYear.fromDesIntToString(taxYear)
                      val reportAcknowledgementContent = RequestData(request.nino, RequestBody(body, reportId = request.feedbackID))

                      logger.debug(s"$correlationID::[acknowledgeReport] ... submitting acknowledgement to NRS")
                      //Submit asynchronously to NRS
                      nonRepudiationService.submit(reportAcknowledgementContent, submissionTimestamp, AssistReportAcknowledged, taxYearFromResponse)
                      //TODO confirm documentation if nrs failure needs to handled/audited?

                      logger.info(s"$correlationID::[acknowledgeReport] ... report submitted to NRS")
                      Future(Right(ResponseWrapper(correlationID, NO_CONTENT)))
                    case None =>
                      logger.warn(s"$correlationID::[acknowledgeReport]Unable to get tax year")
                      Future(Left(ErrorWrapper(correlationID, DownstreamError)): ServiceOutcome[Int])
                  }
                  ret
                }
                //TODO : Place holder for any errors via response when we get them.
                case NO_CONTENT => {
                  logger.warn(s"$correlationID::[acknowledgeReport] Place Holder: rds ack response is ${responseCode}")
                  Future(Right(ResponseWrapper(correlationID, NO_CONTENT)))
                }
                case BAD_REQUEST => {
                  logger.warn(s"$correlationID::[acknowledgeReport] rds ack response is ${responseCode}")
                  Future(Left(ErrorWrapper(correlationID, DownstreamError)): ServiceOutcome[Int])
                }
                // case other errors from system.
                case _ =>
                  logger.warn(s"$correlationID::[acknowledgeReport] rds ack response is unknown ${responseCode} and not been handled so is rejected: ")
                  Future(Left(ErrorWrapper(correlationID, DownstreamError)): ServiceOutcome[Int])
              }
              ret
            }
            case Left(errorWrapper) =>
              logger.warn(s"$correlationID::[acknowledgeReport] Received unknown error")
              Future(Left( errorWrapper): ServiceOutcome[Int])
          }
          ret
      }
      ret
    }.flatten

  }
}
