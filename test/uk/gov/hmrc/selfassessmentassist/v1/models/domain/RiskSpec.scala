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

import uk.gov.hmrc.selfassessmentassist.api.models.domain.PreferredLanguage
import uk.gov.hmrc.selfassessmentassist.api.models.domain.PreferredLanguage.PreferredLanguage
import uk.gov.hmrc.selfassessmentassist.support.UnitSpec
import uk.gov.hmrc.selfassessmentassist.v1.models.response.rds.RdsAssessmentReport

class RiskSpec extends UnitSpec {

  "Risk" when {
    "created from RDS report outputs" when {

      def rdsOutputWrapper(name: String, data: RdsAssessmentReport.DataWrapper): RdsAssessmentReport.MainOutputWrapper = {
        val rdsMetadata = RdsAssessmentReport.MetadataWrapper(Some(Seq(Map("ignoredKey" -> "ignoredValue"))))

        RdsAssessmentReport.MainOutputWrapper(name, Some(Seq(rdsMetadata, data)))
      }

      def rdsDataWrapper(risks: Seq[String]*): RdsAssessmentReport.DataWrapper =
        RdsAssessmentReport.DataWrapper(Some(Seq(risks*)))

      def risksFromRdsReportOutputs(preferredLanguage: PreferredLanguage, reportOutputs: RdsAssessmentReport.Output*) =
        Risk.risksFromRdsReportOutputs(reportOutputs, preferredLanguage)

      def risk(values: String*) = Seq(values*)

      val riskEnglish = risk("message_en", "action_en", "title_en", "[link title_en]", "[linkUrl_en]", "path_en")
      val riskWelsh   = risk("message_cy", "action_cy", "title_cy", "[link title_cy]", "[linkUrl_cy]", "path_cy")

      "English is preferred and present" must {
        "pick out the English values" in {
          risksFromRdsReportOutputs(
            PreferredLanguage.English,
            rdsOutputWrapper("EnglishActions", rdsDataWrapper(riskEnglish)),
            rdsOutputWrapper("WelshActions", rdsDataWrapper(riskWelsh))
          ) shouldBe
            Seq(Risk("title_en", "message_en", "action_en", Seq(Link("link title_en", "linkUrl_en")), "path_en"))
        }
      }

      "Welsh is preferred and present" must {
        "pick out the Welsh values" in {
          risksFromRdsReportOutputs(
            PreferredLanguage.Welsh,
            rdsOutputWrapper("EnglishActions", rdsDataWrapper(riskEnglish)),
            rdsOutputWrapper("WelshActions", rdsDataWrapper(riskWelsh))
          ) shouldBe
            Seq(Risk("title_cy", "message_cy", "action_cy", Seq(Link("link title_cy", "linkUrl_cy")), "path_cy"))
        }
      }

      "there are multiple risks for the preferred language" must {
        "return multiple risks" in {
          risksFromRdsReportOutputs(
            PreferredLanguage.English,
            rdsOutputWrapper(
              "EnglishActions",
              rdsDataWrapper(
                risk("message1_en", "action1_en", "title1_en", "[linkTitle1_en]", "[linkUrl1_en]", "path1_en"),
                risk("message2_en", "action2_en", "title2_en", "[linkTitle2_en]", "[linkUrl2_en]", "path2_en")
              )
            )
          ) shouldBe
            Seq(
              Risk("title1_en", "message1_en", "action1_en", Seq(Link("linkTitle1_en", "linkUrl1_en")), "path1_en"),
              Risk("title2_en", "message2_en", "action2_en", Seq(Link("linkTitle2_en", "linkUrl2_en")), "path2_en")
            )
        }
      }

      "no actions are present" must {
        "return no risks" in {
          risksFromRdsReportOutputs(PreferredLanguage.English) shouldBe Seq.empty
        }
      }

      // Unclear whether this can happen, but for completeness:
      "no actions are present for the preferred language" must {
        "return no risks" in {
          risksFromRdsReportOutputs(
            PreferredLanguage.English,
            rdsOutputWrapper("WelshActions", rdsDataWrapper(riskWelsh))
          ) shouldBe Seq.empty
        }
      }

      "the value inside the MainOutputWrapper is None" must {
        "return no risks" in {
          risksFromRdsReportOutputs(
            PreferredLanguage.English,
            RdsAssessmentReport.MainOutputWrapper("EnglishActions", None)
          ) shouldBe Seq.empty
        }
      }

      "the DataWrapper within the MainOutputWrapper is None" must {
        "return no risks" in {
          risksFromRdsReportOutputs(
            PreferredLanguage.English,
            rdsOutputWrapper("EnglishActions", RdsAssessmentReport.DataWrapper(None))
          ) shouldBe Seq.empty
        }
      }

      "multiple HTTP links are present as comma-separated list" when {
        "the number of links equals the number of titles" must {
          "return a risk with multiple links each with the corresponding title" in {
            risksFromRdsReportOutputs(
              PreferredLanguage.English,
              rdsOutputWrapper(
                "EnglishActions",
                rdsDataWrapper(risk("message", "action", "title", "[linkTitle1, linkTitle2, linkTitle3]", "[linkUrl1, linkUrl2, linkUrl3]", "path"))
              )
            ) shouldBe
              Seq(
                Risk(
                  "title",
                  "message",
                  "action",
                  Seq(
                    Link("linkTitle1", "linkUrl1"),
                    Link("linkTitle2", "linkUrl2"),
                    Link("linkTitle3", "linkUrl3")
                  ),
                  "path"))
          }
        }
      }

    }

  }

}
