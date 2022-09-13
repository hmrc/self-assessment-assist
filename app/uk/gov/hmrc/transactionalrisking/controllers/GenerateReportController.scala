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
import uk.gov.hmrc.transactionalrisking.models.domain._
import uk.gov.hmrc.transactionalrisking.services.cip.InsightService
import uk.gov.hmrc.transactionalrisking.services.eis.IntegrationFrameworkService
import uk.gov.hmrc.transactionalrisking.services.nrs.NrsService
import uk.gov.hmrc.transactionalrisking.services.nrs.models.request.{AssistReportGenerated, GenerarteReportRequestBody, GenerateReportRequest}
import uk.gov.hmrc.transactionalrisking.services.rds.RdsService
import uk.gov.hmrc.transactionalrisking.services.EnrolmentsAuthService
import uk.gov.hmrc.transactionalrisking.utils.{CurrentDateTime, Logging}

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class GenerateReportController @Inject()(
                                                val cc: ControllerComponents,
                                                val integrationFrameworkService: IntegrationFrameworkService,
                                                val authService: EnrolmentsAuthService,
                                                nonRepudiationService: NrsService,
                                                insightService: InsightService,
                                                rdsService:RdsService,
                                                currentDateTime: CurrentDateTime,
                                              )(implicit ec: ExecutionContext,
                                                correlationId: String) extends AuthorisedController(cc) with BaseController with Logging {

  def generateReportInternal(nino: String, calculationId: String): Action[AnyContent] =
    authorisedAction(nino, nrsRequired = true).async { implicit request =>
      //    doImplicitAuditing() // TODO: Fix me.
      //    doExplicitAuditingForGenerationRequest()
      implicit val correlationId: String = UUID.randomUUID().toString
      val customerType = deriveCustomerType(request)
      toId(calculationId).map { calculationIdUuid =>
        val calculationInfo = getCalculationInfo(calculationIdUuid, nino)
        val assessmentRequestForSelfAssessment = new AssessmentRequestForSelfAssessment(calculationIdUuid,
          nino,
          PreferredLanguage.English,
          customerType,
          None,
          calculationInfo.taxYear)
        //TODO Fix me put in for comprehension
        val fraudRiskReport: FraudRiskReport = insightService.assess(generateFraudRiskRequest(assessmentRequestForSelfAssessment))
        //    val fraudRiskReportStub = accessFraudRiskReport(generateFraudRiskRequest(request))
        val rdsAssessmentReportResponse: Future[AssessmentReport] = rdsService.submit(assessmentRequestForSelfAssessment, fraudRiskReport, Internal)

         Future(rdsAssessmentReportResponse.map { rdsReport =>
          val submitRequest = GenerateReportRequest(nino=nino, GenerarteReportRequestBody(rdsReport.toString, calculationId))
           //TODO below generate NRSId as per the spec
          nonRepudiationService.submit(submitRequest, generatedNrsId = nino, currentDateTime.getDateTime, AssistReportGenerated)
          rdsReport
        }.map(Json.toJson[AssessmentReport])
          .map(js => Ok(js))).flatten
      }.getOrElse(Future(BadRequest(asError("Please provide valid ID of an Assessment Report."))))//TODO Error desc maybe fix me


/*     val assessmentRequestForSelfAssessment =  for{
        calculationIdUuid <- toId(calculationId)
        calculationInfo <- getCalculationInfo2(calculationIdUuid, nino)


        //fraudRiskReport <- insightService.assess(generateFraudRiskRequest(assessmentRequestForSelfAssessment))
      } yield { calculationInfo =>
         AssessmentRequestForSelfAssessment(calculationIdUuid,
          nino,
          PreferredLanguage.English,
          customerType,
          None,
          taxYear = calculationInfo.taxYear)
      }*/
    }

  private def deriveCustomerType(request: Request[AnyContent]) = {
    //TODO fix me, write logic to derive customer type
    CustomerType.TaxPayer
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

  private def asError(message: String): JsObject = Json.obj("message" -> message)


  private def getCalculationInfo(id: UUID, nino: String): CalculationInfo =
    integrationFrameworkService.getCalculationInfo(id, nino)
      .getOrElse(throw new RuntimeException(s"Unknown calculation for id [$id] and nino [$nino]"))

}
