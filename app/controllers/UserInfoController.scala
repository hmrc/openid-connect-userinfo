/*
 * Copyright 2024 HM Revenue & Customs
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

import com.google.inject.name.Named
import config.AppContext

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BodyParser, ControllerComponents}
import services.UserInfoService
import uk.gov.hmrc.http.{BadRequestException, UnauthorizedException, UpstreamErrorResponse as UER}
import uk.gov.hmrc.http.UpstreamErrorResponse.{Upstream4xxResponse, Upstream5xxResponse}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController

import scala.concurrent.ExecutionContext

sealed trait Version
case object Version_1_0 extends Version

object Version {
  def fromAcceptHeader(header: Option[String]): Version =
    header match {
      case None => Version_1_0
      // integration test using scalaj.http which inject "Accept" header with default values if you don't provide any so we need a case when empty string is like missing Accept Header
      case Some("")                              => Version_1_0
      case Some("application/vnd.hmrc.1.0+json") => Version_1_0
      case _                                     => throw new IllegalArgumentException("Valid version not supplied")
    }
}

trait UserInfoController extends BackendBaseController with HeaderValidator {
  val service: UserInfoService
  val appContext: AppContext
  implicit val executionContext: ExecutionContext
  override val validateVersion: String => Boolean = version => version == "1.0"

  val responseLogger: Logger = Logger("userInfoResponsePayloadLogger")

  // use custom rule as Accept header is optional therefore it has to return true (see more in play.api.mvc.ActionBuilder) if absent in order to let the controller handle it
  private val acceptHeaderValidationRulesCustom: Option[String] => Boolean =
    _.flatMap(a => matchHeader(a).map(res => validateContentType(res.group("contenttype")) && validateVersion(res.group("version")))).getOrElse(true)

  final def userInfo(): Action[AnyContent] = validateAccept(acceptHeaderValidationRulesCustom).async { implicit request =>
    service.fetchUserInfo(Version.fromAcceptHeader(request.headers.get(ACCEPT))) map { userInfo =>
      val json = Json.toJson(userInfo)

      if (appContext.logUserInfoResponsePayload) {
        responseLogger.debug(s"Returning user info payload: $json")
      }

      Ok(json)
    } recover {
      case Upstream4xxResponse(UER(_, 401, _, _))    => Unauthorized(Json.toJson(ErrorUnauthorized()))
      case Upstream4xxResponse(UER(msg4xx, _, _, _)) => BadGateway(Json.toJson(ErrorBadGateway(msg4xx)))
      case Upstream5xxResponse(UER(msg5xx, _, _, _)) => BadGateway(Json.toJson(ErrorBadGateway(msg5xx)))
      case bex: BadRequestException                  => BadRequest(Json.toJson(ErrorBadRequest(bex.getMessage)))
      case uex: UnauthorizedException                => Unauthorized(Json.toJson(ErrorUnauthorized(uex.getMessage)))
    }
  }
}

@Singleton
class SandboxUserInfoController @Inject() (@Named("sandbox") val service: UserInfoService, val appContext: AppContext, val cc: ControllerComponents)(
  implicit val executionContext: ExecutionContext
) extends UserInfoController {
  override val parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser

  override protected def controllerComponents: ControllerComponents = cc
}

@Singleton
class LiveUserInfoController @Inject() (@Named("live") val service: UserInfoService, val appContext: AppContext, val cc: ControllerComponents)(
  implicit val executionContext: ExecutionContext
) extends UserInfoController {
  override val parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser

  override protected def controllerComponents: ControllerComponents = cc
}
