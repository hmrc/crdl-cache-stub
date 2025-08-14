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

import play.api.http.HeaderNames
import play.api.mvc.{Action, AnyContent, ControllerComponents, RequestHeader}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.crdlcachestub.repositories.CustomsOfficeListsRepository
import play.api.libs.json.Json
import uk.gov.hmrc.crdlcachestub.models.formats.HttpFormats
import uk.gov.hmrc.crdlcachestub.repositories.migration.ImportCustomsOfficesMigration
import uk.gov.hmrc.http.UpstreamErrorResponse

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class CustomsOfficeListsController @Inject() (
  cc: ControllerComponents,
  customsOfficeListsRepository: CustomsOfficeListsRepository,
  customsOfficeListsImport: ImportCustomsOfficesMigration
)(using ec: ExecutionContext)
  extends BackendController(cc)
  with HttpFormats {

  private def hasAuthorizationHeader(request: RequestHeader) =
    request.headers.get(HeaderNames.AUTHORIZATION).isDefined

  def fetchCustomsOfficeLists(
    referenceNumbers: Option[Set[String]],
    countryCodes: Option[Set[String]],
    roles: Option[Set[String]]
  ): Action[AnyContent] = Action.async { request =>
    if (!hasAuthorizationHeader(request))
      Future.failed(UpstreamErrorResponse("Unauthorized", UNAUTHORIZED))
    else
      for {
        _ <- customsOfficeListsImport.migrationComplete
        offices <- customsOfficeListsRepository
          .fetchCustomsOfficeLists(
            referenceNumbers,
            countryCodes,
            roles
          )
      } yield Ok(Json.toJson(offices))
  }
}
