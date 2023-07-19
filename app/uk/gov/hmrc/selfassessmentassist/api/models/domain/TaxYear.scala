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

package uk.gov.hmrc.selfassessmentassist.api.models.domain

/** Opaque representation of a tax year
  */
final case class TaxYear private (private val value: String) {

  /** The tax year in RDS format, e.g. for String "2023-24" this will be Int 2024.
    */
  val asRds: Int = value.toInt

  /** The tax year in MTD (vendor-facing) format, e.g. "2023-24".
    */
  val asMtd: String = {
    val prefix  = value.take(2)
    val yearTwo = value.drop(2)
    val yearOne = (yearTwo.toInt - 1).toString
    prefix + yearOne + "-" + yearTwo
  }

  /** The tax year in the FraudRisk (InsightService) format, e.g. "2024".
    */
  val asFraudRisk: String = value

  override def toString: String = s"TaxYear($value)"
}

object TaxYear {

  /** @param taxYear
    *   tax year in MTD format (e.g. 2017-18)
    */
  def fromMtd(taxYear: String): TaxYear =
    new TaxYear(taxYear.take(2) + taxYear.drop(5))

}
