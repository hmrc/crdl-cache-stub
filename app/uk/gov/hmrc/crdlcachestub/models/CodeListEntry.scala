/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.crdlcachestub.models

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

case class CodeListEntry(
  codeListCode: CodeListCode,
  key: String,
  value: String,
  properties: JsObject
)

object CodeListEntry {
  // Only serialize the key, value and properties in JSON responses
  given Writes[CodeListEntry] = (
    (JsPath \ "key").write[String] and
      (JsPath \ "value").write[String] and
      (JsPath \ "properties").write[JsObject]
  )(entry => (entry.key, entry.value, entry.properties))

  given Reads[CodeListEntry] = Json.reads[CodeListEntry]

  // Serialize the full object in MongoDB
  val mongoFormat: Format[CodeListEntry] =
    Json.format[CodeListEntry]
}
