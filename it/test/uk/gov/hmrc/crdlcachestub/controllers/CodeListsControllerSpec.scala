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

package uk.gov.hmrc.crdlcachestub.controllers

import org.mockito.ArgumentMatchers.eq as equalTo
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.{HeaderNames, Status}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.*
import uk.gov.hmrc.crdlcachestub.models.CodeListCode.{BC08, BC36, BC66}
import uk.gov.hmrc.crdlcachestub.models.{CodeListCode, CodeListEntry}
import uk.gov.hmrc.crdlcachestub.repositories.CodeListsRepository
import uk.gov.hmrc.crdlcachestub.repositories.migration.ImportCodeListsMigration
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.test.HttpClientV2Support
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import scala.concurrent.{ExecutionContext, Future}

class CodeListsControllerSpec
  extends AnyFlatSpec
  with Matchers
  with MockitoSugar
  with ScalaFutures
  with IntegrationPatience
  with HttpClientV2Support
  with GuiceOneServerPerSuite
  with BeforeAndAfterEach {

  given ExecutionContext = ExecutionContext.global
  given HeaderCarrier    = HeaderCarrier()

  private val codeListsRepository = mock[CodeListsRepository]
  private val importCodeListsMigration = mock[ImportCodeListsMigration]

  private val codeListEntries = List(
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
    )
  )

  override def beforeEach(): Unit = {
    reset(codeListsRepository)
    reset(importCodeListsMigration)
    when(importCodeListsMigration.migrationComplete).thenReturn(Future.unit)
  }

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .overrides(
        bind[CodeListsRepository].toInstance(codeListsRepository),
        bind[ImportCodeListsMigration].toInstance(importCodeListsMigration),
        bind[HttpClientV2].toInstance(httpClientV2)
      )
      .build()

  "CodeListsController" should "return 200 OK when there are no errors" in {
    when(
      codeListsRepository.fetchEntries(
        equalTo(BC08),
        equalTo(None),
        equalTo(None)
      )
    )
      .thenReturn(Future.successful(codeListEntries))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${BC08.code}")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.json mustBe Json.arr(
      Json.obj(
        "key"        -> "AW",
        "value"      -> "Aruba",
        "properties" -> Json.obj("actionIdentification" -> "811")
      ),
      Json.obj(
        "key"        -> "BL",
        "value"      -> "Saint Barthélemy",
        "properties" -> Json.obj("actionIdentification" -> "823")
      )
    )

    response.status mustBe Status.OK
  }

  it should "return 200 OK when there are no entries to return" in {
    when(
      codeListsRepository.fetchEntries(
        equalTo(BC08),
        equalTo(None),
        equalTo(None)
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${BC08.code}")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.json mustBe Json.arr()
    response.status mustBe Status.OK
  }

  it should "parse comma-separated keys from the keys parameter when there is only one key" in {
    when(
      codeListsRepository.fetchEntries(
        equalTo(BC08),
        equalTo(Some(Set("GB"))),
        equalTo(None)
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${BC08.code}?keys=GB")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "parse comma-separated keys from the keys parameter when there are multiple keys" in {
    when(
      codeListsRepository.fetchEntries(
        equalTo(BC08),
        equalTo(Some(Set("GB", "XI"))),
        equalTo(None)
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${BC08.code}?keys=GB,XI")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "parse comma-separated keys when there are multiple declarations of the keys parameter" in {
    when(
      codeListsRepository.fetchEntries(
        equalTo(BC08),
        equalTo(Some(Set("GB", "XI", "AW", "BL"))),
        equalTo(None)
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${BC08.code}?keys=GB,XI&keys=AW,BL")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "parse comma-separated keys when there is no value declared for the keys parameter" in {
    when(
      codeListsRepository.fetchEntries(
        equalTo(BC08),
        equalTo(Some(Set.empty)),
        equalTo(None)
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${BC08.code}?keys=")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "parse other query parameters as boolean property filters when they are valid boolean values" in {
    when(
      codeListsRepository.fetchEntries(
        equalTo(BC36),
        equalTo(Some(Set("B000"))),
        equalTo(Some(Map("alcoholicStrengthApplicabilityFlag" -> JsBoolean(true))))
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(
          url"http://localhost:$port/crdl-cache/lists/${BC36.code}?keys=B000&alcoholicStrengthApplicabilityFlag=true"
        )
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "parse other query parameters as null property filters when the query parameter value is null" in {
    when(
      codeListsRepository.fetchEntries(
        equalTo(BC66),
        equalTo(Some(Set("B"))),
        equalTo(Some(Map("responsibleDataManager" -> JsNull)))
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(
          url"http://localhost:$port/crdl-cache/lists/${BC66.code}?keys=B&responsibleDataManager=null"
        )
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "parse other query parameters as String property filters when they are neither boolean nor null values" in {
    when(
      codeListsRepository.fetchEntries(
        equalTo(BC08),
        equalTo(Some(Set("GB"))),
        equalTo(Some(Map("actionIdentification" -> JsString("384"))))
      )
    )
      .thenReturn(Future.successful(List.empty))

    val response =
      httpClientV2
        .get(
          url"http://localhost:$port/crdl-cache/lists/${BC08.code}?keys=GB&actionIdentification=384"
        )
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.OK
  }

  it should "return 401 Unauthorized when the user provides no Authorization header" in {
    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/BC08")
        .execute[HttpResponse]
        .futureValue

    response.json mustBe Json.obj("statusCode" -> 401, "message" -> "Unauthorized")
    response.status mustBe Status.UNAUTHORIZED
  }

  it should "return 500 Internal Server Error when there is an error fetching from the repository" in {
    when(
      codeListsRepository.fetchEntries(
        equalTo(BC08),
        equalTo(None),
        equalTo(None)
      )
    )
      .thenReturn(Future.failed(new RuntimeException("Boom!!!")))

    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists/${BC08.code}")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.status mustBe Status.INTERNAL_SERVER_ERROR
  }

  "CodeListsController.fetchCodeListVersions" should "return 200 OK when there are no errors" ignore {
    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists")
        .setHeader(HeaderNames.AUTHORIZATION -> "some-auth-token")
        .execute[HttpResponse]
        .futureValue

    response.json mustBe Json.arr(
      Json.obj(
        "codeListCode"    -> "BC08",
        "snapshotVersion" -> 1,
        "lastUpdated"     -> "2025-06-29T00:00:00Z"
      ),
      Json.obj(
        "codeListCode"    -> "BC66",
        "snapshotVersion" -> 1,
        "lastUpdated"     -> "2025-06-28T00:00:00Z"
      )
    )

    response.status mustBe Status.OK
  }

  it should "return 401 Unauthorized when the user provides no Authorization header" in {
    val response =
      httpClientV2
        .get(url"http://localhost:$port/crdl-cache/lists")
        .execute[HttpResponse]
        .futureValue

    response.json mustBe Json.obj("statusCode" -> 401, "message" -> "Unauthorized")
    response.status mustBe Status.UNAUTHORIZED
  }
}
