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

package uk.gov.hmrc.openidconnect.userinfo.filters

import controllers.Default.Unauthorized
import play.api.libs.json.Json
import play.api.mvc.{Filter, RequestHeader, Result}
import play.api.routing.Router
import uk.gov.hmrc.openidconnect.userinfo.config.{AuthParamsControllerConfiguration, ControllerConfiguration}
import uk.gov.hmrc.openidconnect.userinfo.controllers.ErrorUnauthorized
import uk.gov.hmrc.openidconnect.userinfo.services.AuthService
import uk.gov.hmrc.play.auth.controllers.{AuthConfig, AuthParamsControllerConfig}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.microservice.filters.MicroserviceFilterSupport

trait MicroserviceAuthFilter extends Filter {
  def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(rh.headers)

    def authConfig(rh: RequestHeader): Option[AuthConfig] = {
      rh.tags.get(Router.Tags.RouteController).flatMap { name =>
        if (controllerNeedsAuth(name)) Some(authParamsConfig.authConfig(name))
        else None
      }
    }

    authConfig(rh) match {
      case Some(authConfig) => authService.isAuthorised().flatMap {
        case true => next(rh)
        case _ => Future.successful(Unauthorized(Json.toJson(ErrorUnauthorized())))
      }
      case _ => next(rh)
    }
  }

  val authService: AuthService
  val authParamsConfig: AuthParamsControllerConfig

  def controllerNeedsAuth(controllerName: String): Boolean = ControllerConfiguration.paramsForController(controllerName).needsAuth
}

object MicroserviceAuthFilter extends MicroserviceAuthFilter with MicroserviceFilterSupport {
  override lazy val authService = AuthService
  override lazy val authParamsConfig = AuthParamsControllerConfiguration
}
