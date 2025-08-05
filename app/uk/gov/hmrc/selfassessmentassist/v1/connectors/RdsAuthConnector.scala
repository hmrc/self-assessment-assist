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

package uk.gov.hmrc.selfassessmentassist.v1.connectors

import cats.data.EitherT
import com.google.common.base.Charsets
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.selfassessmentassist.api.models.auth.RdsAuthCredentials
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{MtdError, RdsAuthDownstreamError}
import uk.gov.hmrc.selfassessmentassist.utils.Logging

import java.util.Base64
import com.google.inject.ImplementedBy
import play.api.http.Status.{ACCEPTED, OK}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.selfassessmentassist.config.AppConfig

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultRdsAuthConnector])
trait RdsAuthConnector[F[_]] {
  def retrieveAuthorisedBearer()(implicit hc: HeaderCarrier, correlationId: String): EitherT[F, MtdError, RdsAuthCredentials]
}

class DefaultRdsAuthConnector @Inject() (http: HttpClientV2)(implicit
    appConfig: AppConfig,
    ec: ExecutionContext
) extends RdsAuthConnector[Future]
    with Logging {

  override def retrieveAuthorisedBearer()(implicit
      hc: HeaderCarrier,
      correlationId: String
  ): EitherT[Future, MtdError, RdsAuthCredentials] = {

    val url: String = appConfig.rdsSasBaseUrlForAuth

    val base64EncodedCredentials: String = Base64.getEncoder.encodeToString(
      s"${appConfig.rdsAuthCredential.client_id}:${appConfig.rdsAuthCredential.client_secret}".getBytes(Charsets.UTF_8)
    )

    val reqHeaders: Seq[(String, String)] = Seq(
      "Content-Type"  -> "application/x-www-form-urlencoded",
      "Accept"        -> "application/json",
      "Authorization" -> s"Basic $base64EncodedCredentials"
    )

    logger.debug(s"$correlationId::[retrieveAuthorisedBearer] request info url=$url")
    EitherT {
      http
        .post(url"$url")
        .setHeader(reqHeaders: _*)
        .withBody(Map("grant_type" -> Seq(appConfig.rdsAuthCredential.grant_type)))
        .withProxy
        .execute[HttpResponse]
        .map { response =>
          logger.debug(s"$correlationId::[retrieveAuthorisedBearer] response is $response}")
          response.status match {
            case ACCEPTED =>
              logger.info(s"$correlationId::[retrieveAuthorisedBearer] ACCEPTED response")
              handleResponse(response)
            case OK =>
              logger.info(s"$correlationId::[retrieveAuthorisedBearer] Ok response")
              handleResponse(response)
            case errorStatusCode =>
              logger.error(s"$correlationId::[retrieveAuthorisedBearer] failed status $errorStatusCode and body: ${response.body}")
              Left(RdsAuthDownstreamError)
          }
        }
        .recover {
          case ex: HttpException =>
            logger.error(s"$correlationId::[retrieveAuthorisedBearer] HttpException=$ex")
            Left(RdsAuthDownstreamError)
          case ex: UpstreamErrorResponse =>
            logger.error(s"$correlationId::[retrieveAuthorisedBearer] UpstreamErrorResponse=$ex")
            Left(RdsAuthDownstreamError)
        }
    }
  }

  private def handleResponse(response: HttpResponse): Either[MtdError, RdsAuthCredentials] =
    response.json.asOpt[RdsAuthCredentials].toRight(RdsAuthDownstreamError)

}
