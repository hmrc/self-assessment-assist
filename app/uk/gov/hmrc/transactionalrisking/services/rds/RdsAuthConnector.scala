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

package uk.gov.hmrc.transactionalrisking.services.rds

import cats.data.EitherT
import uk.gov.hmrc.transactionalrisking.models.errors.MtdError
import uk.gov.hmrc.transactionalrisking.utils.Logging
//import cats.implicits.catsSyntaxEitherId
import com.google.inject.ImplementedBy
import play.api.http.Status.{ACCEPTED, OK}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.transactionalrisking.config.AppConfig
import uk.gov.hmrc.transactionalrisking.models.auth.RdsAuthCredentials
import uk.gov.hmrc.transactionalrisking.models.errors.RdsAuthError
import java.net.URLEncoder
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultRdsAuthConnector])
trait RdsAuthConnector[F[_]] {
  def retrieveAuthorisedBearer()(implicit hc: HeaderCarrier): EitherT[F, MtdError, RdsAuthCredentials]
}

class DefaultRdsAuthConnector @Inject() (@Named("nohook-auth-http-client") http: HttpClient)(implicit
                                                                                            appConfig: AppConfig,
                                                                                            ec: ExecutionContext
) extends RdsAuthConnector[Future] with Logging{

  override def retrieveAuthorisedBearer()(implicit
                                          hc: HeaderCarrier
  ): EitherT[Future, MtdError, RdsAuthCredentials] = {

    val url = s"${appConfig.rdsSasBaseUrlForAuth}"

    //val body = s"&grant_type=${URLEncoder.encode(appConfig.rdsAuthCredential.grant_type, "UTF-8")}"
    val body = s"client_id=${URLEncoder.encode(appConfig.rdsAuthCredential.client_id, "UTF-8")}" +
      s"&client_secret=${URLEncoder.encode(appConfig.rdsAuthCredential.client_secret, "UTF-8")}" +
      s"&grant_type=${URLEncoder.encode(appConfig.rdsAuthCredential.grant_type, "UTF-8")}"+
      s"&code=${URLEncoder.encode("o1a2fXTJVU", "UTF-8")}"


    logger.info(s"RDSConnector :: request info url=$url body=$body")
    EitherT {
      http
        .POSTString(url, body, headers = Seq("Content-type" -> "application/x-www-form-urlencoded"))
        .map { response =>
          logger.info(s"RDSConnector :: response is ${response.json}")
          response.status match {
            case ACCEPTED        =>
              logger.info(s"RDSConnector :: ACCEPTED reponse")
              handleResponse(response)
            case OK              => logger.info(s"RDSConnector :: Ok reponse")
              handleResponse(response)
            case errorStatusCode => Left(RdsAuthError)
          }
        }
        .recover {
          case ex: HttpException         =>
            logger.error(s"RDSConnector :: HttpException=$ex")
            Left(RdsAuthError)
          case ex: UpstreamErrorResponse =>
            logger.error(s"RDSConnector :: UpstreamErrorResponse=$ex")
            Left(RdsAuthError)
        }
    }
  }

  private def handleResponse(response: HttpResponse): Either[MtdError, RdsAuthCredentials] =
    response.json.asOpt[RdsAuthCredentials].toRight(RdsAuthError)

//  private def handleError(statusCode: Int): Either[RdsAuthError, RdsAuthCredentials] =
//    RdsAuthError(statusCode).asLeft[RdsAuthCredentials]

}