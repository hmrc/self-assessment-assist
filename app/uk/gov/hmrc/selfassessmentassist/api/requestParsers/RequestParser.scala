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

package uk.gov.hmrc.selfassessmentassist.api.requestParsers

import uk.gov.hmrc.selfassessmentassist.api.models.errors
import uk.gov.hmrc.selfassessmentassist.api.models.errors.{BadRequestError, ErrorWrapper, MtdError}
import uk.gov.hmrc.selfassessmentassist.api.models.request.RawData
import uk.gov.hmrc.selfassessmentassist.api.requestParsers.validators.Validator
import uk.gov.hmrc.selfassessmentassist.utils.Logging
import uk.gov.hmrc.selfassessmentassist.v1.services.ParseOutcome

import scala.concurrent.{ExecutionContext, Future}

trait RequestParser[Raw <: RawData, Request] extends Logging {
  val validator: Validator[Raw]

  protected def requestFor(data: Raw): Either[MtdError, Request]

  def parseRequest(data: Raw)(implicit ec: ExecutionContext, correlationId: String): Future[ParseOutcome[Request]] = {
    validator.validate(data) match {
      case Nil => Future(requestFor(data).fold(e => Left(ErrorWrapper(correlationId, e, None)), r => Right(r)))
      case err :: Nil =>
        logger.error(message = s"$correlationId::[RequestParser][parseRequest]" +
          s"Validation failed with ${err.code} error for the request")
        Future(Left(ErrorWrapper(correlationId, err, None)))
      case errs =>
        logger.error(s"$correlationId::[RequestParser][parseRequest]" +
          s"Validation failed with ${errs.map(_.code).mkString(",")} errors for the request")
        Future(Left(errors.ErrorWrapper(correlationId, BadRequestError, Some(errs))))
    }
  }

  def parseOrchestratedRequest(data: Raw)(implicit ec: ExecutionContext, correlationId: String): Either[ErrorWrapper, Request] = {
    validator.validate(data) match {
      case Nil => requestFor(data).fold(e => Left(ErrorWrapper(correlationId, e, None)), r => Right(r))
      case err :: Nil =>
        logger.error(message = s"$correlationId::[RequestParser][parseRequest]" +
          s"Validation failed with ${err.code} error for the request")
        Left(ErrorWrapper(correlationId, err, None))
      case errs =>
        logger.error(s"$correlationId::[RequestParser][parseRequest]" +
          s"Validation failed with ${errs.map(_.code).mkString(",")} errors for the request")
        Left(errors.ErrorWrapper(correlationId, BadRequestError, Some(errs)))
    }
  }
}
