/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.selfassessmentassist.v1.models.response

import play.api.libs.json.Json
import uk.gov.hmrc.selfassessmentassist.support.UnitSpec
import uk.gov.hmrc.selfassessmentassist.v1.models.response.rds.RdsAssessmentReport.{ObjectPart, MetadataWrapper, DataWrapper}

class ObjectPartSpec extends UnitSpec {

  "ObjectPart Reads" should {

    "read MetadataWrapper when metadata is present" in {
      val json = Json.parse("""{ "metadata": [ { "a": "b" } ] }""")

      json.as[ObjectPart] shouldBe
        MetadataWrapper(Some(Seq(Map("a" -> "b"))))
    }

    "read DataWrapper when data is present" in {
      val json = Json.parse("""{ "data": [ ["a", "b"], ["c"] ] }""")

      json.as[ObjectPart] shouldBe
        DataWrapper(Some(Seq(Seq("a", "b"), Seq("c"))))
    }
  }

  "ObjectPart Writes" should {

    "write MetadataWrapper correctly" in {
      val obj: ObjectPart =
        MetadataWrapper(Some(Seq(Map("a" -> "b"))))

      Json.toJson(obj) shouldBe
        Json.parse("""{ "metadata": [ { "a": "b" } ] }""")
    }

    "write DataWrapper correctly" in {
      val obj: ObjectPart =
        DataWrapper(Some(Seq(Seq("a", "b"))))

      Json.toJson(obj) shouldBe
        Json.parse("""{ "data": [ ["a", "b"] ] }""")
    }
  }

}
