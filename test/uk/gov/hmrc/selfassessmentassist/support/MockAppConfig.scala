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

package uk.gov.hmrc.selfassessmentassist.support

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import play.api.Configuration
import uk.gov.hmrc.selfassessmentassist.config.AppConfig

import scala.concurrent.duration.FiniteDuration

trait MockAppConfig extends MockFactory {

  val mockAppConfig: AppConfig = mock[AppConfig]

  object MockedAppConfig {

    // RDS config items
    def rdsBaseUrlForSubmit: CallHandler[String] = (mockAppConfig.rdsBaseUrlForSubmit _).expects().anyNumberOfTimes()
    def rdsBaseUrlForAcknowledge: CallHandler[String] = (mockAppConfig.rdsBaseUrlForAcknowledge _).expects().anyNumberOfTimes()
    def rdsAuthRequiredForThisEnv: CallHandler[Boolean] = (mockAppConfig.rdsAuthRequiredForThisEnv _).expects().anyNumberOfTimes()
    def cipFraudServiceBaseUrl:CallHandler[String] = (mockAppConfig.cipFraudServiceBaseUrl _).expects().anyNumberOfTimes()
    //API Config
    def featureSwitch: CallHandler[Option[Configuration]] = (mockAppConfig.featureSwitch _: () => Option[Configuration]).expects()

    // NRS config items
    def nrsApiKey: CallHandler[String] = (mockAppConfig.nrsApiKey _).expects().anyNumberOfTimes()
    def appName: CallHandler[String] = (mockAppConfig.appName _).expects().anyNumberOfTimes()
    def nrsBaseUrl: CallHandler[String] = (mockAppConfig.nrsBaseUrl _).expects().anyNumberOfTimes()
    def nrsRetries: CallHandler[List[FiniteDuration]] = (mockAppConfig.nrsRetries _).expects().anyNumberOfTimes()

    // IFS config items
    def ifsBaseUrl: CallHandler[String] = (mockAppConfig.ifsBaseUrl _).expects().anyNumberOfTimes()
    def ifsToken: CallHandler[String] = (mockAppConfig.ifsToken _).expects().anyNumberOfTimes()
  }
}
