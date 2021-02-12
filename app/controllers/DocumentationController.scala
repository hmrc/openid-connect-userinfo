/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers

import javax.inject.{Inject, Singleton}
import play.api.http.HttpErrorHandler
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import config.{APIAccessVersions, AppContext}
import views._

import scala.language.dynamics

@Singleton
class DocumentationController @Inject() (errorHandler: HttpErrorHandler, appContext: AppContext, assets: Assets, cc: ControllerComponents)
  extends uk.gov.hmrc.api.controllers.DocumentationController(cc, assets, errorHandler) {

  override def definition(): Action[AnyContent] = Action {
    val versions = APIAccessVersions(appContext.access)
    Ok(txt.definition(versions.versions.getOrElse(List()))).withHeaders("Content-Type" -> "application/json")
  }

  def ramlDocs(version: String, filename: String): Action[AnyContent] = {
    assets.at(s"/public/api/conf/$version", filename)
  }
}
