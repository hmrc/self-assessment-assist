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

package uk.gov.hmrc.transactionalrisking.v1.connectors.httpParsers

import play.api.http.Status.{FORBIDDEN, OK, UNAUTHORIZED}
import play.api.libs.json.{Reads, __}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.transactionalrisking.v1.models.errors.{DownstreamError, InvalidBearerTokenError, MtdError, NinoFormatError}



object MtdIdLookupHttpParser {

  private val mtdIdJsonReads: Reads[String] = (__ \ "mtdbsa").read[String]

  implicit val mtdIdLookupHttpReads: HttpReads[Either[MtdError, String]] = (_: String, _: String, response: HttpResponse) => {
    response.status match {
      case OK =>
        response.json.asOpt[String](mtdIdJsonReads) match {
          case Some(mtdId) => Right(mtdId)
          case None        => Left(DownstreamError)
        }
      case FORBIDDEN    => Left(NinoFormatError)
      case UNAUTHORIZED => Left(InvalidBearerTokenError)
      case _            => Left(DownstreamError)
    }
  }

}