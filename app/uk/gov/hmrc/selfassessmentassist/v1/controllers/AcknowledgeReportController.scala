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

package uk.gov.hmrc.selfassessmentassist.v1.controllers

import play.api.mvc._
import uk.gov.hmrc.selfassessmentassist.api.connectors.MtdIdLookupConnector
import uk.gov.hmrc.selfassessmentassist.api.controllers._
import uk.gov.hmrc.selfassessmentassist.utils.{CurrentDateTime, IdGenerator, Logging}
import uk.gov.hmrc.selfassessmentassist.v1.Orchestrator
import uk.gov.hmrc.selfassessmentassist.v1.models.request.AcknowledgeReportRawData
import uk.gov.hmrc.selfassessmentassist.v1.requestParsers.AcknowledgeRequestParser
import uk.gov.hmrc.selfassessmentassist.v1.services.{EnrolmentsAuthService, IfsService, NrsService, RdsService}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AcknowledgeReportController @Inject() (
    val cc: ControllerComponents,
    parser: AcknowledgeRequestParser,
    val authService: EnrolmentsAuthService,
    val lookupConnector: MtdIdLookupConnector,
    nonRepudiationService: NrsService,
    rdsService: RdsService,
    currentDateTime: CurrentDateTime,
    idGenerator: IdGenerator,
    ifsService: IfsService
)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc)
    with Logging {

  def acknowledgeReportForSelfAssessment(nino: String, reportId: String, rdsCorrelationId: String): Action[AnyContent] = {
    implicit val correlationId: String = idGenerator.generateCorrelationId
    implicit val endpointLogContext: EndpointLogContext =
      EndpointLogContext(controllerName = "AcknowledgeReportController", endpointName = "reportsAcknowledge")

    logger.debug(s"$correlationId::[acknowledgeReportForSelfAssessment]Received request to acknowledge assessment report")

    val submissionTimestamp = currentDateTime.getDateTime

    authorisedAction(nino).async { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(correlationId, endpointLogContext)

      val orchestrator = new Orchestrator(rdsService, ifsService, nonRepudiationService)

      val requestHandler =
        RequestHandler
          .withParser(parser)
          .withService(orchestrator.orchestrate(_, request, reportId, submissionTimestamp))
          .withNoContentResult()

      requestHandler.handleRequest(AcknowledgeReportRawData(nino, reportId, rdsCorrelationId))
    }
  }

}
