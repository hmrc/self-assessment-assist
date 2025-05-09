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

package definitions

import uk.gov.hmrc.selfassessmentassist.definitions.APIStatus.{ALPHA, BETA}
import uk.gov.hmrc.selfassessmentassist.definitions.ApiDefinitionFactory
import uk.gov.hmrc.selfassessmentassist.definitions.Versions.VERSION_1
import uk.gov.hmrc.selfassessmentassist.support._

class ApiDefinitionFactorySpec extends UnitSpec with MockAppConfig {

  class Test {
    val factory = new ApiDefinitionFactory(mockAppConfig)
  }

  "buildAPIStatus" when {
    val anyVersion = VERSION_1
    "the 'apiStatus' parameter is present and valid" should {
      "return the correct status" in new Test {
        MockedAppConfig.apiStatus(anyVersion) returns "BETA"
        factory.buildAPIStatus(version = anyVersion) shouldBe BETA
      }
    }

    "the 'apiStatus' parameter is present and invalid" should {
      "default to alpha" in new Test {
        MockedAppConfig.apiStatus(anyVersion) returns "ALPHO"
        factory.buildAPIStatus(version = anyVersion) shouldBe ALPHA
      }
    }
  }

}
