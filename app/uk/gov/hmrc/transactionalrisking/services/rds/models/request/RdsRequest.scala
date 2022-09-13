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

package uk.gov.hmrc.transactionalrisking.services.rds.models.request

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json._
import uk.gov.hmrc.transactionalrisking.services.rds.models.request.RdsRequest.Input

import java.util.UUID

case class RdsRequest(inputs: Seq[Input]) {

  def calculationId: UUID =
    inputs.find(_.name == "calculationId").map(_.value.toString).map(UUID.fromString)
      .getOrElse(throw new RuntimeException("No 'calculationId' present."))

}

object RdsRequest {

  trait Input {
    def name: String
    def value: Any
  }

  object Input {
    implicit val reads: Reads[Input] = {
      case json@JsObject(values) =>
        values.get("value") match {
          case Some(JsString(_)) => InputWithString.reads.reads(json)
          case Some(JsNull) => InputWithString.reads.reads(json)
          case Some(JsNumber(_)) => InputWithInt.reads.reads(json)
          case Some(JsArray(_)) => InputWithObject.reads.reads(json)
          case Some(JsBoolean(_)) => InputWithBoolean.reads.reads(json)
        }
    }

    implicit val writes: Writes[Input] = {
      case i@InputWithString(_, _) => InputWithString.writes.writes(i)
      case i@InputWithInt(_, _) => InputWithInt.writes.writes(i)
      case i@InputWithObject(_, _) => InputWithObject.writes.writes(i)
    }

  }

  case class InputWithString(name: String, value: String) extends Input

  object InputWithString {

    val reads: Reads[InputWithString] =
      (JsPath \ "name").read[String]
        .and((JsPath \ "value").readWithDefault[String](null))(InputWithString.apply _)

    val writes: Writes[InputWithString] =
      (JsPath \ "name").write[String]
        .and((JsPath \ "value").write[String])(unlift(InputWithString.unapply))

  }

  case class InputWithInt(name: String, value: Int) extends Input

  object InputWithInt {

    val reads: Reads[InputWithInt] =
      (JsPath \ "name").read[String]
        .and((JsPath \ "value").read[Int])(InputWithInt.apply _)

    val writes: Writes[InputWithInt] =
      (JsPath \ "name").write[String]
        .and((JsPath \ "value").write[Int])(unlift(InputWithInt.unapply))

  }

  case class InputWithObject(name: String, value: Seq[ObjectPart]) extends Input

  object InputWithObject {

    val reads: Reads[InputWithObject] =
      (JsPath \ "name").read[String]
        .and((JsPath \ "value").read[Seq[ObjectPart]])(InputWithObject.apply _)

    val writes: Writes[InputWithObject] =
      (JsPath \ "name").write[String]
        .and((JsPath \ "value").write[Seq[ObjectPart]])(unlift(InputWithObject.unapply))

  }

  case class InputWithBoolean(name: String, value: Boolean) extends Input

  object InputWithBoolean {

    val reads: Reads[InputWithBoolean] =
      (JsPath \ "name").read[String]
        .and((JsPath \ "value").readWithDefault[Boolean](false))(InputWithBoolean.apply _)

    val writes: Writes[InputWithBoolean] =
      (JsPath \ "name").write[String]
        .and((JsPath \ "value").write[Boolean])(unlift(InputWithBoolean.unapply))

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

  implicit val reads: Reads[RdsRequest] =
    (JsPath \ "inputs").read[Seq[Input]].map(RdsRequest.apply)

  implicit val writes: Writes[RdsRequest] =
    (JsPath \ "inputs").write[Seq[Input]].contramap(_.inputs)

}