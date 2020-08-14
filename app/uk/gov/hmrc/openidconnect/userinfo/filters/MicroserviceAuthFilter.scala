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

package uk.gov.hmrc.openidconnect.userinfo.filters

import akka.stream.Materializer
import controllers.Default.Unauthorized
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{Filter, RequestHeader, Result}
import play.api.routing.Router
import uk.gov.hmrc.auth.core.{AuthorisationException, AuthorisedFunctions}
import uk.gov.hmrc.openidconnect.userinfo.connectors.AuthConnector
import uk.gov.hmrc.openidconnect.userinfo.controllers.ErrorUnauthorized
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MicroserviceAuthFilter @Inject() (configuration: Configuration, val authConnector: AuthConnector)(implicit val mat: Materializer, ec: ExecutionContext) extends Filter with AuthorisedFunctions {

  def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(rh.headers)

    rh.tags.get(Router.Tags.RouteController) match {
      case Some(name) if controllerNeedsAuth(name).getOrElse(false) =>
        authorised() {
          next(rh)
        } recoverWith {
          case e: AuthorisationException => Future.successful(Unauthorized(Json.toJson(ErrorUnauthorized())))
        }
      case _ => next(rh)
    }
  }

  lazy val controllerConfigs: Option[Configuration] = configuration.getConfig("controllers")

  def controllerNeedsAuth(controllerName: String): Option[Boolean] =
    controllerConfigs
      .flatMap(_.getConfig(controllerName))
      .flatMap(_.getBoolean("needsAuth"))

}
