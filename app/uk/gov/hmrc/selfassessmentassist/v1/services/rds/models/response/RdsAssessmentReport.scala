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

package uk.gov.hmrc.selfassessmentassist.v1.services.rds.models.response

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json._
import uk.gov.hmrc.selfassessmentassist.v1.services.rds.models.response.RdsAssessmentReport.{KeyValueWrapper, KeyValueWrapperInt, Output}

import java.util.UUID

case class RdsAssessmentReport(links: Seq[String],
                               version: Int,
                               moduleId: String,
                               stepId: String,
                               executionState: String,
                               outputs: Seq[Output]
                                 ) {

  def calculationId: Option[UUID] = outputs.collectFirst {
    case KeyValueWrapper("calculationId", value) => value.map(UUID.fromString)
  }.flatten

  def rdsCorrelationId: Option[String] = outputs.collectFirst {
      case KeyValueWrapper("correlationId", value) => value
    }.flatten

  def feedbackId: Option[UUID] = outputs.collectFirst {
    case KeyValueWrapper("feedbackId", value) => value.map(UUID.fromString)
  }.flatten

  def responseCode: Option[Int] = outputs.collectFirst {
    case KeyValueWrapperInt("responseCode", value) => value
  }

  def responseMessage: Option[String] = outputs.collectFirst {
    case KeyValueWrapper("responseMessage", value) => value
  }.flatten

  def calculationTimestamp: Option[String] = outputs.collectFirst {
    case KeyValueWrapper("calculationTimestamp", value) => value
  }.flatten

}

object RdsAssessmentReport {

  sealed trait Output

  object Output {
    val specialKeysWithIntValues: Seq[String] = List("responseCode")
    val specialKeys: Seq[String] = List("correlationId","feedbackId","calculationId","nino","taxYear","response",
      "responseMessage","Created_dttm","createdDttm","calculationTimestamp","rt_Record_Contacts_Outer")
    implicit val reads: Reads[Output] = {
      case json@JsObject(fields) =>
        fields.keys.toSeq match {
          case Seq("name", "value") if specialKeysWithIntValues.contains(json("name").asInstanceOf[JsString].value) =>
            KeyValueWrapperInt.reads.reads(json)
          case Seq("name", "value") if specialKeys.contains(json("name").asInstanceOf[JsString].value) =>
              KeyValueWrapper.reads.reads(json)
          case _ =>
              MainOutputWrapper.reads.reads(json)
        }
      case _ => throw new IllegalThreadStateException("Output malformed")
    }

    implicit val writes: Writes[Output] = {
      case o@MainOutputWrapper(_, _) => MainOutputWrapper.writes.writes(o)
      case o@IdentifiersWrapper(_) => IdentifiersWrapper.writes.writes(o)
      case o@KeyValueWrapper(_, _) => KeyValueWrapper.writes.writes(o)
      case o@KeyValueWrapperInt(_, _) => KeyValueWrapperInt.writes.writes(o)
    }
  }

  case class KeyValueWrapperInt(name:String,value:Int) extends Output
  object KeyValueWrapperInt {

    val reads: Reads[KeyValueWrapperInt] =
      ((JsPath \ "name").read[String] and
        (JsPath \ "value").read[Int])(KeyValueWrapperInt.apply _)

    val writes: Writes[KeyValueWrapperInt] =
      (JsPath \ "name").write[String].and((JsPath \ "value").write[Int])(unlift(KeyValueWrapperInt.unapply))
  }

  case class KeyValueWrapper(name:String,value:Option[String]) extends Output
  object KeyValueWrapper {
   implicit  val reads: Reads[KeyValueWrapper] =
      ((JsPath \ "name").read[String] and
        (JsPath \ "value").readNullable[String])(KeyValueWrapper.apply _)

    implicit val writes: Writes[KeyValueWrapper] =
      ((JsPath \ "name").write[String] and
        (JsPath \ "value").writeNullable[String])(unlift(KeyValueWrapper.unapply))
  }

