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

package uk.gov.hmrc.selfassessmentassist.v1.models.response

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.selfassessmentassist.support.UnitSpec
import uk.gov.hmrc.selfassessmentassist.v1.models.response.rds.RdsAssessmentReport.{DataWrapper, Identifier, IdentifiersWrapper, KeyValueWrapper, MainOutputWrapper, MetadataWrapper}

class WrappersSpec extends UnitSpec {

  val identifiers: Seq[Identifier] = Seq(Identifier("some-name", "some-value"))

  val keyValueWrapper: KeyValueWrapper       = KeyValueWrapper("some-name", Some("some-value"))
  val identifiersWrapper: IdentifiersWrapper = IdentifiersWrapper(identifiers)
  val metadataWrapper: MetadataWrapper       = MetadataWrapper(Some(Seq(Map("name" -> "some-name", "value" -> "some-value"))))
  val dataWrapper: DataWrapper               = DataWrapper(Some(Seq(Seq("name", "value"))))

  val mainOutputWithMetadataWrapper: MainOutputWrapper = MainOutputWrapper("mainOutputWrapper", Some(Seq(metadataWrapper)))
  val mainOutputWithDataWrapper: MainOutputWrapper     = MainOutputWrapper("mainOutputWrapper", Some(Seq(dataWrapper)))

  val identifiersWrapperJson: JsValue = getWrapperJson("identifiers")

  val keyValuesJson: JsValue = Json.parse("""
    |{
    |"name": "some-name",
    |"value": "some-value"
    |}""".stripMargin)

  val mainOutputMetadataWrapperJson: JsValue = Json.parse("""{
      |"name": "mainOutputWrapper",
      |"value": [
      |  {
      |    "metadata":[
      |     {
      |       "name": "some-name",
      |       "value": "some-value"
      |     }
      |    ]
      |   }
      | ]
      |}
      |""".stripMargin)

  val mainOutputDataWrapperJson: JsValue = Json.parse(s"""{
      |"name": "mainOutputWrapper",
      |"value": [
      |  {
      |    "data":[   [ "name","value"]    ]
      |   }
      | ]
      |}
      |""".stripMargin)

  def getWrapperJson(wrapperType: String): JsValue =
    Json.parse(s"""{
        | "$wrapperType":[
        |  {
        |     "name": "some-name",
        |     "value": "some-value"
        |  }
        | ]
        |}
        |""".stripMargin)

  "IdentifiersWrapper" when {
    "written to JSON" must {
      "return the expected JSON body" in {
        Json.toJson(identifiersWrapper) shouldBe identifiersWrapperJson
      }
    }
    "read from JSON" must {
      "return the expected model" in {
        identifiersWrapperJson.as[IdentifiersWrapper] shouldBe identifiersWrapper
      }
    }
  }

  "KeyValueWrapper" when {
    "written to JSON" must {
      "return the expected JSON body" in {
        Json.toJson(keyValueWrapper) shouldBe keyValuesJson
      }
    }
    "read from JSON" must {
      "return the expected model" in {
        keyValuesJson.as[KeyValueWrapper] shouldBe keyValueWrapper
      }
    }
  }

  "MetadataWrapper" when {
    val metadataWrapperJson: JsValue = getWrapperJson("metadata")
    "written to JSON" must {
      "return the expected JSON body" in {
        Json.toJson(metadataWrapper) shouldBe metadataWrapperJson
      }
    }
    "read from JSON" must {
      "return the expected model" in {
        metadataWrapperJson.as[MetadataWrapper] shouldBe metadataWrapper
      }
    }
  }

  "DataWrapper" when {

    val dataWrapperJson: JsValue = Json.parse("""{"data":  [   ["name", "value"]  ]}""".stripMargin)
    "written to JSON" must {
      "return the expected JSON body" in {
        Json.toJson(dataWrapper) shouldBe dataWrapperJson
      }
    }

    "read from JSON" must {
      "return the expected model" in {
        dataWrapperJson.as[DataWrapper] shouldBe dataWrapper
      }
    }
  }

  "MainOutputWrapper with MetadataWrapper" when {
    "written to JSON" must {
      "return the expected JSON body" in {
        Json.toJson(mainOutputWithMetadataWrapper) shouldBe mainOutputMetadataWrapperJson
      }
    }

    "read from JSON" must {
      "return the expected model" in {
        mainOutputMetadataWrapperJson.as[MainOutputWrapper] shouldBe mainOutputWithMetadataWrapper
      }
    }
  }

  "MainOutputWrapper with DataWrapper" when {
    "written to JSON" must {
      "return the expected JSON body" in {
        Json.toJson(mainOutputWithDataWrapper) shouldBe mainOutputDataWrapperJson
      }
    }

    "read from JSON" must {
      "return the expected model" in {
        mainOutputDataWrapperJson.as[MainOutputWrapper] shouldBe mainOutputWithDataWrapper
      }
    }
  }

}
