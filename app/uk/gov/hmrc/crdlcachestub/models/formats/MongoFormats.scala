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

package uk.gov.hmrc.crdlcachestub.models.formats

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.crdlcachestub.models.CodeListCode
import uk.gov.hmrc.crdlcachestub.models.{
  CodeListEntry,
  CustomsOffice,
  CustomsOfficeDetail,
  CustomsOfficeTimetable,
  RoleTrafficCompetence,
  TimetableLine
}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

trait MongoFormats extends MongoJavatimeFormats.Implicits with JavaTimeFormats {

  given codeListEntryFormat: Format[CodeListEntry] = Json.format[CodeListEntry]

  given customsOfficeFormat: Format[CustomsOffice] = Json.format[CustomsOffice]

  given Format[CustomsOfficeDetail] = Json.format[CustomsOfficeDetail]

  given Format[CustomsOfficeTimetable] = Json.format[CustomsOfficeTimetable]

  given Format[TimetableLine] = Json.format[TimetableLine]

  given Format[RoleTrafficCompetence] = Json.format[RoleTrafficCompetence]
}

object MongoFormats extends MongoFormats
