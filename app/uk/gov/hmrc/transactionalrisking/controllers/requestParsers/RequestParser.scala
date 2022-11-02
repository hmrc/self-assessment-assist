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

package uk.gov.hmrc.transactionalrisking.controllers.requestParsers

import uk.gov.hmrc.transactionalrisking.controllers.requestParsers.validators.Validator
import uk.gov.hmrc.transactionalrisking.models.errors.{BadRequestError, ErrorWrapper}
import uk.gov.hmrc.transactionalrisking.models.outcomes.ResponseWrapper
import uk.gov.hmrc.transactionalrisking.models.request.RawData
import uk.gov.hmrc.transactionalrisking.services.ParseOutcome
import uk.gov.hmrc.transactionalrisking.utils.Logging

trait RequestParser[Raw <: RawData, Request] extends Logging {
  val validator: Validator[Raw]

  protected def requestFor(data: Raw): Request

  def parseRequest(data: Raw)(implicit correlationID: String): ParseOutcome[ Request] = {
    validator.validate(data) match {
      case Nil =>
        logger.info(message = s"$correlationID::[RequestParser][parseRequest]" +
          s"Validation successful for the request")
        Right( ResponseWrapper( correlationID, requestFor(data)))
      case err :: Nil =>
        logger.error(message = s"$correlationID::[RequestParser][parseRequest]" +
          s"Validation failed with ${err.code} error for the request")
        Left(ErrorWrapper( correlationID, err, None))
      case errs =>
        logger.error(s"$correlationID::[RequestParser][parseRequest]" +
          s"Validation failed with ${errs.map(_.code).mkString(",")} errors for the request")
        Left(ErrorWrapper( correlationID, BadRequestError, Some(errs)))
    }
  }
}
