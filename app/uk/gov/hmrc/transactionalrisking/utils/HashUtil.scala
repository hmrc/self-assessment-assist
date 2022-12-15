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

package uk.gov.hmrc.transactionalrisking.utils

import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils
import play.api.libs.json.{JsValue, Json}

import java.nio.charset.StandardCharsets
import javax.inject.{Inject, Singleton}

@Singleton
class HashUtil @Inject()() {

  def encode(value: String): String =
    Base64.encodeBase64String(value.getBytes(StandardCharsets.UTF_8))
  def decode(payload: String): JsValue =
    Json.parse(new String(Base64.decodeBase64(payload)))
  def getHash(value: String): String = DigestUtils.sha256Hex(value)

}

