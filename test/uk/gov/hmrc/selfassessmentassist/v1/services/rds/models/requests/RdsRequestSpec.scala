package uk.gov.hmrc.selfassessmentassist.v1.services.rds.models.requests

import play.api.libs.json.{JsObject, Json, JsSuccess}
import uk.gov.hmrc.selfassessmentassist.support.UnitSpec
import uk.gov.hmrc.selfassessmentassist.v1.services.rds.models.request.RdsRequest
import uk.gov.hmrc.selfassessmentassist.v1.services.rds.models.request.RdsRequest.{DataWrapper, Input, InputWithBoolean, InputWithInt, InputWithObject, InputWithString, MetadataWrapper, ObjectPart}
import uk.gov.hmrc.selfassessmentassist.v1.services.rds.RdsTestData.fraudRiskReport

class RdsRequestSpec extends UnitSpec {

  "InputWithString" should {
    val calculationId = "f2fb30e5-4ab6-4a29-b3c1-c00000000001"
    val inputWithStringJson: JsObject = Json.obj(
      "name"  -> "calculationId",
      "value" -> calculationId
    )

    val inputWithStringObject: InputWithString = InputWithString("calculationId", calculationId)

    "write to json" in {
      InputWithString.writes.writes(inputWithStringObject) shouldBe inputWithStringJson
    }

    "read to object" in {
      InputWithString.reads.reads(inputWithStringJson).map(_ shouldBe inputWithStringObject)
    }

    "throw an exception for malformed JSON" in {
      val json = Json.obj(
        "invalid" -> "value"
      )

      val exception = intercept[IllegalStateException] {
        Input.reads.reads(json)
      }

      exception.getMessage shouldBe "Input malformed"
    }

    "throw an exception with correct error message when JSON is not an object" in {
      val json = Json.arr("value")

      val exception = intercept[IllegalStateException] {
        Input.reads.reads(json)
      }

      exception.getMessage shouldBe "Input malformed"
    }
  }

  "InputWithInt" should {
    val inputWithIntJson: JsObject = Json.obj(
      "name"  -> "fraudRiskReportScore",
      "value" -> 10
    )

    val inputWithIntObject: InputWithInt = InputWithInt("fraudRiskReportScore", fraudRiskReport.score)

    "write to json" in {
      InputWithInt.writes.writes(inputWithIntObject) shouldBe inputWithIntJson
    }

    "read to object" in {
      InputWithString.reads.reads(inputWithIntJson).map(_ shouldBe inputWithIntObject)
    }
  }

  "InputWithObject" should {

    val inputWithObject = InputWithObject(
      "fraudRiskReportHeaders",
      Seq(
        MetadataWrapper(
          Seq(
            Map("KEY"   -> "string"),
            Map("VALUE" -> "string")
          )),
        DataWrapper(Seq(Seq.empty))
      ))

    val inputWithObjectJson =
      "{\"name\":\"fraudRiskReportHeaders\",\"value\":[{\"metadata\":[{\"KEY\":\"string\"},{\"VALUE\":\"string\"}]},{\"data\":[[]]}]}"

    "write to json" in {
      InputWithObject.writes.writes(inputWithObject).toString() shouldBe inputWithObjectJson
    }

    "read to object" in {
      InputWithObject.reads.reads(Json.toJson(inputWithObjectJson)).map(_ shouldBe inputWithObject)
    }

    "return an error when the json is malformed" in {
      val faultyInputWithObjectJson = Json.obj(
        "name"  -> "fraudRiskReportHeaders",
        "value" -> Json.arr("metadata" -> Json.arr("KEY" -> "string", "VALUE" -> "string"), "data" -> Json.arr(Json.arr())))

      val exception = intercept[IllegalStateException] {
        InputWithObject.reads.reads(Json.toJson(faultyInputWithObjectJson))
      }

      exception.getMessage should be("Object part malformed")
    }
  }

