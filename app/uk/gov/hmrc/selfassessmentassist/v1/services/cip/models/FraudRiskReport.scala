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

package uk.gov.hmrc.selfassessmentassist.v1.services.cip.models

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{JsPath, Reads, Writes}

case class FraudRiskReport(score: Int, riskCorrelationId: String, reasons: Seq[String])

object FraudRiskReport {

  implicit val reads: Reads[FraudRiskReport] =
    (JsPath \ "riskScore")
      .read[Int]
      .and((JsPath \ "riskCorrelationId").read[String])
      .and((JsPath \ "reasons").readWithDefault[Seq[String]](Seq.empty))(FraudRiskReport.apply _)

  implicit val writes: Writes[FraudRiskReport] =
    (JsPath \ "riskScore")
      .write[Int]
      .and((JsPath \ "riskCorrelationId").write[String])
      .and((JsPath \ "reasons").write[Seq[String]])(unlift(FraudRiskReport.unapply))

}
