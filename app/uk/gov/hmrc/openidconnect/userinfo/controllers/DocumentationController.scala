/*
 * Copyright 2016 HM Revenue & Customs
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

import uk.gov.hmrc.openidconnect.userinfo.config.{APIAccessConfig, AppContext}
import uk.gov.hmrc.openidconnect.userinfo.data.UserInfoGenerator
import uk.gov.hmrc.openidconnect.userinfo.domain.{APIAccess, UserInfo}
import uk.gov.hmrc.openidconnect.userinfo.views._

import play.api.mvc.{Action, AnyContent}
import play.twirl.api.Xml

import scala.language.dynamics

trait DocumentationController extends uk.gov.hmrc.api.controllers.DocumentationController {

  override def definition(): Action[AnyContent] = Action {
    Ok(txt.definition(buildAccess())).withHeaders(("Content-Type" -> "application/json"))
  }

  override def documentation(version: String, endpointName: String): Action[AnyContent] = Action {
    Documentation.findDocumentation(endpointName, version) match {
      case Some(docs) => Ok(docs).withHeaders("Content-Type" -> "application/xml")
      case None => NotFound
    }
  }

  private def buildAccess() = {
    val access = APIAccessConfig(AppContext.access)
    APIAccess(access.accessType, access.whiteListedApplicationIds)
  }
}

object DocumentationController extends DocumentationController

object Documentation {
  val version1_0 = "1.0"
  val getUserInfo = "Get user info"
  val getUserInfoPost = "Get user info POST"

  def findDocumentation(endpointName: String, version: String) = applyTemplate(endpointName, version)(userInfo)

  def applyTemplate(apiName: String, version: String)(info: UserInfo): Option[Xml] = {
    (apiName, version) match {
      case (`getUserInfo`, `version1_0`) => Some(xml.getUserInfo(info))
      case (`getUserInfoPost`, `version1_0`) => Some(xml.getUserInfoPost(info))
      case _ => None
    }
  }

  def userInfo = {
    UserInfoGenerator.userInfo.sample match {
      case Some(userInfo) => userInfo
      case None => throw new RuntimeException("Failed to generate dynamic UserInfo")
    }
  }
}
