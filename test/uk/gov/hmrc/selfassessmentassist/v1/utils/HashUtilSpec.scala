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

package uk.gov.hmrc.selfassessmentassist.v1.utils

import uk.gov.hmrc.selfassessmentassist.support.UnitSpec
import uk.gov.hmrc.selfassessmentassist.utils.HashUtil

class HashUtilSpec extends UnitSpec {

  val hashUtil: HashUtil = app.injector.instanceOf[HashUtil]

  "HashUtil" when {
    "the encode method is called" must {
      "correctly encode a string using Base64 algorithm" in {
        val expectedValue  = "dGVzdA=="
        val stringToEncode = "test"

        val result = hashUtil.encode(stringToEncode)

        result shouldBe expectedValue
      }
    }

    "the getHash method is called" must {
      "correctly encode a string using SHA-256 algorithm" in {
        val expectedValue  = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"
        val stringToEncode = "test"

        val result = hashUtil.getHash(stringToEncode)

        result shouldBe expectedValue
      }
    }
  }

}