  case class MainOutputWrapper(name: String, value: Option[Seq[ObjectPart]]) extends Output

  object MainOutputWrapper {

    implicit val reads: Reads[MainOutputWrapper] =
      (JsPath \ "name").read[String]
        .and((JsPath \ "value").readNullable[Seq[ObjectPart]])(MainOutputWrapper.apply _)

    implicit val writes: Writes[MainOutputWrapper] =
      (JsPath \ "name").write[String]
        .and((JsPath \ "value").write[Option[Seq[ObjectPart]]])(unlift(MainOutputWrapper.unapply))

  }

  case class IdentifiersWrapper(identifiers: Seq[Identifier]) extends Output

  object IdentifiersWrapper {

    implicit val  reads: Reads[IdentifiersWrapper] =
      (JsPath \ "identifiers").read[Seq[Identifier]].map(IdentifiersWrapper.apply)

    implicit val writes: Writes[IdentifiersWrapper] =
      (JsPath \ "identifiers").write[Seq[Identifier]].contramap(_.identifiers)


  }

  sealed trait ObjectPart

  object ObjectPart {

    implicit val reads: Reads[ObjectPart] = {
      case json@JsObject(values) =>
        values.keys.toList match {
          case List("metadata") => MetadataWrapper.reads.reads(json)
          case List(_) => DataWrapper.reads.reads(json)
        }
      case _ => throw new IllegalThreadStateException("Object part malformed")
    }

    implicit val writes: Writes[ObjectPart] = {
      case o@MetadataWrapper(_) => MetadataWrapper.writes.writes(o)
      case o@DataWrapper(_) => DataWrapper.writes.writes(o)
    }
  }


  case class MetadataWrapper(metadata: Option[Seq[Map[String, String]]]) extends ObjectPart

  object MetadataWrapper {

   implicit  val reads: Reads[MetadataWrapper] =
      (JsPath \ "metadata").readNullable[Seq[Map[String, String]]].map(MetadataWrapper.apply)

    implicit val writes: Writes[MetadataWrapper] =
      (JsPath \ "metadata").write[Option[Seq[Map[String, String]]]].contramap(_.metadata)


  }

  case class DataWrapper(data: Option[Seq[Seq[String]]]) extends ObjectPart

  object DataWrapper {
    implicit val reads: Reads[DataWrapper] =
      (JsPath \ "data").readNullable[Seq[Seq[String]]].map(DataWrapper.apply)

    implicit val writes: Writes[DataWrapper] =
      (JsPath \ "data").write[Option[Seq[Seq[String]]]].contramap(_.data)
  }

  case class Identifier(name: String, value: String)

  object Identifier {
    implicit val reads: Reads[Identifier] =
      (JsPath \ "name").read[String]
        .and((JsPath \ "value").read[String])(Identifier.apply _)

    implicit val writes: Writes[Identifier] =
      (JsPath \ "name").write[String]
        .and((JsPath \ "value").write[String])(unlift(Identifier.unapply))
  }

  implicit val reads: Reads[RdsAssessmentReport] =
    (JsPath \ "links").read[Seq[String]]
      .and((JsPath \ "version").read[Int])
      .and((JsPath \ "moduleId").read[String])
      .and((JsPath \ "stepId").read[String])
      .and((JsPath \ "executionState").read[String])
      .and((JsPath \ "outputs").read[Seq[Output]])(RdsAssessmentReport.apply _)

  implicit val writes: Writes[RdsAssessmentReport] =
    (JsPath \ "links").write[Seq[String]]
      .and((JsPath \ "version").write[Int])
      .and((JsPath \ "moduleId").write[String])
      .and((JsPath \ "stepId").write[String])
      .and((JsPath \ "executionState").write[String])
      .and((JsPath \ "outputs").write[Seq[Output]])(unlift(RdsAssessmentReport.unapply))

}
