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

package handlers

import javax.inject.{Inject, Singleton}
import com.google.inject.Provider
import play.api.http.DefaultHttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc.Results.Status
import play.api.mvc.{RequestHeader, Result}
import play.api.routing.Router
import play.api.{Configuration, Environment, OptionalSourceMapper}
import controllers.{ErrorBadGateway, ErrorUnauthorized}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ErrorHandler @Inject() (
    env:          Environment,
    config:       Configuration,
    sourceMapper: OptionalSourceMapper,
    router:       Provider[Router]
)(implicit ec: ExecutionContext) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    super.onServerError(request, exception) map (res => {
      res.header.status match {
        case 401 => Status(ErrorUnauthorized().httpStatusCode)(Json.toJson(ErrorUnauthorized()))
        case _   => Status(ErrorBadGateway().httpStatusCode)(Json.toJson(ErrorBadGateway(exception.getMessage)))
      }
    })
  }
}
