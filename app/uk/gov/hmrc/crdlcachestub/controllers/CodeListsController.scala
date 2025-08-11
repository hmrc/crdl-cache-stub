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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.crdlcachestub.repositories.CodeListsRepository
import uk.gov.hmrc.crdlcachestub.repositories.migration.ImportCodeListsMigration
import uk.gov.hmrc.crdlcachestub.models.{CodeListCode, CodeListEntry}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import play.api.http.HeaderNames
import play.api.mvc.RequestHeader
import scala.concurrent.Future
import uk.gov.hmrc.http.UpstreamErrorResponse

@Singleton()
class CodeListsController @Inject() (
  cc: ControllerComponents,
  codeListsRepository: CodeListsRepository,
  codeListImport: ImportCodeListsMigration
)(using ec: ExecutionContext)
  extends BackendController(cc) {

  private def hasAuthorizationHeader(request: RequestHeader) =
    request.headers.get(HeaderNames.AUTHORIZATION).isDefined

  def fetchCodeListEntries(
    codeListCode: CodeListCode,
    filterKeys: Option[Set[String]],
    filterProperties: Option[Map[String, JsValue]]
  ): Action[AnyContent] =
    Action.async { request =>
      if (!hasAuthorizationHeader(request))
        Future.failed(UpstreamErrorResponse("Unauthorized", UNAUTHORIZED))
      else
        for {
          _ <- codeListImport.migrationComplete
          entries <- codeListsRepository.fetchEntries(
            codeListCode,
            filterKeys,
            filterProperties
          )
        } yield Ok(Json.toJson(entries))
    }

  def fetchCodeListVersions: Action[AnyContent] =
    Action { request =>
      if (!hasAuthorizationHeader(request))
        throw UpstreamErrorResponse("Unauthorized", UNAUTHORIZED)
      else
        InternalServerError
    }
}
