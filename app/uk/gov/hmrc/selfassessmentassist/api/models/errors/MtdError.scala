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

package uk.gov.hmrc.selfassessmentassist.api.models.errors

import play.api.http.Status._
import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{JsObject, JsPath, Json, OWrites}

case class MtdError(code: String, message: String, httpStatus: Int = BAD_REQUEST, paths: Option[Seq[String]] = None) {
  val asJson: JsObject = Json.toJson(this).as[JsObject]
}

object MtdError {

  implicit val writes: OWrites[MtdError] = (
    (JsPath \ "code").write[String] and
      (JsPath \ "message").write[String] and
      (JsPath \ "paths").writeNullable[Seq[String]]
    ) (unlift(MtdError.unapply))

  // excludes httpStatus
  def unapply(e: MtdError): Option[(String, String, Option[Seq[String]])] = Some((e.code, e.message, e.paths))

  implicit def genericWrites[T <: MtdError]: OWrites[T] =
    writes.contramap[T](c => c: MtdError)

}