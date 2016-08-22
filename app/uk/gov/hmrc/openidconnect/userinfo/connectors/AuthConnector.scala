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

package uk.gov.hmrc.openidconnect.userinfo.connectors

import play.api.Logger
import uk.gov.hmrc.openidconnect.userinfo.config.WSHttp
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AuthConnector extends uk.gov.hmrc.play.auth.microservice.connectors.AuthConnector {
  val authBaseUrl: String
  val http: HttpGet

  def confidenceLevel()(implicit hc: HeaderCarrier): Future[Option[Int]] = {
      http.GET(s"$authBaseUrl/auth/authority") map {
        resp =>
          val cf = (resp.json \ "confidenceLevel").as[Int]
          Some(cf)
      } recover {
        case e: Throwable =>
          Logger.error("failed to retrieve auth confidenceLevel", e)
          None
      }
  }
}

object AuthConnector extends AuthConnector with ServicesConfig {
  override lazy val authBaseUrl = baseUrl("auth")
  lazy val http = WSHttp
}
