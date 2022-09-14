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
import play.api.libs.json.{JsObject, JsPath, Reads, Writes}
import NewRdsAssessmentReport.{IdentifiersWrapper, Output}

import java.util.UUID

//TODO Revisit this taken from Simulator
case class NewRdsAssessmentReport(links: Seq[String],
                                  version: Int,
                                  moduleId: String,
                                  stepId: String,
                                  executionState: String,
                                  outputs: Seq[Output]
                                 ) {

  def calculationId: UUID =
    outputs
      .find(_.isInstanceOf[IdentifiersWrapper])
      .map(_.asInstanceOf[IdentifiersWrapper])
      .map(_.identifiers.find(_.name == "calculationID").head)
      .map(_.value)
      .map(UUID.fromString)
      .getOrElse(throw new RuntimeException("No 'calculationId' present."))

//  def feedbackId: UUID =
//    outputs
//      .find(_.isInstanceOf[IdentifiersWrapper])
//      .map(_.asInstanceOf[IdentifiersWrapper])
//      .map(_.identifiers.find(_.name == "feedbackID").head)
//      .map(_.value)
//      .map(UUID.fromString)
//      .getOrElse(throw new RuntimeException("No 'feedbackID' present."))

}

object NewRdsAssessmentReport {

  trait Output

  object Output {

    implicit val reads: Reads[Output] = {
      case json@JsObject(fields) =>
        fields.keys.toSeq match {
          case Seq("name", "value") => MainOutputWrapper.reads.reads(json)
          case Seq("identifiers") => IdentifiersWrapper.reads.reads(json)
        }
    }

    implicit val writes: Writes[Output] = {
      case o@MainOutputWrapper(_, _) => MainOutputWrapper.writes.writes(o)
      case o@IdentifiersWrapper(_) => IdentifiersWrapper.writes.writes(o)

    }

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

  implicit val reads: Reads[NewRdsAssessmentReport] =
    (JsPath \ "links").read[Seq[String]]
      .and((JsPath \ "version").read[Int])
      .and((JsPath \ "moduleId").read[String])
      .and((JsPath \ "stepId").read[String])
      .and((JsPath \ "executionState").read[String])
      .and((JsPath \ "outputs").read[Seq[Output]])(NewRdsAssessmentReport.apply _)

  implicit val writes: Writes[NewRdsAssessmentReport] =
    (JsPath \ "links").write[Seq[String]]
      .and((JsPath \ "version").write[Int])
      .and((JsPath \ "moduleId").write[String])
      .and((JsPath \ "stepId").write[String])
      .and((JsPath \ "executionState").write[String])
      .and((JsPath \ "outputs").write[Seq[Output]])(unlift(NewRdsAssessmentReport.unapply))

}
