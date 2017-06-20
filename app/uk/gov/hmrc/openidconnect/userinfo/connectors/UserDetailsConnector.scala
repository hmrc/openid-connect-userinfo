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

package uk.gov.hmrc.openidconnect.userinfo.connectors

import play.api.Logger
import uk.gov.hmrc.openidconnect.userinfo.config.WSHttp
import uk.gov.hmrc.openidconnect.userinfo.domain.{Authority, UserDetails}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait UserDetailsConnector {

  val http: HttpGet

  def fetchUserDetails(auth: Authority)(implicit hc: HeaderCarrier): Future[Option[UserDetails]] = {
    auth.userDetailsLink map { url =>
      http.GET[UserDetails](url) map {ud =>
        Some(ud)
      } recover {
        case e: Throwable => {
          Logger.error(e.getMessage, e)
          None
        }
      }
    } getOrElse (Future.successful(None))
  }
}

object UserDetailsConnector extends UserDetailsConnector with ServicesConfig {
  override val http = WSHttp
}
