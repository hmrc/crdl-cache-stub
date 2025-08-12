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

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import uk.gov.hmrc.crdlcachestub.repositories.CodeListsRepository
import org.mockito.Mockito.*
import org.apache.pekko.stream.scaladsl.{Source, Sink}
import org.mongodb.scala.{ClientSession, MongoClient, MongoDatabase, SingleObservable}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.apache.pekko.actor.ActorSystem
import uk.gov.hmrc.mongo.MongoComponent
import org.scalatest.BeforeAndAfterEach
import scala.concurrent.{ExecutionContext, Future}
import com.fasterxml.jackson.core.JsonParseException
import play.api.libs.json.JsResult

class ImportCodeListsMigrationSpec
  extends AsyncFlatSpec
  with Matchers
  with MockitoSugar
  with BeforeAndAfterEach {

  given system: ActorSystem = ActorSystem("test")
  given ExecutionContext    = system.dispatcher

  private val mongoComponent = mock[MongoComponent]
  private val mongoClient    = mock[MongoClient]
  private val mongoDatabase  = mock[MongoDatabase]
  private val clientSession  = mock[ClientSession]
  private val repository     = mock[CodeListsRepository]

  override def beforeEach() = {
    reset(
      mongoComponent,
      mongoClient,
      mongoDatabase,
      clientSession,
      repository
    )

    // Transactions
    when(mongoComponent.client).thenReturn(mongoClient)
    when(mongoComponent.database).thenReturn(mongoDatabase)
    when(mongoClient.startSession(any())).thenReturn(SingleObservable(clientSession))
    when(clientSession.commitTransaction())
      .thenAnswer(_ => Source.empty[Void].runWith(Sink.asPublisher(fanout = false)))
    when(clientSession.abortTransaction())
      .thenAnswer(_ => Source.empty[Void].runWith(Sink.asPublisher(fanout = false)))
  }

  "ImportCodeListsMigration" should "begin a migration when it is instantiated" in {
    when(repository.deleteEntries(eqTo(clientSession))).thenReturn(Future.unit)
    when(repository.saveEntries(eqTo(clientSession), any())).thenReturn(Future.unit)

    val migration = new ImportCodeListsMigration(
      mongoComponent,
      repository,
      ResourceProvider.FromClasspath
    )

    migration.migrationComplete.map { result =>
      verify(repository, times(1)).deleteEntries(eqTo(clientSession))
      verify(repository, times(19)).saveEntries(eqTo(clientSession), any())
      verify(clientSession, times(1)).commitTransaction()
      succeed
    }
  }

  it should "roll back when deleting entries fails" in {
    when(repository.deleteEntries(eqTo(clientSession)))
      .thenReturn(Future.failed(new RuntimeException("Error")))

    when(repository.saveEntries(eqTo(clientSession), any())).thenReturn(Future.unit)

    val migration = new ImportCodeListsMigration(
      mongoComponent,
      repository,
      ResourceProvider.FromClasspath
    )

    recoverToSucceededIf[RuntimeException](migration.migrationComplete).map { result =>
      verify(repository, times(1)).deleteEntries(eqTo(clientSession))
      verify(repository, never()).saveEntries(eqTo(clientSession), any())
      verify(clientSession, times(1)).abortTransaction()
      result
    }
  }

  it should "roll back when saving the first page of entries fails" in {
    when(repository.deleteEntries(eqTo(clientSession))).thenReturn(Future.unit)

    when(repository.saveEntries(eqTo(clientSession), any()))
      .thenReturn(Future.failed(new RuntimeException("Error")))

    val migration = new ImportCodeListsMigration(
      mongoComponent,
      repository,
      ResourceProvider.FromClasspath
    )

    recoverToSucceededIf[RuntimeException](migration.migrationComplete).map { result =>
      verify(repository, times(1)).deleteEntries(eqTo(clientSession))
      verify(repository, times(1)).saveEntries(eqTo(clientSession), any())
      verify(clientSession, times(1)).abortTransaction()
      result
    }
  }

  it should "roll back when saving later pages of entries fails" in {
    when(repository.deleteEntries(eqTo(clientSession))).thenReturn(Future.unit)

    when(repository.saveEntries(eqTo(clientSession), any()))
      .thenReturn(Future.unit)
      .thenReturn(Future.unit)
      .thenReturn(Future.failed(new RuntimeException("Error")))

    val migration = new ImportCodeListsMigration(
      mongoComponent,
      repository,
      ResourceProvider.FromClasspath
    )

    recoverToSucceededIf[RuntimeException](migration.migrationComplete).map { result =>
      verify(repository, times(1)).deleteEntries(eqTo(clientSession))
      verify(repository, times(3)).saveEntries(eqTo(clientSession), any())
      verify(clientSession, times(1)).abortTransaction()
      result
    }
  }

  it should "roll back when parsing the JSON fails" in {
    when(repository.deleteEntries(eqTo(clientSession))).thenReturn(Future.unit)

    val migration = new ImportCodeListsMigration(
      mongoComponent,
      repository,
      ResourceProvider.FromString("""{"Invalid JSON":}""")
    )

    recoverToSucceededIf[JsonParseException](migration.migrationComplete).map { result =>
      verify(repository, times(1)).deleteEntries(eqTo(clientSession))
      verify(repository, never()).saveEntries(eqTo(clientSession), any())
      verify(clientSession, times(1)).abortTransaction()
      result
    }
  }

  it should "roll back when the JSON does not represent valid codelist entries" in {
    when(repository.deleteEntries(eqTo(clientSession))).thenReturn(Future.unit)

    val migration = new ImportCodeListsMigration(
      mongoComponent,
      repository,
      ResourceProvider.FromString("""[{"this is":"not a codelist entry"}]""")
    )

    recoverToSucceededIf[JsResult.Exception](migration.migrationComplete).map { result =>
      verify(repository, times(1)).deleteEntries(eqTo(clientSession))
      verify(repository, never()).saveEntries(eqTo(clientSession), any())
      verify(clientSession, times(1)).abortTransaction()
      result
    }
  }

  it should "roll back when the resource cannot be found" in {
    when(repository.deleteEntries(eqTo(clientSession))).thenReturn(Future.unit)

    val migration = new ImportCodeListsMigration(
      mongoComponent,
      repository,
      new ResourceProvider { def getResource(name: String) = () => null }
    )

    recoverToSucceededIf[NullPointerException](migration.migrationComplete).map { result =>
      verify(repository, times(1)).deleteEntries(eqTo(clientSession))
      verify(repository, never()).saveEntries(eqTo(clientSession), any())
      verify(clientSession, times(1)).abortTransaction()
      result
    }
  }
}
