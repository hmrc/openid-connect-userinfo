/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.openidconnect.userinfo.controllers

import controllers.Assets
import play.api.http.{HttpErrorHandler, LazyHttpErrorHandler}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.openidconnect.userinfo.config.{APIAccessConfig, AppContext}
import uk.gov.hmrc.openidconnect.userinfo.domain.APIAccess
import uk.gov.hmrc.openidconnect.userinfo.views._

import scala.language.dynamics

class DocumentationController(errorHandler:HttpErrorHandler) extends uk.gov.hmrc.api.controllers.DocumentationController(errorHandler) {

  override def definition(): Action[AnyContent] = Action {
    Ok(txt.definition(buildAccess())).withHeaders("Content-Type" -> "application/json")
  }

  def ramlDocs(version: String, filename: String): Action[AnyContent] = {
    Assets.at(s"/public/api/conf/$version", filename)
  }

  private def buildAccess() = {
    val access = APIAccessConfig(AppContext.access)
    APIAccess(access.accessType, access.whiteListedApplicationIds)
  }
}

object DocumentationController extends DocumentationController(LazyHttpErrorHandler)
