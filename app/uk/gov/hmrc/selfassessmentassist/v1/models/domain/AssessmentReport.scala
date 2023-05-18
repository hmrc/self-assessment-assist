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

package uk.gov.hmrc.selfassessmentassist.v1.models.domain

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{JsPath, Json, Writes}
import uk.gov.hmrc.selfassessmentassist.v1.services.rds.models.response.RdsAssessmentReport

import java.time.LocalDateTime
import java.util.UUID

case class AssessmentReportWrapper(calculationTimestamp: LocalDateTime, report: AssessmentReport, rdsAssessmentReport: RdsAssessmentReport)

case class AssessmentReport(reportId: UUID, risks: Seq[Risk], nino: String, taxYear: String, calculationId: UUID, rdsCorrelationId: String) {
  def stringify: String = Json.stringify(Json.toJson(this))
}

object AssessmentReport {

  implicit val writes: Writes[AssessmentReport] =
    (JsPath \ "reportId")
      .write[UUID]
      .and((JsPath \ "messages").write[Seq[Risk]])
      .and((JsPath \ "nino").write[String])
      .and((JsPath \ "taxYear").write[String])
      .and((JsPath \ "calculationId").write[UUID])
      .and((JsPath \ "correlationId").write[String])(unlift(AssessmentReport.unapply))

}

case class Risk(title: String, body: String, action: String, links: Seq[Link], path: String)

object Risk {

  implicit val writes: Writes[Risk] =
    (JsPath \ "title")
      .write[String]
      .and((JsPath \ "body").write[String])
      .and((JsPath \ "action").write[String])
      .and((JsPath \ "links").write[Seq[Link]])
      .and((JsPath \ "path").write[String])(unlift(Risk.unapply))

}

case class Link(title: String, url: String)

object Link {

  implicit val writes: Writes[Link] =
    (JsPath \ "title")
      .write[String]
      .and((JsPath \ "url").write[String])(unlift(Link.unapply))

}
