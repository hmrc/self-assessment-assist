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

package uk.gov.hmrc.selfassessmentassist.v1.models.domain

import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.selfassessmentassist.api.models.domain.PreferredLanguage
import uk.gov.hmrc.selfassessmentassist.api.models.domain.PreferredLanguage.PreferredLanguage
import uk.gov.hmrc.selfassessmentassist.v1.models.response.rds.RdsAssessmentReport

case class Risk(title: String, body: String, action: String, links: Seq[Link], path: String)

object Risk {

  implicit val writes: Writes[Risk] = Json.writes

  private object RdsFieldIndexes {
    val body       = 0
    val action     = 1
    val title      = 2
    val linkTitles = 3
    val linkUrls   = 4
    val path       = 5
  }

  def risksFromRdsReportOutputs(rdsOutputs: Seq[RdsAssessmentReport.Output], preferredLanguage: PreferredLanguage): Seq[Risk] =
    rdsOutputs
      .collect { case output: RdsAssessmentReport.MainOutputWrapper if isPreferredLanguage(output.name, preferredLanguage) => output }
      .flatMap(_.value.getOrElse(Seq.empty))
      .collect { case value: RdsAssessmentReport.DataWrapper => value }
      .flatMap(_.data.getOrElse(Seq.empty))
      .flatMap(Risk.fromRdsFields)

  private def isPreferredLanguage(language: String, preferredLanguage: PreferredLanguage) =
    preferredLanguage match {
      case PreferredLanguage.English => language == "EnglishActions"
      case PreferredLanguage.Welsh   => language == "WelshActions"
      case _                         => false
    }

  private def fromRdsFields(riskParts: Seq[String]): Option[Risk] = {
    if (riskParts.isEmpty) None
    else {
      import RdsFieldIndexes._
      Some(
        Risk(
          title = riskParts(title),
          body = riskParts(body),
          action = riskParts(action),
          links = linksFromRdsFields(riskParts),
          path = riskParts(path)
        ))
    }
  }

  private def linksFromRdsFields(riskParts: Seq[String]): Seq[Link] = {
    def asSeq(value: String) =
      value.stripPrefix("[").stripSuffix("]").split(",").map(_.trim).toSeq

    import RdsFieldIndexes._
    val titles = asSeq(riskParts(linkTitles))
    val urls   = asSeq(riskParts(linkUrls))

    (titles zip urls).map { case (title, url) => Link(title, url) }
  }

}
