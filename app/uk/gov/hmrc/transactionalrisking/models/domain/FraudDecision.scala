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

package uk.gov.hmrc.transactionalrisking.models.domain

import play.api.libs.json.{Format, Json}
//TODO revisit me later
object FraudDecision extends Enumeration {

  type FraudRiskDecision = Value

  val Accept: FraudDecision.Value = Value("A")
  val Check: FraudDecision.Value = Value("C")
  val Reject: FraudDecision.Value = Value("R")

  implicit val format: Format[FraudRiskDecision] = Json.formatEnum(FraudDecision)

}

