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
import uk.gov.hmrc.transactionalrisking.models.domain.{Internal, Origin}
import uk.gov.hmrc.transactionalrisking.models.errors.{BadRequestError, DownstreamError, ErrorWrapper}
import uk.gov.hmrc.transactionalrisking.models.request.AcknowledgeReportRawData
import uk.gov.hmrc.transactionalrisking.services.cip.InsightService
import uk.gov.hmrc.transactionalrisking.services.eis.IntegrationFrameworkService
import uk.gov.hmrc.transactionalrisking.services.nrs.NrsService
import uk.gov.hmrc.transactionalrisking.services.nrs.models.request.{AcknowledgeReportRequest, AssistReportAcknowledged, GenerarteReportRequestBody, GenerateReportRequest}
import uk.gov.hmrc.transactionalrisking.services.rds.RdsService
import uk.gov.hmrc.transactionalrisking.services.rds.models.response.RdsAcknowledgementResponse
import uk.gov.hmrc.transactionalrisking.services.EnrolmentsAuthService
import uk.gov.hmrc.transactionalrisking.utils.{CurrentDateTime, Logging}

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AcknowledgeReportController @Inject()(
                                             val cc: ControllerComponents,
                                             requestParser: AcknowledgeRequestParser,
                                             val authService: EnrolmentsAuthService,
                                             nonRepudiationService: NrsService,
                                             rdsService: RdsService,
                                             currentDateTime: CurrentDateTime,
                                           )(implicit ec: ExecutionContext) extends AuthorisedController(cc) with BaseController with Logging {
  //TODO revisit if reportId needs to be UUID instead of string? as regex validation is done anyway
  def acknowledgeReportForSelfAssessment(nino: String, reportId: String, rdsReportReferencesId:String): Action[AnyContent] =
    authorisedAction(nino, nrsRequired = true).async { implicit request => {
      implicit val correlationId: String = UUID.randomUUID().toString
      logger.info(s"Received request to acknowledge assessment report: [$reportId]")


      val parsedRequest: Either[ErrorWrapper, AcknowledgeReportRequest] = requestParser.parseRequest(AcknowledgeReportRawData(nino, reportId))
      val response = parsedRequest.map(req => acknowledgeReport(req, Internal))

      response match {
        case Right(value) => {
          value.map(r => logger.info(s"RDS success response $r"))

          Future(NoContent)
        }
        case Left(value) => Future(BadRequest(Json.toJson(value)))
      }
      //
      //        for{
      //          parsedRequest: AcknowledgeReportRequest <- requestParser.parseRequest(AcknowledgeReportRawData(nino, reportId))
      //          response <- acknowledgeReport(parsedRequest,Internal)
      //        }yield {
      //          response.map{v =>
      //            logger.info(s"RDS success response $v")
      //            Future(NoContent)
      //          }
      //          }.recoverWith(Future(BadRequest(asError("Please provide valid ID of an Assessment Report."))))
    }
    }

  private def acknowledgeReport(request: AcknowledgeReportRequest, origin: Origin)(implicit hc: HeaderCarrier,
                                                                                   ec: ExecutionContext,
                                                                                   //  logContext: EndpointLogContext,
                                                                                   userRequest: UserRequest[_],
                                                                                   correlationId: String) = {
    logger.info(s"${correlationId} Received request to acknowledge assessment report for Self Assessment [${request.feedbackId}]")
    //    doImplicitAuditing() // TODO: This should be at the controller level.
    //    auditRequestToAcknowledge(request)
    //TODO Fix me dont need to retun status code at this level
    rdsService.acknowlege(request).map(_ match {
      //TODO This status code doesn't look right, need to check the reponse code from RDS it might be 2xx
      case a if (a == 204) =>
        logger.info(s"rds ack response is ${a}")
        val submissionTimestamp = currentDateTime.getDateTime
        val nrsId = request.nino //TODO generate nrs id as per the spec
        val body = s"""{"reportId":"${request.feedbackId}"}"""

        val reportAcknowledgementContent = GenerateReportRequest(nrsId, GenerarteReportRequestBody(body, reportId = request.feedbackId))
        logger.info(s"... submitting acknowledgement to NRS with body $reportAcknowledgementContent")
        //Submit asynchronously to NRS
        nonRepudiationService.submit(reportAcknowledgementContent, nrsId, submissionTimestamp, AssistReportAcknowledged)
        //TODO confirm documentation if nrs failure needs to handled/audited?
        logger.info("... report submitted to NRS returning.")
        Future(OK)

      case _ => Future(INTERNAL_SERVER_ERROR)
    }
    ).flatten
  }

  private def asError(message: String): JsObject = Json.obj("message" -> message)


}
