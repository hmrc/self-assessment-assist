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

package uk.gov.hmrc.selfassessmentassist.config

import play.api.Configuration

case class FeatureSwitch(value: Option[Configuration]) {

  private val versionRegex = """(\d)\.\d""".r

  def isVersionEnabled(version: String): Boolean = {
    val versionNoIfPresent: Option[String] =
      version match {
        case versionRegex(v) => Some(v)
        case _               => None
      }

    val enabled = for {
      versionNo <- versionNoIfPresent
      config    <- value
      enabled   <- config.getOptional[Boolean](s"version-$versionNo.enabled")
    } yield enabled

    enabled.getOrElse(false)
  }

  def isEnabled(feature: Feature): Boolean = isEnabled(feature.name)

  def isEnabled(featureName: String): Boolean = {

    val enabled = for {
      config  <- value
      enabled <- config.getOptional[Boolean](s"${featureName}.enabled")
    } yield enabled

    enabled.getOrElse(false)
  }

  val supportingAgentsAccessControlEnabled: Boolean = isEnabled("supporting-agents-access-control")

}
