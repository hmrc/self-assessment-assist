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

package uk.gov.hmrc.transactionalrisking.v1.services.nrs

import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.transactionalrisking.utils.{DateUtils, HashUtil, Logging}
import uk.gov.hmrc.transactionalrisking.v1.controllers.UserRequest
import uk.gov.hmrc.transactionalrisking.v1.models.domain.AssessmentReport
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.request._
import uk.gov.hmrc.transactionalrisking.v1.services.nrs.models.response.NrsResponse

import java.time.OffsetDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NrsService @Inject()(connector: NrsConnector,
                            hashUtil: HashUtil) extends Logging {
  //                           override val metrics: Metrics) extends Timer with Logging { TODO include metrics later

  def buildNrsSubmission(payload: String,
                         reportId: String,
                         submissionTimestamp: OffsetDateTime,
                         request: UserRequest[_], notableEventType: NotableEventType)(implicit correlationId: String): NrsSubmission = {

    logger.info(s"$correlationId::[buildNrsSubmission]Build the NRS submission")

    val encodedPayload = hashUtil.encode(payload)
    val sha256Checksum = hashUtil.getHash(payload)
    val formattedDate = submissionTimestamp.format(DateUtils.isoInstantDatePattern)
    //TODO refer https://confluence.tools.tax.service.gov.uk/display/NR/Transactional+Risking+Service+-+API+-+NRS+Assessment

    NrsSubmission(
      payload = encodedPayload,
      Metadata(
        businessId = "saa",
        notableEvent = notableEventType.value,
        payloadContentType = "application/json",
        payloadSha256Checksum = sha256Checksum,
        userSubmissionTimestamp = formattedDate,
        identityData = request.userDetails.identityData,
        userAuthToken = request.headers.get("Authorization").get,  //TODO:Fix error handling for get throws. Maybe build NRS should be moved out of class.
        headerData = Json.toJson(request.headers.toMap.map { h => h._1 -> h._2.head }),//TODO remove auth header
        searchKeys =
          SearchKeys(
            reportId = reportId
          )
      )
    )
  }

  def submit(submission: NrsSubmission, key: NotableEventType)(implicit hc: HeaderCarrier,
                                                ec: ExecutionContext,
                                                correlationId: String): Future[Option[NrsResponse]] = {
    logger.info(s"$correlationId::[submit] Request initiated to store ${key.value} content to NRS")
    connector.submit(submission).map {
      case Right(value) =>
        logger.info(s"$correlationId::[submit] Successful submission")
        Some(value)
      case Left(_) =>
        logger.info(s"$correlationId::[submit] Error occurred when submitting NRS")
        None
    }
  }

  def submit(report: AssessmentReport, submissionTimestamp: OffsetDateTime)(
    implicit request: UserRequest[_],
    hc: HeaderCarrier,
    ec: ExecutionContext,
    correlationId: String
  ): Future[Option[NrsResponse]] = {
    val payload = report.stringify
    val nrsSubmission = buildNrsSubmission(payload, report.reportId.toString, submissionTimestamp, request, AssistReportGenerated)
    submit(nrsSubmission, AssistReportGenerated)
  }

  def submit(reportId: AcknowledgeReportId, submissionTimestamp: OffsetDateTime)(
    implicit request: UserRequest[_],
    hc: HeaderCarrier,
    ec: ExecutionContext,
    correlationId: String
  ): Future[Option[NrsResponse]] = {
    //TODO this has to come outside of this method, as failure in building NRS Request should fail the transaction
    val payload = reportId.stringify
    val nrsSubmission = buildNrsSubmission(payload, reportId.reportId, submissionTimestamp, request, AssistReportAcknowledged)

    submit(nrsSubmission, AssistReportAcknowledged)
  }

}
