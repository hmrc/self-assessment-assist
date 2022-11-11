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

package uk.gov.hmrc.transactionalrisking.services.nrs

import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transactionalrisking.controllers.AuthorisedController.ninoKey
import uk.gov.hmrc.transactionalrisking.controllers.UserRequest
import uk.gov.hmrc.transactionalrisking.services.nrs.models.request._
import uk.gov.hmrc.transactionalrisking.services.nrs.models.response.NrsResponse
import uk.gov.hmrc.transactionalrisking.utils.{DateUtils, HashUtil, Logging}

import java.time.OffsetDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NrsService @Inject()(
                            //                            auditService: AuditService,
                            connector: NrsConnector,
                            hashUtil: HashUtil) extends Logging {
  //                           override val metrics: Metrics) extends Timer with Logging { TODO include metrics later

  def buildNrsSubmission(requestData: RequestData,
                         submissionTimestamp: OffsetDateTime,
                         request: UserRequest[_], notableEventType: NotableEventType, taxYear: String, correlationID:String): NrsSubmission = {
    logger.info(s"$correlationID::[buildNrsSubmission]Build the NRS submission")

    //RequestData(nino = nino, RequestBody(newRdsAssessmentReportResponse.toString, calculationID))
    //TODO fix me later, body will be instance of class NewRdsAssessmentReport
    val payloadString = Json.toJson(requestData.body).toString()
    val encodedPayload = hashUtil.encode(payloadString)
    val sha256Checksum = hashUtil.getHash(payloadString)
    val formattedDate = submissionTimestamp.format(DateUtils.isoInstantDatePattern)
    //TODO refer https://confluence.tools.tax.service.gov.uk/display/NR/Transactional+Risking+Service+-+API+-+NRS+Assessment

    NrsSubmission(
      payload = encodedPayload,
      Metadata(
        businessId = "self-assessment-assist",
        notableEvent = notableEventType.value, //assist-report-generated,assist-report-acknowledged
        payloadContentType = "application/json",
        payloadSha256Checksum = sha256Checksum,
        userSubmissionTimestamp = formattedDate,
        identityData = request.userDetails.identityData,
        userAuthToken = request.headers.get("Authorization").get,  //TODO:Fix error handling for get throws. Maybe build NRS should be moved out of class.
        headerData = Json.toJson(request.headers.toMap.map { h => h._1 -> h._2.head }),
        searchKeys =
          SearchKeys(
            nino = ninoKey,
            taxYear = taxYear,
            reportId = requestData.body.reportId,
          )
      )
    )
  }

  def submit(requestData: RequestData, submissionTimestamp: OffsetDateTime, notableEventType: NotableEventType, taxYear: String)(
    implicit request: UserRequest[_],
    hc: HeaderCarrier,
    ec: ExecutionContext,
    correlationID: String
  ): Future[Option[NrsResponse]] = {
    logger.info(s"$correlationID::[submit]submit the data to nrs")

    val nrsSubmission = buildNrsSubmission(requestData, submissionTimestamp, request, notableEventType, taxYear, correlationID)
    logger.info(s"$correlationID::[submit]Request initiated to store report content to NRS")
    connector.submit(nrsSubmission).map { response =>
      val ret = response.toOption
      ret match {
          case Some(response) =>
            logger.info(s"$correlationID::[submit]Succesful submission")
            response
          case None =>
            logger.info(s"$correlationID::[submit]Nothing submitted")
            None
      }
      ret
    }

  }

}
