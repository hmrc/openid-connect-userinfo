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
import uk.gov.hmrc.openidconnect.userinfo.domain.{Authority, Enrolment}
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, NotFoundException, Upstream4xxResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AuthConnector extends uk.gov.hmrc.play.auth.microservice.connectors.AuthConnector {
  val authBaseUrl: String

  val http: HttpGet

  def fetchEnrolments(auth: Authority)(implicit headerCarrier: HeaderCarrier): Future[Option[Seq[Enrolment]]] = {
    auth.enrolments map { enrolmentsUri =>
      http.GET(s"$authBaseUrl$enrolmentsUri") map { response =>
        response.json.asOpt[Seq[Enrolment]]
      } recover {
        case e: NotFoundException => {
          None
        }
      }
    } getOrElse {
      Logger.debug("No enrolment uri.")
      Future.successful(None)
    }
  }

  def fetchAuthority()(implicit headerCarrier: HeaderCarrier): Future[Option[Authority]] = {
    http.GET(s"$authBaseUrl/auth/authority") map { response =>
      response.json.asOpt[Authority]
    }
  }
}

object AuthConnector extends AuthConnector with ServicesConfig {
  override lazy val authBaseUrl = baseUrl("auth")
  lazy val http = WSHttp
}
