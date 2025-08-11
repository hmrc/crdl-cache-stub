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

package uk.gov.hmrc.crdlcachestub.repositories.migration

import javax.inject.Singleton
import uk.gov.hmrc.crdlcachestub.repositories.CodeListsRepository
import uk.gov.hmrc.crdlcachestub.models.CodeListEntry
import org.apache.pekko.stream.scaladsl.StreamConverters
import org.apache.pekko.stream.scaladsl.JsonFraming
import javax.inject.Inject
import org.apache.pekko.stream.Materializer
import uk.gov.hmrc.mongo.transaction.Transactions
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.transaction.TransactionConfiguration
import org.apache.pekko.actor.ActorSystem
import scala.concurrent.ExecutionContext
import play.api.libs.json.Json

@Singleton
class ImportCodeListsMigration @Inject() (
  val mongoComponent: MongoComponent,
  repository: CodeListsRepository
)(using system: ActorSystem)
  extends Transactions {
  given ExecutionContext         = system.dispatcher
  given TransactionConfiguration = TransactionConfiguration.strict

  val migrationComplete = withSessionAndTransaction { session =>
    for {
      _ <- repository.deleteEntries(session)

      _ <- StreamConverters
        .fromInputStream(() => getClass.getResourceAsStream("/data/codelists.json"))
        .via(JsonFraming.objectScanner(Int.MaxValue))
        .map(bs => Json.parse(bs.toArray))
        .map(json => Json.fromJson[CodeListEntry](json).get)
        .grouped(100)
        .mapAsync(1)(entries => repository.saveEntries(session, entries))
        .run()
    } yield ()
  }
}
