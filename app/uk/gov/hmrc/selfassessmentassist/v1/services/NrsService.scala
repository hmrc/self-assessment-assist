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

package uk.gov.hmrc.selfassessmentassist.v1.services

import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}
import uk.gov.hmrc.selfassessmentassist.api.controllers.UserRequest
import uk.gov.hmrc.selfassessmentassist.utils.{DateUtils, HashUtil, Logging}
import uk.gov.hmrc.selfassessmentassist.v1.connectors.NrsConnector
import uk.gov.hmrc.selfassessmentassist.v1.models.request.nrs.{Metadata, NotableEventType, NrsSubmission, SearchKeys}
import uk.gov.hmrc.selfassessmentassist.v1.models.response.nrs.NrsFailure

import java.time.OffsetDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scala.util.Try

@Singleton
class NrsService @Inject() (connector: NrsConnector, hashUtil: HashUtil) extends Logging {

  def buildNrsSubmission(payload: String,
                         reportId: String,
                         submissionTimestamp: OffsetDateTime,
                         request: UserRequest[_],
                         notableEventType: NotableEventType)(implicit correlationId: String): Either[NrsFailure, NrsSubmission] = {

    logger.debug(s"$correlationId::[buildNrsSubmission] Building the NRS submission")

    val userAuthToken: Option[String] = request.headers.get(HeaderNames.authorisation)

    userAuthToken match {
      case Some(token) =>
        Try {
          val encodedPayload = hashUtil.encode(payload)
          val sha256Checksum = hashUtil.getHash(payload)
          val formattedDate  = submissionTimestamp.format(DateUtils.isoInstantDateTimePattern)

          NrsSubmission(
            payload = encodedPayload,
            Metadata(
              businessId = "saa",
              notableEvent = notableEventType.value,
              payloadContentType = "application/json",
              payloadSha256Checksum = sha256Checksum,
              userSubmissionTimestamp = formattedDate,
              identityData = request.userDetails.identityData,
              userAuthToken = token,
              headerData = Json.toJson(request.headers.toMap.filterNot { case (k, _) => k.equals("authorization") }.map { h => h._1 -> h._2.head }),
              searchKeys = SearchKeys(
                reportId = reportId
              )
            )
          )
        }.fold(
          error => {
            logger.error(s"$correlationId::[buildNrsSubmission] unable to build NRS event due to $error")
            Left(NrsFailure.Exception("failed to create submission request data"))
          },
          event => {
            logger.info(s"$correlationId::[buildNrsSubmission] successfully built NRS event for submission")
            Right(event)
          }
        )
      case None =>
        logger.error(s"$correlationId::[buildNrsSubmission] unable to build NRS event, no user bearer token")
        Left(NrsFailure.Exception("no beaker token for user"))
    }

  }

  def submit(submission: NrsSubmission)(implicit hc: HeaderCarrier, correlationId: String): Future[NrsOutcome] = connector.submit(submission)

}
