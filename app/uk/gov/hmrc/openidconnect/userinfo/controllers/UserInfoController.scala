package uk.gov.hmrc.openidconnect.userinfo.controllers

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

import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.openidconnect.userinfo.services.{LiveUserInfoService, SandboxUserInfoService, UserInfoService}
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.play.http.{HeaderCarrier, NotImplementedException}
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

trait UserInfoController extends BaseController with HeaderValidator {
  val service: UserInfoService
  implicit val hc: HeaderCarrier = HeaderCarrier()

  final def userInfo() = validateAccept(acceptHeaderValidationRules).async {
    service.fetchUserInfo().map(userInfo => Ok(Json.toJson(userInfo))
    ) recover {
      case ex: NotImplementedException => Status(ErrorNotImplemented.httpStatusCode)(Json.toJson(ErrorNotImplemented))
      case e: Throwable =>
        Logger.error(s"Internal server error: ${e.getMessage}", e)
        Status(ErrorInternalServerError.httpStatusCode)(Json.toJson(ErrorInternalServerError))
    }
  }
}

trait SandboxUserInfoController extends UserInfoController {
  override val service = SandboxUserInfoService
}

trait LiveUserInfoController extends UserInfoController {
  override val service = LiveUserInfoService
}

object SandboxUserInfoController extends SandboxUserInfoController
object LiveUserInfoController extends LiveUserInfoController