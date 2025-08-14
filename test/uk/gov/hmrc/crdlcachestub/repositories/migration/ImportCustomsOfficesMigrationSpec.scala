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

import com.fasterxml.jackson.core.JsonParseException
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.mockito.Mockito.{never, reset, times, verify, when}
import org.mongodb.scala.{ClientSession, MongoClient, MongoDatabase, SingleObservable}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsResult
import uk.gov.hmrc.crdlcachestub.repositories.CustomsOfficeListsRepository
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.{ExecutionContext, Future}

class ImportCustomsOfficesMigrationSpec
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
  private val repository     = mock[CustomsOfficeListsRepository]

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

  "ImportCustomsOfficesMigration" should "begin a migration when it is instantiated" in {
    when(repository.deleteOffices(eqTo(clientSession))).thenReturn(Future.unit)
    when(repository.saveOffices(eqTo(clientSession), any())).thenReturn(Future.unit)

    val migration = new ImportCustomsOfficesMigration(
      mongoComponent,
      repository,
      ResourceProvider.FromClasspath
    )

    migration.migrationComplete.map { result =>
      verify(repository, times(1)).deleteOffices(eqTo(clientSession))
      verify(repository, times(47)).saveOffices(eqTo(clientSession), any())
      verify(clientSession, times(1)).commitTransaction()
      succeed
    }
  }

  it should "roll back when deleting offices fails" in {
    when(repository.deleteOffices(eqTo(clientSession)))
      .thenReturn(Future.failed(new RuntimeException("Error")))

    when(repository.saveOffices(eqTo(clientSession), any())).thenReturn(Future.unit)

    val migration = new ImportCustomsOfficesMigration(
      mongoComponent,
      repository,
      ResourceProvider.FromClasspath
    )

    recoverToSucceededIf[RuntimeException](migration.migrationComplete).map { result =>
      verify(repository, times(1)).deleteOffices(eqTo(clientSession))
      verify(repository, never()).saveOffices(eqTo(clientSession), any())
      verify(clientSession, times(1)).abortTransaction()
      result
    }
  }

  it should "roll back when saving the first page of offices fails" in {
    when(repository.deleteOffices(eqTo(clientSession))).thenReturn(Future.unit)

    when(repository.saveOffices(eqTo(clientSession), any()))
      .thenReturn(Future.failed(new RuntimeException("Error")))

    val migration = new ImportCustomsOfficesMigration(
      mongoComponent,
      repository,
      ResourceProvider.FromClasspath
    )

    recoverToSucceededIf[RuntimeException](migration.migrationComplete).map { result =>
      verify(repository, times(1)).deleteOffices(eqTo(clientSession))
      verify(repository, times(1)).saveOffices(eqTo(clientSession), any())
      verify(clientSession, times(1)).abortTransaction()
      result
    }
  }

  it should "roll back when saving later pages of offices fails" in {
    when(repository.deleteOffices(eqTo(clientSession))).thenReturn(Future.unit)

    when(repository.saveOffices(eqTo(clientSession), any()))
      .thenReturn(Future.unit)
      .thenReturn(Future.unit)
      .thenReturn(Future.failed(new RuntimeException("Error")))

    val migration = new ImportCustomsOfficesMigration(
      mongoComponent,
      repository,
      ResourceProvider.FromClasspath
    )

    recoverToSucceededIf[RuntimeException](migration.migrationComplete).map { result =>
      verify(repository, times(1)).deleteOffices(eqTo(clientSession))
      verify(repository, times(3)).saveOffices(eqTo(clientSession), any())
      verify(clientSession, times(1)).abortTransaction()
      result
    }
  }

  it should "roll back when parsing the JSON fails" in {
    when(repository.deleteOffices(eqTo(clientSession))).thenReturn(Future.unit)

    val migration = new ImportCustomsOfficesMigration(
      mongoComponent,
      repository,
      ResourceProvider.FromString("""{"Invalid JSON":}""")
    )

    recoverToSucceededIf[JsonParseException](migration.migrationComplete).map { result =>
      verify(repository, times(1)).deleteOffices(eqTo(clientSession))
      verify(repository, never()).saveOffices(eqTo(clientSession), any())
      verify(clientSession, times(1)).abortTransaction()
      result
    }
  }

  it should "roll back when the JSON does not represent valid customs offices" in {
    when(repository.deleteOffices(eqTo(clientSession))).thenReturn(Future.unit)

    val migration = new ImportCustomsOfficesMigration(
      mongoComponent,
      repository,
      ResourceProvider.FromString("""[{"this is":"not a customs office"}]""")
    )

    recoverToSucceededIf[JsResult.Exception](migration.migrationComplete).map { result =>
      verify(repository, times(1)).deleteOffices(eqTo(clientSession))
      verify(repository, never()).saveOffices(eqTo(clientSession), any())
      verify(clientSession, times(1)).abortTransaction()
      result
    }
  }

  it should "roll back when the resource cannot be found" in {
    when(repository.deleteOffices(eqTo(clientSession))).thenReturn(Future.unit)

    val migration = new ImportCustomsOfficesMigration(
      mongoComponent,
      repository,
      new ResourceProvider {
        def getResource(name: String) = () => null
      }
    )

    recoverToSucceededIf[NullPointerException](migration.migrationComplete).map { result =>
      verify(repository, times(1)).deleteOffices(eqTo(clientSession))
      verify(repository, never()).saveOffices(eqTo(clientSession), any())
      verify(clientSession, times(1)).abortTransaction()
      result
    }
  }
}
