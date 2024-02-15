/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.selfassessmentassist.api.models.auth.RdsAuthCredentials
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{MtdError, RdsAuthDownstreamError}
import uk.gov.hmrc.selfassessmentassist.utils.Logging

import java.util.Base64
//import cats.implicits.catsSyntaxEitherId
import com.google.inject.ImplementedBy
import play.api.http.Status.{ACCEPTED, OK}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.selfassessmentassist.config.AppConfig

import java.net.URLEncoder
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultRdsAuthConnector])
trait RdsAuthConnector[F[_]] {
  def retrieveAuthorisedBearer()(implicit hc: HeaderCarrier, correlationId: String): EitherT[F, MtdError, RdsAuthCredentials]
}

class DefaultRdsAuthConnector @Inject() (@Named("nohook-auth-http-client") http: HttpClient)(implicit
    appConfig: AppConfig,
    ec: ExecutionContext
) extends RdsAuthConnector[Future]
    with Logging {

  override def retrieveAuthorisedBearer()(implicit
      hc: HeaderCarrier,
      correlationId: String
  ): EitherT[Future, MtdError, RdsAuthCredentials] = {

    val url = s"${appConfig.rdsSasBaseUrlForAuth}"

    val utfEncodedClientId  = URLEncoder.encode(appConfig.rdsAuthCredential.client_id, "UTF-8")
    val utfEncodedSecret    = URLEncoder.encode(appConfig.rdsAuthCredential.client_secret, "UTF-8")
    val utfEncodedGrantType = URLEncoder.encode(appConfig.rdsAuthCredential.grant_type, "UTF-8")

    val body = s"grant_type=$utfEncodedGrantType"

    val credentials              = s"$utfEncodedClientId:$utfEncodedSecret"
    val base64EncodedCredentials = Base64.getEncoder.encodeToString(credentials.getBytes)

    val reqHeaders = Seq(
      "Content-type"  -> "application/x-www-form-urlencoded",
      "Accept"        -> "application/json",
      "Authorization" -> s"Basic $base64EncodedCredentials")

    logger.debug(s"$correlationId::[retrieveAuthorisedBearer] request info url=$url")
    EitherT {
      http
        .POSTString(url, body, headers = reqHeaders)
        .map { response =>
          logger.debug(s"$correlationId::[retrieveAuthorisedBearer] response is $response}")
          response.status match {
            case ACCEPTED =>
              logger.info(s"$correlationId::[retrieveAuthorisedBearer] ACCEPTED reponse")
              handleResponse(response)
            case OK =>
              logger.info(s"$correlationId::[retrieveAuthorisedBearer] Ok reponse")
              handleResponse(response)
            case errorStatusCode =>
              logger.error(s"$correlationId::[retrieveAuthorisedBearer] failed $errorStatusCode")
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
