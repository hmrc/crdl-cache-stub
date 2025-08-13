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
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.crdlcachestub.models.{CustomsOffice, CustomsOfficeDetail, CustomsOfficeTimetable, RoleTrafficCompetence, TimetableLine}
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, IndexedMongoQueriesSupport, PlayMongoRepositorySupport}
import uk.gov.hmrc.mongo.transaction.TransactionConfiguration
import org.scalatest.Assertion
import org.mongodb.scala.*

import java.time.{DayOfWeek, LocalDate, LocalTime}
import java.time.format.DateTimeFormatter
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}

class CustomsOfficeListsRepositorySpec  extends AnyFlatSpec
  with PlayMongoRepositorySupport[CustomsOffice]
  with CleanMongoCollectionSupport
  with IndexedMongoQueriesSupport
  with Matchers
  with OptionValues
  with ScalaFutures {
  given TransactionConfiguration = TransactionConfiguration.strict

  given ec: ExecutionContext = ExecutionContext.global

  override protected val repository: CustomsOfficeListsRepository =
    new CustomsOfficeListsRepository(mongoComponent)

  override given patienceConfig: PatienceConfig =
    PatienceConfig(timeout = 30.seconds, interval = 100.millis)

  def withCustomsOfficeEntries(
                                offices: Seq[CustomsOffice]
                              )(test: ClientSession => Future[Assertion]): Unit = {
    repository.collection.insertMany(offices).toFuture.futureValue
    repository.withSessionAndTransaction(test).futureValue
  }

  protected val timeFormat: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME
  private val dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd")
  val DK003102 = CustomsOffice(
    "DK003102",
    None,
    None,
    Some("DK003102"),
    Some("DK003102"),
    None,
    "DK",
    Some("test@dk"),
    None,
    None,
    None,
    "9850",
    Some("+45 342234 34543"),
    None,
    None,
    None,
    None,
    false,
    None,
    None,
    List("SN0009"),
    CustomsOfficeDetail(
      "Hirtshals Toldekspedition",
      "DA",
      "Hirtshals",
      false,
      None,
      None,
      false,
      "Dalsagervej 7"
    ),
    List(
      CustomsOfficeTimetable(
        1,
        None,
        LocalDate.parse("20180101", dateFormat),
        LocalDate.parse("20991231", dateFormat),
        List(
          TimetableLine(
            DayOfWeek.of(1),
            LocalTime.parse("08:00", timeFormat),
            LocalTime.parse("16:00", timeFormat),
            DayOfWeek.of(5),
            None,
            None,
            List(
              RoleTrafficCompetence("EXL", "P"),
              RoleTrafficCompetence("EXL", "R"),
              RoleTrafficCompetence("EXP", "P"),
              RoleTrafficCompetence("EXP", "R"),
              RoleTrafficCompetence("EXT", "P"),
              RoleTrafficCompetence("EXT", "R"),
              RoleTrafficCompetence("PLA", "R"),
              RoleTrafficCompetence("RFC", "R"),
              RoleTrafficCompetence("DIS", "N/A"),
              RoleTrafficCompetence("IPR", "N/A"),
              RoleTrafficCompetence("ENQ", "P"),
              RoleTrafficCompetence("ENQ", "R"),
              RoleTrafficCompetence("ENQ", "N/A"),
              RoleTrafficCompetence("REC", "P"),
              RoleTrafficCompetence("REC", "R"),
              RoleTrafficCompetence("REC", "N/A")
            )
          )
        )
      )
    )
  )

  val invalidatedoffice = CustomsOffice(
    "IT314102",
    Some("ITP00023"),
    Some("IT314102"),
    Some("IT314102"),
    None,
    None,
    "IT",
    Some("testo@it"),
    None,
    None,
    None,
    "10043",
    Some("345 34234"),
    None,
    None,
    None,
    None,
    false,
    Some("IT"),
    Some("TIN"),
    List.empty,
    CustomsOfficeDetail(
      "ORBASSANO",
      "IT",
      "ORBASSANO (TO)",
      false,
      Some("A"),
      None,
      true,
      "Prima Strada, 5"
    ),
    List(
      CustomsOfficeTimetable(
        1,
        Some("ALL YEAR"),
        LocalDate.parse("20180101", dateFormat),
        LocalDate.parse("99991231", dateFormat),
        List(
          TimetableLine(
            DayOfWeek.of(1),
            LocalTime.parse("08:00", timeFormat),
            LocalTime.parse("18:00", timeFormat),
            DayOfWeek.of(5),
            None,
            None,
            List(
              RoleTrafficCompetence("DEP", "R"),
              RoleTrafficCompetence("INC", "R"),
              RoleTrafficCompetence("TRA", "R"),
              RoleTrafficCompetence("EXP", "R"),
              RoleTrafficCompetence("EIN", "R"),
              RoleTrafficCompetence("ENT", "R"),
              RoleTrafficCompetence("EXC", "R"),
              RoleTrafficCompetence("DES", "R"),
              RoleTrafficCompetence("GUA", "R"),
              RoleTrafficCompetence("EXT", "R"),
              RoleTrafficCompetence("REG", "R"),
              RoleTrafficCompetence("REC", "R"),
              RoleTrafficCompetence("IPR", "N/A"),
              RoleTrafficCompetence("ENQ", "N/A")
            )
          )
        )
      )
    )
  )

  val newOffice = CustomsOffice(
    "IT223101",
    Some("ITP00002"),
    Some("IT223101"),
    Some("IT223101"),
    None,
    None,
    "IT",
    Some("test@it"),
    None,
    Some(LocalDate.parse("20250501", dateFormat)),
    None,
    "40131",
    Some("1234 045483382"),
    Some("2343 34543"),
    None,
    Some("Q"),
    None,
    false,
    Some("IT"),
    Some("TIN"),
    List.empty,
    CustomsOfficeDetail(
      "AEROPORTO DI BOLOGNA",
      "IT",
      "BOLOGNA",
      false,
      Some("A"),
      None,
      true,
      "VIA DELL'AEROPORTO, 1"
    ),
    List(CustomsOfficeTimetable(
      1,
      Some("ALL YEAR"),
      LocalDate.parse("20180101", dateFormat),
      LocalDate.parse("20991231", dateFormat),
      List(
        TimetableLine(
          DayOfWeek.of(1),
          LocalTime.parse("00:00", timeFormat),
          LocalTime.parse("23:59", timeFormat),
          DayOfWeek.of(6),
          None,
          None,
          List(
            RoleTrafficCompetence("DEP", "AIR"),
            RoleTrafficCompetence("INC", "AIR"),
            RoleTrafficCompetence("TXT", "AIR"),
            RoleTrafficCompetence("DES", "AIR"),
            RoleTrafficCompetence("ENQ", "N/A"),
            RoleTrafficCompetence("ENT", "AIR"),
            RoleTrafficCompetence("EXC", "N/A"),
            RoleTrafficCompetence("EXP", "AIR"),
            RoleTrafficCompetence("EXT", "AIR"),
            RoleTrafficCompetence("REC", "N/A"),
            RoleTrafficCompetence("REG", "N/A"),
            RoleTrafficCompetence("TRA", "AIR"),
            RoleTrafficCompetence("EIN", "AIR"),
            RoleTrafficCompetence("PLA", "N/A"),
            RoleTrafficCompetence("DIS", "N/A"),
            RoleTrafficCompetence("RFC", "N/A"),
            RoleTrafficCompetence("IPR", "N/A")
          )
        )
      )
    )
    ))

  val officeWithACERole = newOffice.copy(
    referenceNumber =  "GB223101",
    customsOfficeTimetable = List(CustomsOfficeTimetable(
      1,
      Some("ALL YEAR"),
      LocalDate.parse("20180101", dateFormat),
      LocalDate.parse("20991231", dateFormat),
      List(
        TimetableLine(
          DayOfWeek.of(1),
          LocalTime.parse("00:00", timeFormat),
          LocalTime.parse("23:59", timeFormat),
          DayOfWeek.of(6),
          None,
          None,
          List(
            RoleTrafficCompetence("ACE", "AIR"),
          )
        )
      )
    )
    )
  )

  private val customsOffices = Seq(DK003102, invalidatedoffice, newOffice)
  "CustomsOfficeLists.fetchCustomsOfficeLists" should "return all the customs offices" in withCustomsOfficeEntries(
    customsOffices
  ) { _ =>
    repository
      .fetchCustomsOfficeLists(
        referenceNumbers = None,
        countryCodes = None,
        roles = None
      )
      .map(_ must contain allElementsOf customsOffices)
  }

  it should "apply filtering of offices according to the supplied referenceNumbers" in withCustomsOfficeEntries(
    customsOffices
  ) { _ =>
    repository
      .fetchCustomsOfficeLists(
        referenceNumbers = Some(Set("IT223101")),
        countryCodes = None,
        roles = None
      )
      .map(_ mustBe List(newOffice))
  }

  it should "apply filtering of offices according to the supplied countryCodes" in withCustomsOfficeEntries(
    customsOffices
  ) { _ =>
    repository
      .fetchCustomsOfficeLists(
        referenceNumbers = None,
        countryCodes = Some(Set("DK")),
        roles = None,
      )
      .map(_ must contain(DK003102))
  }

  it should "apply filtering of offices according to the supplied roles" in withCustomsOfficeEntries(
    customsOffices :+ officeWithACERole
  ) { _ =>
    repository
      .fetchCustomsOfficeLists(
        referenceNumbers = None,
        countryCodes = None,
        roles = Some(Set("EXL")),
      )
      .map(_ mustBe List(DK003102))
  }

  it should "not apply filtering of referenceNumbers, countries and roles when the set of supplied referenceNumbers, countries and roles is empty" in withCustomsOfficeEntries(
    customsOffices
  ) { _ =>
    repository
      .fetchCustomsOfficeLists(
        referenceNumbers = Some(Set.empty),
        countryCodes = Some(Set.empty),
        roles = Some(Set.empty)
      )
      .map(_ must contain allElementsOf List(DK003102, newOffice))
  }

  it should "not return other offices even when matching referenceNumbers are specified" in withCustomsOfficeEntries(
    customsOffices
  ) { _ =>
    repository
      .fetchCustomsOfficeLists(
        referenceNumbers = Some(Set("IT223101")),
        countryCodes = None,
        roles = None
      )
      .map(_ mustNot contain(DK003102))
  }

  it should "not return other offices even when matching countryCodes are specified" in withCustomsOfficeEntries(
    customsOffices
  ) { _ =>
    repository
      .fetchCustomsOfficeLists(
        referenceNumbers = None,
        countryCodes = Some(Set("IT")),
        roles = None
      )
      .map(_ mustNot contain(DK003102))
  }

  it should "not return other offices even when matching roles are specified" in withCustomsOfficeEntries(
    customsOffices
  ) { _ =>
    repository
      .fetchCustomsOfficeLists(
        referenceNumbers = None,
        countryCodes = None,
        roles = Some(Set("EIN"))
      )
      .map(_ mustNot contain(DK003102))
  }

}
