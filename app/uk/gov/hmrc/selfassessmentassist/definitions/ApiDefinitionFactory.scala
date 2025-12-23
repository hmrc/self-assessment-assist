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

package uk.gov.hmrc.selfassessmentassist.definitions

import uk.gov.hmrc.selfassessmentassist.config.AppConfig
import uk.gov.hmrc.selfassessmentassist.definitions.Versions.*
import uk.gov.hmrc.selfassessmentassist.utils.Logging

import javax.inject.{Inject, Singleton}

@Singleton
class ApiDefinitionFactory @Inject() (appConfig: AppConfig) extends Logging {

  lazy val definition: Definition =
    Definition(
      api = APIDefinition(
        name = "Self Assessment Assist (MTD)",
        description = "Allows you to read your self assessment report and acknowledge it.",
        context = appConfig.apiGatewayContext,
        categories = Seq("INCOME_TAX_MTD"),
        versions = Seq(
          APIVersion(
            version = VERSION_1,
            status = buildAPIStatus(VERSION_1),
            endpointsEnabled = appConfig.endpointsEnabled(version = "1")
          )
        ),
        requiresTrust = None
      )
    )

  def buildAPIStatus(version: String): APIStatus = {
    lazy val apiStatus = appConfig.apiStatus(version)
    APIStatus.parser
      .lift(apiStatus)
      .find(_.toString == apiStatus)
      .getOrElse {
        logger.error(s"[ApiDefinition][buildApiStatus] no API Status found in config.  Reverting to Alpha")
        APIStatus.ALPHA
      }
  }

}
