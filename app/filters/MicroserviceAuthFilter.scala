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

package filters

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{Filter, RequestHeader, Result, Results}
import play.api.routing.Router
import uk.gov.hmrc.auth.core.{AuthorisationException, AuthorisedFunctions}
import connectors.AuthConnector
import controllers.ErrorUnauthorized
import org.apache.pekko.stream.Materializer
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MicroserviceAuthFilter @Inject() (configuration: Configuration, val authConnector: AuthConnector)(implicit
  val mat: Materializer,
  ec:      ExecutionContext
) extends Filter
    with AuthorisedFunctions
    with Results {

  def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    implicit val hc = HeaderCarrierConverter.fromRequest(rh)

    rh.attrs.get(Router.Attrs.HandlerDef) match {
      case Some(name) if controllerNeedsAuth(name.controller).getOrElse(false) =>
        authorised() {
          next(rh)
        } recoverWith { case e: AuthorisationException =>
          Future.successful(Unauthorized(Json.toJson(ErrorUnauthorized())))
        }
      case _ => next(rh)
    }
  }

  lazy val controllerConfigs: Option[Configuration] = configuration.getOptional[Configuration]("controllers")

  def controllerNeedsAuth(controllerName: String): Option[Boolean] =
    controllerConfigs
      .flatMap(_.getOptional[Configuration](controllerName))
      .flatMap(_.getOptional[Boolean]("needsAuth"))

}
