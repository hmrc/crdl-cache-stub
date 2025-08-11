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

import play.api.libs.json.*

import java.time.LocalDate

case class CustomsOffice(
  referenceNumber: String,
  referenceNumberMainOffice: Option[String],
  referenceNumberHigherAuthority: Option[String],
  referenceNumberCompetentAuthorityOfEnquiry: Option[String],
  referenceNumberCompetentAuthorityOfRecovery: Option[String],
  referenceNumberTakeover: Option[String],
  countryCode: String,
  emailAddress: Option[String],
  unLocodeId: Option[String],
  nctsEntryDate: Option[LocalDate],
  nearestOffice: Option[String],
  postalCode: String,
  phoneNumber: Option[String],
  faxNumber: Option[String],
  telexNumber: Option[String],
  geoInfoCode: Option[String],
  regionCode: Option[String],
  traderDedicated: Boolean,
  dedicatedTraderLanguageCode: Option[String],
  dedicatedTraderName: Option[String],
  customsOfficeSpecificNotesCodes: List[String],
  customsOfficeLsd: CustomsOfficeDetail,
  customsOfficeTimetable: List[CustomsOfficeTimetable]
)

object CustomsOffice {
  given Format[CustomsOffice] = Json.format[CustomsOffice]

  val mongoFormat: Format[CustomsOffice] = { // Use the Mongo Extended JSON format for dates
    import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.Implicits.*
    Json.format[CustomsOffice]
  }
}
