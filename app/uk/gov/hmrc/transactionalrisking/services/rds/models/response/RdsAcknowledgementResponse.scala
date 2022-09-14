/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.transactionalrisking.services.rds.models.response

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json._
import uk.gov.hmrc.transactionalrisking.services.rds.models.response.RdsAcknowledgementResponse.AcknowledgementOutput


//TODO Revisit this error handling is not proper
case class RdsAcknowledgementResponse(links: Seq[String],
                                      version: Int,
                                      moduleId: String,
                                      stepId: String,
                                      executionState: String,
                                      outputs: Seq[AcknowledgementOutput]
                                 ) {

}
object RdsAcknowledgementResponse {

  trait AcknowledgementOutput

  object AcknowledgementOutput {

    implicit val reads: Reads[AcknowledgementOutput] = {
      case json@JsObject(fields) =>
        fields.keys.toSeq match {
          case Seq("name", "value") => AcknowledgementMainOutputWrapper.reads.reads(json)
        }
    }

    implicit val writes: Writes[AcknowledgementOutput] = {
      case o@AcknowledgementMainOutputWrapper(_, _) => AcknowledgementMainOutputWrapper.writes.writes(o)
    }

  }
  sealed trait DataValue
  object DataValue{

    implicit val dataValueReads: Reads[DataValue] = new Reads[DataValue] {
      override def reads(json: JsValue): JsResult[DataValue] = {
        json match {
          case JsString(s) => JsSuccess(DataValueString(s))
          case JsNumber(s) => JsSuccess(DataValueInt(s.toInt))
          case _ => throw new Exception("Invalid json value") // TODO Add better error handling
        }
      }
    }

    implicit val dataValueWrites: Writes[DataValue] = {
      case o@DataValueString(_) => DataValueString.writes.writes(o)
      case o@DataValueInt(_) => DataValueInt.writes.writes(o)
    }



  }
  case class DataValueString(s: String) extends DataValue
  object DataValueString  {
    val writes: Writes[DataValueString] =
      (JsPath).write[String].contramap(_.s)
  }
  case class DataValueInt(i: Int) extends DataValue
  object DataValueInt  {
    val writes: Writes[DataValueInt] =
      (JsPath).write[Int].contramap(_.i)
  }

  case class AcknowledgementMainOutputWrapper(name: String, value: DataValue) extends AcknowledgementOutput

  object AcknowledgementMainOutputWrapper {
//    val reads: Reads[AcknowledgementMainOutputWrapper] =
//      (JsPath \ "name").read[String]
//        .and((JsPath \ "value").read[String])(AcknowledgementMainOutputWrapper.apply _)
//
//    val writes: Writes[AcknowledgementMainOutputWrapper] =
//      (JsPath \ "name").write[String]
//        .and((JsPath \ "value").write[String])(unlift(AcknowledgementMainOutputWrapper.unapply))


    val reads: Reads[AcknowledgementMainOutputWrapper] = (
      (JsPath \ "key").read[String] and
        (JsPath \ "value").read[DataValue]
      )(AcknowledgementMainOutputWrapper.apply _)

    val writes: Writes[AcknowledgementMainOutputWrapper] =
      (JsPath \ "name").write[String]
        .and((JsPath \ "value").write[DataValue])(unlift(AcknowledgementMainOutputWrapper.unapply))
  }


/*  trait AcknowledgementObjectPart

  object AcknowledgementObjectPart {

    implicit val reads: Reads[AcknowledgementObjectPart] = {
      case json@JsObject(values) =>
        values.keys.toList match {
          case List("metadata") => AcknowledgementMetadataWrapper.reads.reads(json)
        }
    }

    implicit val writes: Writes[AcknowledgementObjectPart] = {
      case o@AcknowledgementMetadataWrapper(_) => AcknowledgementMetadataWrapper.writes.writes(o)
    }
  }


  case class AcknowledgementMetadataWrapper(metadata: Seq[Map[String, String]]) extends AcknowledgementObjectPart

  object AcknowledgementMetadataWrapper {

    val reads: Reads[AcknowledgementMetadataWrapper] =
      (JsPath \ "metadata").read[Seq[Map[String, String]]].map(AcknowledgementMetadataWrapper.apply)

    val writes: Writes[AcknowledgementMetadataWrapper] =
      (JsPath \ "metadata").write[Seq[Map[String, String]]].contramap(_.metadata)


  }*/

  implicit val reads: Reads[RdsAcknowledgementResponse] =
    (JsPath \ "links").read[Seq[String]]
      .and((JsPath \ "version").read[Int])
      .and((JsPath \ "moduleId").read[String])
      .and((JsPath \ "stepId").read[String])
      .and((JsPath \ "executionState").read[String])
      .and((JsPath \ "outputs").read[Seq[AcknowledgementOutput]])(RdsAcknowledgementResponse.apply _)

  implicit val writes: Writes[RdsAcknowledgementResponse] =
    (JsPath \ "links").write[Seq[String]]
      .and((JsPath \ "version").write[Int])
      .and((JsPath \ "moduleId").write[String])
      .and((JsPath \ "stepId").write[String])
      .and((JsPath \ "executionState").write[String])
      .and((JsPath \ "outputs").write[Seq[AcknowledgementOutput]])(unlift(RdsAcknowledgementResponse.unapply))

}

