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
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.openidconnect.userinfo.config.WSHttp
import uk.gov.hmrc.openidconnect.userinfo.domain.Enrolment
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AuthConnector extends uk.gov.hmrc.play.auth.microservice.connectors.AuthConnector {
  val authBaseUrl: String
  val http: HttpGet

  def confidenceLevel()(implicit hc: HeaderCarrier): Future[Option[Int]] = {
    http.GET(s"$authBaseUrl/auth/authority") map { resp =>
      (resp.json \ "confidenceLevel").asOpt[Int]
    } recover {
      case e: Throwable => {
        Logger.error(e.getMessage, e)
        None
      }
    }
  }

  def fetchNino()(implicit hc:HeaderCarrier): Future[Option[Nino]] = {
    http.GET(s"$authBaseUrl/auth/authority") map {
      resp => (resp.json \ "nino").asOpt[Nino]
    } recover {
      case e: Throwable => {
        Logger.error("Nino not present.", e)
        None
      }
    }
  }

  def fetchEnrolments()(implicit headerCarrier: HeaderCarrier): Future[Option[Seq[Enrolment]]] = {
    getEnrolmentsUri flatMap {
      case Some(enrolmentsUri) => {
        http.GET(s"$authBaseUrl$enrolmentsUri") map { response =>
          response.json.asOpt[Seq[Enrolment]]
        } recover {
          case e: Throwable => {
            Logger.error(e.getMessage, e)
            None
          }
        }
      }
      case None => {
        Logger.debug("No enrolment uri.")
        Future.successful(None)
      }
    }
  }

  private def getEnrolmentsUri()(implicit hc: HeaderCarrier): Future[Option[String]] = {
    http.GET(s"$authBaseUrl/auth/authority").map { response =>
      (response.json \ "enrolments").asOpt[String]
    } recover {
      case e: Throwable => {
        Logger.error(e.getMessage, e)
        None
      }
    }
  }
}

object AuthConnector extends AuthConnector with ServicesConfig {
  override lazy val authBaseUrl = baseUrl("auth")
  lazy val http = WSHttp
}
