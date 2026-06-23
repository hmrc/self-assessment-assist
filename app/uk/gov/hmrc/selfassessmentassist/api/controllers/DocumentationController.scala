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

package uk.gov.hmrc.selfassessmentassist.api.controllers

import controllers.Assets
import org.apache.pekko.stream.Materializer
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import play.filters.cors.CORSActionBuilder
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.selfassessmentassist.definitions.ApiDefinitionFactory

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class DocumentationController @Inject() (
    apiDefinition: ApiDefinitionFactory,
    assets: Assets,
    configuration: Configuration,
    cc: ControllerComponents
)(implicit ec: ExecutionContext, materializer: Materializer)
    extends BackendController(cc) {

  def definition(): Action[AnyContent] = Action {
    Ok(Json.toJson(apiDefinition.definition))
  }

  def specification(version: String, file: String): Action[AnyContent] = {
    CORSActionBuilder(configuration).async { implicit request =>
      assets.at(s"/public/api/conf/$version", file)(request)
    }
  }

}
