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

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.{JsonFraming, StreamConverters}
import play.api.Logging
import play.api.libs.json.{JsResult, Json}
import uk.gov.hmrc.crdlcachestub.models.CustomsOffice
import uk.gov.hmrc.crdlcachestub.models.formats.MongoFormats
import uk.gov.hmrc.crdlcachestub.repositories.CustomsOfficeListsRepository
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.transaction.{TransactionConfiguration, Transactions}

import javax.inject.Singleton
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ImportCustomsOfficesMigration @Inject() (
  val mongoComponent: MongoComponent,
  repository: CustomsOfficeListsRepository,
  resourceProvider: ResourceProvider
)(using system: ActorSystem)
  extends Transactions
  with Logging
  with MongoFormats {

  given ExecutionContext = system.dispatcher

  given TransactionConfiguration = TransactionConfiguration.strict

  val migrationComplete: Future[Unit] =
    withSessionAndTransaction { session =>
      val importResult = for {
        _ <- repository.deleteOffices(session)

        _ <- StreamConverters
          .fromInputStream(resourceProvider.getResource("/data/customsOffices.json"))
          .via(JsonFraming.objectScanner(Int.MaxValue))
          .map(bs => Json.parse(bs.toArray))
          .map(json => Json.fromJson[CustomsOffice](json))
          .mapAsync(1)(result => Future.fromTry(JsResult.toTry(result)))
          .grouped(100)
          .mapAsync(1)(offices => repository.saveOffices(session, offices))
          .run()
      } yield ()

      importResult.foreach { _ =>
        logger.info(s"import-offices job completed successfully")
      }

      importResult.failed.foreach { err =>
        logger.error(s"import-offices job failed due to error", err)
      }

      importResult
    }
}
