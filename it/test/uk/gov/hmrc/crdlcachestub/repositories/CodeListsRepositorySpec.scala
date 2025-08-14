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

import org.mongodb.scala.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{Assertion, OptionValues}
import play.api.libs.json.{JsNull, JsString, JsTrue, Json}
import uk.gov.hmrc.crdlcachestub.models.CodeListCode.{BC08, BC36, BC66}
import uk.gov.hmrc.crdlcachestub.models.CodeListEntry
import uk.gov.hmrc.mongo.test.{
  CleanMongoCollectionSupport,
  IndexedMongoQueriesSupport,
  PlayMongoRepositorySupport
}
import uk.gov.hmrc.mongo.transaction.TransactionConfiguration

import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}

class CodeListsRepositorySpec
  extends AnyFlatSpec
  with PlayMongoRepositorySupport[CodeListEntry]
  with CleanMongoCollectionSupport
  with IndexedMongoQueriesSupport
  with Matchers
  with OptionValues
  with ScalaFutures {

  given TransactionConfiguration = TransactionConfiguration.strict
  given ec: ExecutionContext     = ExecutionContext.global

  override protected val repository: CodeListsRepository = new CodeListsRepository(
    mongoComponent
  )

  override given patienceConfig: PatienceConfig =
    PatienceConfig(timeout = 30.seconds, interval = 100.millis)

  private val countryCodelistEntries = Seq(
    CodeListEntry(
      BC08,
      "AW",
      "Aruba",
      Json.obj(
        "actionIdentification" -> "811"
      )
    ),
    CodeListEntry(
      BC08,
      "BL",
      "Saint Barthélemy",
      Json.obj(
        "actionIdentification" -> "823"
      )
    ),
    CodeListEntry(
      BC08,
      "BM",
      "Bermuda",
      Json.obj(
        "actionIdentification" -> "824"
      )
    )
  )

  private val differentCodeListEntries = Seq(
    CodeListEntry(
      BC66,
      "B",
      "Beer",
      Json.obj(
        "actionIdentification" -> "1084"
      )
    )
  )

  def withCodeListEntries(
    entries: Seq[CodeListEntry]
  )(test: ClientSession => Future[Assertion]): Unit = {
    repository.collection.insertMany(entries).toFuture.futureValue
    repository.withSessionAndTransaction(test).futureValue
  }

  private val codelistEntries = countryCodelistEntries ++ differentCodeListEntries

  "CodeListsRepository.fetchEntries" should "return the codelist entries for a given codelist code" in withCodeListEntries(
    codelistEntries
  ) { _ =>
    repository
      .fetchEntries(
        BC08,
        filterKeys = None,
        filterProperties = None
      )
      .map(_ must contain allElementsOf countryCodelistEntries)
  }

  it should "apply filtering of entries according to the supplied keys" in withCodeListEntries(
    codelistEntries
  ) { _ =>
    repository
      .fetchEntries(
        BC08,
        filterKeys = Some(Set("AW", "BL")),
        filterProperties = None
      )
      .map(_ must contain allElementsOf countryCodelistEntries.take(2))
  }

  it should "not apply filtering of entries when the set of supplied keys is empty" in withCodeListEntries(
    codelistEntries
  ) { _ =>
    repository
      .fetchEntries(
        BC08,
        filterKeys = Some(Set.empty),
        filterProperties = None
      )
      .map(_ must contain allElementsOf countryCodelistEntries)
  }

  it should "not return entries from other lists" in withCodeListEntries(codelistEntries) { _ =>
    repository
      .fetchEntries(
        BC08,
        filterKeys = None,
        filterProperties = None
      )
      .map(_ must contain noElementsOf differentCodeListEntries)
  }

  it should "not return entries from other lists even when matching keys are specified" in withCodeListEntries(
    codelistEntries
  ) { _ =>
    repository
      .fetchEntries(
        BC08,
        filterKeys = Some(Set("B")),
        filterProperties = None
      )
      .map(_ must contain noElementsOf differentCodeListEntries)
  }

  private val exciseProductEntries = Seq(
    CodeListEntry(
      BC36,
      "B000",
      "Beer",
      Json.obj(
        "exciseProductsCategoryCode"         -> "B",
        "unitOfMeasureCode"                  -> "3",
        "alcoholicStrengthApplicabilityFlag" -> true,
        "degreePlatoApplicabilityFlag"       -> true,
        "densityApplicabilityFlag"           -> false,
        "responsibleDataManager"             -> null
      )
    ),
    CodeListEntry(
      BC36,
      "E200",
      "Vegetable and animal oils Products falling within CN codes 1507 to 1518, if these are intended for use a\ns heating fuel or motor fuel (Article 20(1)(a))",
      Json.obj(
        "exciseProductsCategoryCode"         -> "E",
        "unitOfMeasureCode"                  -> "2",
        "alcoholicStrengthApplicabilityFlag" -> false,
        "degreePlatoApplicabilityFlag"       -> false,
        "densityApplicabilityFlag"           -> true
      )
    ),
    CodeListEntry(
      BC36,
      "W200",
      "Still wine and still fermented beverages other than wine and beer",
      Json.obj(
        "exciseProductsCategoryCode"         -> "W",
        "unitOfMeasureCode"                  -> "3",
        "alcoholicStrengthApplicabilityFlag" -> true,
        "degreePlatoApplicabilityFlag"       -> false,
        "densityApplicabilityFlag"           -> false,
        "responsibleDataManager"             -> "ABC"
      )
    )
  )

  it should "apply filtering of entries using String properties" in withCodeListEntries(
    exciseProductEntries
  ) { _ =>
    repository
      .fetchEntries(
        BC36,
        filterKeys = Some(Set("B000", "W200")),
        filterProperties = Some(Map("exciseProductsCategoryCode" -> JsString("W")))
      )
      .map(_ must contain only exciseProductEntries.last)
  }

  it should "apply filtering of entries using Boolean properties" in withCodeListEntries(
    exciseProductEntries
  ) { _ =>
    for {
      allEntriesWithFlag <- repository
        .fetchEntries(
          BC36,
          filterKeys = None,
          filterProperties = Some(Map("alcoholicStrengthApplicabilityFlag" -> JsTrue))
        )

      filteredEntriesWithFlag <- repository
        .fetchEntries(
          BC36,
          filterKeys = Some(Set("B000", "W200")),
          filterProperties = Some(Map("degreePlatoApplicabilityFlag" -> JsTrue))
        )
    } yield {
      allEntriesWithFlag must contain allOf (exciseProductEntries.head, exciseProductEntries.last)
      filteredEntriesWithFlag must contain only exciseProductEntries.head
    }
  }

  it should "apply filtering of entries using null or missing values" in withCodeListEntries(
    exciseProductEntries
  ) { _ =>
    for {
      allEntriesWithNull <- repository
        .fetchEntries(
          BC36,
          filterKeys = None,
          filterProperties = Some(Map("responsibleDataManager" -> JsNull))
        )

      filteredEntriesWithNull <- repository
        .fetchEntries(
          BC36,
          filterKeys = Some(Set("B000", "W200")),
          filterProperties = Some(Map("responsibleDataManager" -> JsNull))
        )
    } yield {
      allEntriesWithNull must contain allElementsOf exciseProductEntries.init
      filteredEntriesWithNull must contain only exciseProductEntries.head
    }
  }
}
