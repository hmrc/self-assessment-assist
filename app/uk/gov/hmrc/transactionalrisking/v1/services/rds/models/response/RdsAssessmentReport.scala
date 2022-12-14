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

package uk.gov.hmrc.transactionalrisking.v1.services.rds.models.response

import play.api.libs.functional.syntax.{toAlternativeOps, toFunctionalBuilderOps, unlift}
import play.api.libs.json._
import uk.gov.hmrc.transactionalrisking.v1.services.rds.models.response.RdsAssessmentReport.{KeyValueWrapper, Output}

import java.util.UUID

case class RdsAssessmentReport(links: Seq[String],
                               version: Int,
                               moduleId: String,
                               stepId: String,
                               executionState: String,
                               outputs: Seq[Output]
                                 ) {

  //we read calculationid and compare it with the one we passed it.TRDT-697

  def calculationId: Option[UUID] = outputs.collectFirst {
    case KeyValueWrapper("calculationId", value) => UUID.fromString(value)
  }

  def rdsCorrelationId: Option[String] = outputs.collectFirst {
      case KeyValueWrapper("correlationId", value) => value
    }

  def feedbackId: Option[UUID] = outputs.collectFirst {
    case KeyValueWrapper("feedbackId", value) => UUID.fromString(value)
  }

  def responseCode: Option[Int] = outputs.collectFirst {
    case KeyValueWrapper("responseCode", value) => value.toInt
  }

  def responseMessage: Option[String] = outputs.collectFirst {
    case KeyValueWrapper("responseMessage", value) => value
  }


}

object RdsAssessmentReport {

  trait Output

  object Output {
    val specialKeys = List("correlationId","feedbackId","calculationId","nino","taxYear","responseCode","response","responseMessage","Created_dttm","createdDttm")
    implicit val reads: Reads[Output] = {
      case json@JsObject(fields) =>
        fields.keys.toSeq match {
          case Seq("name", "value") if (specialKeys.contains(json("name").asInstanceOf[JsString].value)) =>
              KeyValueWrapper.reads.reads(json)
          case _ =>
              MainOutputWrapper.reads.reads(json)
        }
    }

    implicit val writes: Writes[Output] = {
      case o@MainOutputWrapper(_, _) => MainOutputWrapper.writes.writes(o)
      case o@IdentifiersWrapper(_) => IdentifiersWrapper.writes.writes(o)

    }
  }

  case class KeyValueWrapper(name:String,value:String) extends Output
  object KeyValueWrapper {
    val convertIntToString: Reads[String] = implicitly[Reads[Int]]
      .map(x => x.toString)

    val reads: Reads[KeyValueWrapper] =
      ((JsPath \ "name").read[String] and
        ((JsPath \ "value").read[String] or
          (JsPath \ "value").read[String](convertIntToString)))(KeyValueWrapper.apply _)

    val writes: Writes[KeyValueWrapper] =
      (JsPath \ "name").write[String]
        .and((JsPath \ "value").write[String])(unlift(KeyValueWrapper.unapply))
  }

  case class MainOutputWrapper(name: String, value: Seq[ObjectPart]) extends Output

  object MainOutputWrapper {

    val reads: Reads[MainOutputWrapper] =
      (JsPath \ "name").read[String]
        .and((JsPath \ "value").read[Seq[ObjectPart]])(MainOutputWrapper.apply _)

    val writes: Writes[MainOutputWrapper] =
      (JsPath \ "name").write[String]
        .and((JsPath \ "value").write[Seq[ObjectPart]])(unlift(MainOutputWrapper.unapply))

  }

  case class IdentifiersWrapper(identifiers: Seq[Identifier]) extends Output

  object IdentifiersWrapper {

    val reads: Reads[IdentifiersWrapper] =
      (JsPath \ "identifiers").read[Seq[Identifier]].map(IdentifiersWrapper.apply)

    val writes: Writes[IdentifiersWrapper] =
      (JsPath \ "identifiers").write[Seq[Identifier]].contramap(_.identifiers)


  }

  trait ObjectPart

  object ObjectPart {

    implicit val reads: Reads[ObjectPart] = {
      case json@JsObject(values) =>
        values.keys.toList match {
          case List("metadata") => MetadataWrapper.reads.reads(json)
          case List("data") => DataWrapper.reads.reads(json)
        }
    }

    implicit val writes: Writes[ObjectPart] = {
      case o@MetadataWrapper(_) => MetadataWrapper.writes.writes(o)
      case o@DataWrapper(_) => DataWrapper.writes.writes(o)
    }
  }


  case class MetadataWrapper(metadata: Seq[Map[String, String]]) extends ObjectPart

  object MetadataWrapper {

    val reads: Reads[MetadataWrapper] =
      (JsPath \ "metadata").read[Seq[Map[String, String]]].map(MetadataWrapper.apply)

    val writes: Writes[MetadataWrapper] =
      (JsPath \ "metadata").write[Seq[Map[String, String]]].contramap(_.metadata)


  }

  case class DataWrapper(data: Seq[Seq[String]]) extends ObjectPart

  object DataWrapper {
    val reads: Reads[DataWrapper] =
      (JsPath \ "data").read[Seq[Seq[String]]].map(DataWrapper.apply)

    val writes: Writes[DataWrapper] =
      (JsPath \ "data").write[Seq[Seq[String]]].contramap(_.data)
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