  "ObjectPart" should {
    "deserialize valid MetadataWrapper JSON" in {
      val json = Json.obj(
        "metadata" -> Seq(
          Map("key1" -> "value1"),
          Map("key2" -> "value2")
        )
      )

      val result = ObjectPart.reads.reads(json)

      result.map(
        _ shouldBe MetadataWrapper(
          Seq(
            Map("key1" -> "value1"),
            Map("key2" -> "value2")
          )))
    }

    "deserialize valid DataWrapper JSON" in {
      val json = Json.obj(
        "data" -> Seq(
          Seq("value1", "value2"),
          Seq("value3", "value4")
        )
      )

      val result = ObjectPart.reads.reads(json)

      result.map(
        _ shouldBe DataWrapper(
          Seq(
            Seq("value1", "value2"),
            Seq("value3", "value4")
          )))
    }

    "throw an exception for malformed JSON" in {
      val json = Json.obj(
        "invalid" -> "value"
      )

      an[IllegalStateException] should be thrownBy {
        Json.fromJson[ObjectPart](json)
      }
    }
  }

  "InputWithBoolean" should {
    val inputWithBooleanJson: JsObject = Json.obj(
      "name"  -> "input",
      "value" -> true
    )

    val inputWithBooleanObject: InputWithBoolean = InputWithBoolean("input", value = true)

    "write to json" in {
      InputWithBoolean.writes.writes(inputWithBooleanObject) shouldBe inputWithBooleanJson
    }

    "read to object" in {
      InputWithBoolean.reads.reads(inputWithBooleanJson).map(_ shouldBe inputWithBooleanObject)
    }
  }

  "MetadataWrapper" should {
    val metadataWrapperJson = Json.obj(
      "metadata" -> Json.arr(
        Json.obj(
          "name"  -> "input",
          "value" -> "true"
        )
      )
    )

    val metadataWrapperObject: MetadataWrapper = MetadataWrapper(Seq(Map("name" -> "input", "value" -> "true")))

    "write to json" in {
      MetadataWrapper.writes.writes(metadataWrapperObject) shouldBe metadataWrapperJson
    }

    "read to object" in {
      MetadataWrapper.reads.reads(metadataWrapperJson).map(_ shouldBe metadataWrapperObject)
    }
  }

  "DataWrapper" should {
    val dataWrapperJson = Json.obj(
      "data" -> Json.arr(
        Json.arr(
          "martin",
          "luther"
        )
      )
    )

    val dataWrapperObject: DataWrapper = DataWrapper(Seq(Seq("martin", "luther")))

    "write to json" in {
      DataWrapper.writes.writes(dataWrapperObject) shouldBe dataWrapperJson
    }

    "read to object" in {
      DataWrapper.reads.reads(dataWrapperJson).map(_ shouldBe dataWrapperObject)
    }
  }

  "RdsRequest" should {
    val rdsRequestJson = Json.obj(
      "inputs" -> Json.arr(
        Json.obj(
          "name"  -> "fraudRiskReportScore",
          "value" -> 10
        )
      )
    )

    val rdsRequestObject: RdsRequest = RdsRequest(inputs = Seq(InputWithInt("fraudRiskReportScore", fraudRiskReport.score)))

    "write to json" in {
      RdsRequest.writes.writes(rdsRequestObject) shouldBe rdsRequestJson
    }

    "read to object" in {
      RdsRequest.reads.reads(rdsRequestJson).map(_ shouldBe rdsRequestObject)
    }

    "handle multiple inputs" should {
      val rdsRequestJsonWithMultipleInputsJson = Json.obj(
        "inputs" -> Json.arr(
          Json.obj(
            "name"  -> "fraudRiskReportScore",
            "value" -> 10
          ),
          Json.obj(
            "name"  -> "input",
            "value" -> true
          )
        )
      )

      val rdsRequestObjectWithMultipleInputs: RdsRequest =
        RdsRequest(inputs = Seq(
          InputWithInt("fraudRiskReportScore", fraudRiskReport.score),
          InputWithBoolean("input", value = true)
        ))

      "write" in {
        RdsRequest.writes.writes(rdsRequestObjectWithMultipleInputs) shouldBe rdsRequestJsonWithMultipleInputsJson
      }

      "read" in {
        RdsRequest.reads.reads(rdsRequestJsonWithMultipleInputsJson).map(_ shouldBe rdsRequestObjectWithMultipleInputs)
      }
    }
  }

}
