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

package definitions

import uk.gov.hmrc.selfassessmentassist.definitions.APIStatus.ALPHA
import uk.gov.hmrc.selfassessmentassist.definitions.*
import play.api.libs.json.*
import uk.gov.hmrc.selfassessmentassist.support.*
import uk.gov.hmrc.selfassessmentassist.definitions.Versions.VERSION_1

class ApiDefinitionSpec extends UnitSpec {

  private val apiVersion: APIVersion       = APIVersion(VERSION_1, ALPHA, endpointsEnabled = false)
  private val apiDefinition: APIDefinition = APIDefinition("b", "c", "d", Seq("e"), Seq(apiVersion), Some(false))

  private val apiVersionJson: JsValue = Json.parse(
    """
      |{
      |  "version": "1.0",
      |  "status": "ALPHA",
      |  "endpointsEnabled": false
      |}
    """.stripMargin
  )

  private val apiDefinitionJson: JsValue = Json.parse(
    """
      |{
      |  "name": "b",
      |  "description": "c",
      |  "context": "d",
      |  "categories": [
      |    "e"
      |  ],
      |  "versions": [
      |    {
      |      "version": "1.0",
      |      "status": "ALPHA",
      |      "endpointsEnabled": false
      |    }
      |  ],
      |  "requiresTrust": false
      |}
    """.stripMargin
  )

  private val definitionJson = Json.parse(
    """
      |{
      |  "api": {
      |    "name": "b",
      |    "description": "c",
      |    "context": "d",
      |    "categories": [
      |      "e"
      |    ],
      |    "versions": [
      |      {
      |        "version": "1.0",
      |        "status": "ALPHA",
      |        "endpointsEnabled": false
      |      }
      |    ],
      |    "requiresTrust": false
      |  }
      |}
    """.stripMargin
  )

  "APIDefinition" when {
    "the 'name' parameter is empty" should {
      "throw an 'IllegalArgumentException'" in {
        assertThrows[IllegalArgumentException](
          apiDefinition.copy(name = "")
        )
      }
    }
  }

  "the 'description' parameter is empty" should {
    "throw an 'IllegalArgumentException'" in {
      assertThrows[IllegalArgumentException](
        apiDefinition.copy(description = "")
      )
    }
  }

  "the 'context' parameter is empty" should {
    "throw an 'IllegalArgumentException'" in {
      assertThrows[IllegalArgumentException](
        apiDefinition.copy(context = "")
      )
    }
  }

  "the 'categories' parameter is empty" should {
    "throw an 'IllegalArgumentException'" in {
      assertThrows[IllegalArgumentException](
        apiDefinition.copy(categories = Seq())
      )
    }
  }

  "the 'versions' parameter is not unique" should {
    "throw an 'IllegalArgumentException'" in {
      assertThrows[IllegalArgumentException](
        apiDefinition.copy(versions = Seq(apiVersion, apiVersion))
      )
    }
  }

  "the 'versions' parameter is empty" should {
    "throw an 'IllegalArgumentException'" in {
      assertThrows[IllegalArgumentException](
        apiDefinition.copy(versions = Seq())
      )
    }
  }

  "APIVersion" should {
    "deserialise to model" in {
      apiVersionJson.as[APIVersion] shouldBe apiVersion
    }

    "serialise to JSON" in {
      Json.toJson(apiVersion) shouldBe apiVersionJson
    }

    "error when JSON is invalid" in {
      JsObject.empty.validate[APIVersion] shouldBe a[JsError]
    }
  }

  "APIDefinition" should {
    "deserialise to model" in {
      apiDefinitionJson.as[APIDefinition] shouldBe apiDefinition
    }

    "serialise to JSON" in {
      Json.toJson(apiDefinition) shouldBe apiDefinitionJson
    }

    "error when JSON is invalid" in {
      JsObject.empty.validate[APIDefinition] shouldBe a[JsError]
    }
  }

  "Definition" should {
    val definition = Definition(apiDefinition)

    "deserialise to model" in {
      definitionJson.as[Definition] shouldBe definition
    }

    "serialise to JSON" in {
      Json.toJson(definition) shouldBe definitionJson
    }

    "error when JSON is invalid" in {
      JsObject.empty.validate[Definition] shouldBe a[JsError]
    }
  }

}
