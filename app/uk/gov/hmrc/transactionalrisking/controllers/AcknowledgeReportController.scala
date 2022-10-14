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
import uk.gov.hmrc.transactionalrisking.services.{EnrolmentsAuthService, ServiceOutcome}
import uk.gov.hmrc.transactionalrisking.utils.{CurrentDateTime, Logging, ProvideRandomCorrelationId}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AcknowledgeReportController @Inject()(
                                             val cc: ControllerComponents,
                                             requestParser: AcknowledgeRequestParser,
                                             val authService: EnrolmentsAuthService,
                                             nonRepudiationService: NrsService,
                                             rdsService: RdsService,
                                             currentDateTime: CurrentDateTime,
                                             provideRandomCorrelationId:ProvideRandomCorrelationId
                                           )(implicit ec: ExecutionContext) extends AuthorisedController(cc) with BaseController with Logging {
  //TODO revisit if reportId needs to be UUID instead of string? as regex validation is done anyway
  def acknowledgeReportForSelfAssessment(nino: String, reportId: String, rdsCorrelationId: String): Action[AnyContent] =
    authorisedAction(nino, nrsRequired = true).async { implicit request =>
      implicit val correlationId: String = provideRandomCorrelationId.getRandomCorrelationId()
      logger.info(s"Received request to acknowledge assessment report")

      val parsedRequest: ServiceOutcome[AcknowledgeReportRequest] = requestParser.parseRequest(AcknowledgeReportRawData(nino, reportId, rdsCorrelationId))

      def response: Future[Result] =
        parsedRequest match {
          case Right( ResponseWrapper( responseCorrelationId, acknowledgeReportRequest: AcknowledgeReportRequest)) =>

            def acknowledge: Future[Result] = {
              val retAcknowledgeReport = acknowledgeReport(acknowledgeReportRequest, Internal).map {
                acknowledgeReport:ServiceOutcome[Int] =>
                  acknowledgeReport match {
                    case Right(ResponseWrapper(responseCorrelationId, returnCode)) =>
                      val retFuture: Future[Result] = returnCode match {
                        case NO_CONTENT =>
                          val retFutureOk: Future[Result] = Future(NoContent.withApiHeaders(correlationId))
                          retFutureOk
                        case _ =>
                          val retError: Future[Result] = Future(BadRequest(Json.toJson(DownstreamError)).withApiHeaders(correlationId))
                          retError
                      }
                      retFuture
                    case Left(errorWrapper) =>
                      val ret: Future[Result] = Future(BadRequest(Json.toJson(errorWrapper.error)).withApiHeaders(correlationId))
                      ret
                    case _ =>
                      val retError: Future[Result] = Future(BadRequest(Json.toJson( ServiceUnavailableError )).withApiHeaders(correlationId))
                      retError
                  }

              }
              retAcknowledgeReport
            }.flatten

            acknowledge

          case Left(errorWrapper) =>  Future(BadRequest(Json.toJson(errorWrapper.error)).withApiHeaders(correlationId))
          case _ =>  Future(BadRequest(Json.toJson(ServiceUnavailableError)).withApiHeaders(correlationId))
        }

      response
    }


  private def acknowledgeReport(request: AcknowledgeReportRequest, origin: Origin)(implicit hc: HeaderCarrier,
                                                                                   ec: ExecutionContext,
                                                                                   //  logContext: EndpointLogContext,
                                                                                   userRequest: UserRequest[_],
                                                                                   correlationId: String): Future[ServiceOutcome[Int]] = {
    logger.info(s"${correlationId} Received request to acknowledge assessment report for Self Assessment [${request.feedbackId}]")
    //    doImplicitAuditing()
    //    auditRequestToAcknowledge(request)
    //TODO Fix me dont need to return status code at this level

    Future {
      val ret: Future[ServiceOutcome[Int]] = rdsService.acknowlege(request).flatMap {
        acknowledgeReportSO: ServiceOutcome[NewRdsAssessmentReport] =>
          val ret: Future[ServiceOutcome[Int]] = acknowledgeReportSO match {
            case Right(ResponseWrapper(correlationIdResponse, acknowledgeReport)) => {
              val reponseCode:Int = Try(acknowledgeReport.responseCode.toInt)
                .getOrElse(BAD_REQUEST)
              val ret: Future[ServiceOutcome[Int]] = reponseCode match {
                //TODO This status code doesn't look right, need to check the response code from RDS it might be 2xx
                case a if (a == NO_CONTENT) => {
                  logger.info(s"rds ack response is ${a}")

                  //TODO submissionTimestamp should this be current time?
                  val submissionTimestamp = currentDateTime.getDateTime
                  val body = s"""{"reportId":"${request.feedbackId}"}"""
                  val taxYearFromResponse: String = DesTaxYear.fromDesIntToString(acknowledgeReport.taxYear)
                  val reportAcknowledgementContent = RequestData(request.nino, RequestBody(body, reportId = request.feedbackId))

                  logger.info(s"... submitting acknowledgement to NRS")

                  //Submit asynchronously to NRS
                  nonRepudiationService.submit(reportAcknowledgementContent, submissionTimestamp, AssistReportAcknowledged, taxYearFromResponse)
                  //TODO confirm documentation if nrs failure needs to handled/audited?
                  logger.info("... report submitted to NRS returning.")
                  Future(Right(ResponseWrapper(correlationId, NO_CONTENT)))
                }
                case a if (a==BAD_REQUEST) =>
              	  Future(Left(ErrorWrapper(correlationId, DownstreamError)): ServiceOutcome[Int])
                case _ =>
                  Future(Left(ErrorWrapper(correlationId, DownstreamError)): ServiceOutcome[Int])
              }
              ret
            }
            case Left(errorWrapper) =>
              Future(Left( errorWrapper): ServiceOutcome[Int])
          }
          ret
      }
      ret
    }.flatten

  }

  //private def asError(message: String): JsObject = Json.obj("message" -> message)


}
