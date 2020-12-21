/*
 * Copyright 2020 HM Revenue & Customs
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
import com.google.inject.name.Named
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, BodyParser, ControllerComponents}
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.http.{BadRequestException, Upstream4xxResponse, Upstream5xxResponse}
import config.AppContext
import services.{LiveUserInfoService, SandboxUserInfoService, UserInfoService}
import uk.gov.hmrc.play.bootstrap.controller.BackendBaseController

import scala.concurrent.ExecutionContext

sealed trait Version
case object Version_1_0 extends Version
case object Version_1_1 extends Version

object Version {
  def fromAcceptHeader(header: Option[String]): Version = {
    header match {
      case Some("application/vnd.hmrc.1.0+json") => Version_1_0
      case Some("application/vnd.hmrc.1.1+json") => Version_1_1
      case _                                     => throw new IllegalArgumentException("Valid version not supplied")
    }
  }
}

trait UserInfoController extends BackendBaseController with HeaderValidator {
  val service: UserInfoService
  val appContext: AppContext
  implicit val executionContext: ExecutionContext
  override val validateVersion: String => Boolean = version => (version == "1.0") | (version == "1.1")

  val responseLogger = Logger("userInfoResponsePayloadLogger")

  final def userInfo() = validateAccept(acceptHeaderValidationRules).async { implicit request =>
    service.fetchUserInfo(Version.fromAcceptHeader(request.headers.get(ACCEPT))) map { userInfo =>
      val json = Json.toJson(userInfo)

      if (appContext.logUserInfoResponsePayload) {
        responseLogger.debug(s"Returning user info payload: $json")
      }

      Ok(json)
    } recover {
      case Upstream4xxResponse(msg, 401, _, _)  => Unauthorized(Json.toJson(ErrorUnauthorized()))
      case Upstream4xxResponse(msg4xx, _, _, _) => BadGateway(Json.toJson(ErrorBadGateway(msg4xx)))
      case Upstream5xxResponse(msg5xx, _, _, _) => BadGateway(Json.toJson(ErrorBadGateway(msg5xx)))
      case bex: BadRequestException             => BadRequest(Json.toJson(ErrorBadRequest(bex.getMessage)))
    }
  }
}

@Singleton
class SandboxUserInfoController @Inject() (@Named("sandbox") val service: UserInfoService, val appContext: AppContext, val cc: ControllerComponents)(implicit val executionContext: ExecutionContext) extends UserInfoController {
  override val parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser

  override protected def controllerComponents: ControllerComponents = cc
}

@Singleton
class LiveUserInfoController @Inject() (@Named("live") val service: UserInfoService, val appContext: AppContext, val cc: ControllerComponents)(implicit val executionContext: ExecutionContext) extends UserInfoController {
  override val parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser

  override protected def controllerComponents: ControllerComponents = cc
}
