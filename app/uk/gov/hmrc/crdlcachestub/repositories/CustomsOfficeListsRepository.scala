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

package uk.gov.hmrc.crdlcachestub.repositories

import org.mongodb.scala.ClientSession
import org.mongodb.scala.model.*
import org.mongodb.scala.model.Filters.*
import uk.gov.hmrc.crdlcachestub.models.CustomsOffice
import uk.gov.hmrc.crdlcachestub.models.formats.MongoFormats
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.transaction.Transactions

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CustomsOfficeListsRepository @Inject() (val mongoComponent: MongoComponent)(using
  ec: ExecutionContext
) extends PlayMongoRepository[CustomsOffice](
    mongoComponent,
    collectionName = "customsOfficeLists",
    domainFormat = MongoFormats.customsOfficeFormat,
    indexes = Seq(
      IndexModel(Indexes.ascending("referenceNumber", "activeFrom"), IndexOptions().unique(true)),
      IndexModel(
        Indexes.ascending("countryCode", "referenceNumber", "activeFrom")
      ),
      IndexModel(
        Indexes.ascending(
          "customsOfficeTimetable.customsOfficeTimetableLine.customsOfficeRoleTrafficCompetence.roleName",
          "referenceNumber",
          "activeFrom"
        )
      )
    )
  )
  with Transactions {

  def fetchCustomsOfficeLists(
    referenceNumbers: Option[Set[String]],
    countryCodes: Option[Set[String]],
    roles: Option[Set[String]]
  ): Future[Seq[CustomsOffice]] = {
    val referenceNumberFilters = referenceNumbers
      .map(referenceNumbers =>
        if referenceNumbers.nonEmpty then List(in("referenceNumber", referenceNumbers.toSeq*))
        else List.empty
      )
      .getOrElse(List.empty)

    val countryCodeFilters = countryCodes
      .map(countryCodes =>
        if countryCodes.nonEmpty then List(in("countryCode", countryCodes.toSeq*)) else List.empty
      )
      .getOrElse(List.empty)

    val roleFilters = roles
      .map(roles =>
        if roles.nonEmpty then
          List(
            in(
              "customsOfficeTimetable.customsOfficeTimetableLine.customsOfficeRoleTrafficCompetence.roleName",
              roles.toSeq*
            )
          )
        else List.empty
      )
      .getOrElse(List.empty)

    val allFilters = referenceNumberFilters ++ countryCodeFilters ++ roleFilters
    collection
      .find(if (allFilters.nonEmpty) and(allFilters*) else empty())
      .toFuture()
  }

  def deleteOffices(session: ClientSession): Future[Unit] =
    collection
      .deleteMany(session, empty())
      .toFuture()
      .map(_ => ())

  def saveOffices(session: ClientSession, offices: Seq[CustomsOffice]): Future[Unit] =
    collection
      .insertMany(session, offices)
      .toFuture()
      .map(_ => ())
}
