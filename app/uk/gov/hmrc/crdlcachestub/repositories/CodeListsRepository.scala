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
import org.mongodb.scala.model.*
import org.mongodb.scala.model.Filters.*
import play.api.libs.json.*
import uk.gov.hmrc.crdlcachestub.models.{CodeListCode, CodeListEntry}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.mongo.transaction.Transactions

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CodeListsRepository @Inject() (val mongoComponent: MongoComponent)(using
  ec: ExecutionContext
) extends PlayMongoRepository[CodeListEntry](
    mongoComponent,
    collectionName = "codelists",
    domainFormat = CodeListEntry.mongoFormat,
    extraCodecs =
      Codecs.playFormatSumCodecs[JsValue](Format(Reads.JsValueReads, Writes.jsValueWrites)) ++
        Codecs.playFormatSumCodecs[JsBoolean](Format(Reads.JsBooleanReads, Writes.jsValueWrites)),
    indexes = Seq(
      IndexModel(
        Indexes.ascending("codeListCode", "key", "value", "activeFrom"),
        IndexOptions().unique(true)
      )
    )
  )
  with Transactions {

  def deleteEntries(session: ClientSession) =
    collection.deleteMany(session, empty()).toFuture()

  def saveEntries(session: ClientSession, entries: Seq[CodeListEntry]) =
    collection.insertMany(session, entries).toFuture()

  def fetchEntries(
    code: CodeListCode,
    filterKeys: Option[Set[String]],
    filterProperties: Option[Map[String, JsValue]]
  ): Future[Seq[CodeListEntry]] = {
    val mandatoryFilters = List(equal("codeListCode", code.code))

    val keyFilters = filterKeys
      .map(ks => if ks.nonEmpty then List(in("key", ks.toSeq*)) else List.empty)
      .getOrElse(List.empty)

    val propertyFilters = filterProperties
      .map { props =>
        if props.nonEmpty
        then props.map((k, v) => equal(s"properties.$k", v))
        else List.empty
      }
      .getOrElse(List.empty)

    val allFilters = mandatoryFilters ++ keyFilters ++ propertyFilters

    collection
      .find(and(allFilters*))
      .sort(Sorts.ascending("key"))
      .toFuture()
  }
}
