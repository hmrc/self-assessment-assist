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

package uk.gov.hmrc.selfassessmentassist.api.requestParsers.validators.validations

import uk.gov.hmrc.selfassessmentassist.api.models.errors.{CalculationIdFormatError, MtdError}

import java.util.UUID
import scala.util.Try

object CalculationIdValidation {
  private val calculationIdRegex = "^[0-9]{8}|[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"

  private def toId(rawId: String): Option[UUID] =
    Try(UUID.fromString(rawId)).toOption

  def validate(calculationId: String): List[MtdError] = toId(calculationId) match {
    case Some(_) => if (calculationId matches calculationIdRegex) NoValidationErrors else List(CalculationIdFormatError)
    case None    => List(CalculationIdFormatError)
  }

}
