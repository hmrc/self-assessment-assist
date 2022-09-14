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

package uk.gov.hmrc.transactionalrisking.models.domain

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsPath, Reads}
import uk.gov.hmrc.transactionalrisking.models.domain.CustomerType.CustomerType
import uk.gov.hmrc.transactionalrisking.models.domain.PreferredLanguage.PreferredLanguage

import java.util.UUID

case class AssessmentRequestForSelfAssessment(calculationId: UUID,
                                              nino: String,
                                              preferredLanguage: PreferredLanguage,
                                              customerType: CustomerType,
                                              agentRef: Option[String],
                                              taxYear: String
                                             )

object AssessmentRequestForSelfAssessment {

  implicit val reads: Reads[AssessmentRequestForSelfAssessment] =
    (JsPath \ "calculation_id").read[UUID]
      .and((JsPath \ "nino").read[String])
      .and((JsPath \ "preferred_language").read[PreferredLanguage])
      .and((JsPath \ "customer_type").read[CustomerType])
      .and((JsPath \ "agent_ref").readNullable[String])
      .and((JsPath \ "tax_year").read[String])(AssessmentRequestForSelfAssessment.apply _)


}