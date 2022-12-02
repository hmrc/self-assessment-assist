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

package uk.gov.hmrc.transactionalrisking.v1.models.domain

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{JsPath, Reads, Writes}
import uk.gov.hmrc.transactionalrisking.v1.models.domain.FraudDecision.FraudRiskDecision


// This is still being determined; please see TRDT-85.
case class FraudRiskReport( score: Int, headers: Set[FraudRiskHeader], fraudRiskReportReasons: Set[FraudRiskReportReason])

object FraudRiskReport {

  implicit val reads: Reads[FraudRiskReport] =
    (JsPath \ "score").read[Int]
      .and((JsPath \ "headers").readWithDefault[Set[FraudRiskHeader]](Set.empty))
      .and((JsPath \ "fraudRiskReportReasons").readWithDefault[Set[FraudRiskReportReason]](Set.empty))(FraudRiskReport.apply _)

  implicit val writes: Writes[FraudRiskReport] =
   (JsPath \ "score").write[Int]
      .and((JsPath \ "headers").write[Set[FraudRiskHeader]])
      .and((JsPath \ "fraudRiskReportReasons").write[Set[FraudRiskReportReason]])(unlift(FraudRiskReport.unapply))

}