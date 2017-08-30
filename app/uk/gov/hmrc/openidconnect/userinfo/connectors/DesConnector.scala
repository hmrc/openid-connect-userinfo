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

import uk.gov.hmrc.auth.core.retrieve.{Retrievals, ~}
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, PlayAuthConnector}
import uk.gov.hmrc.openidconnect.userinfo.config.WSHttp
import uk.gov.hmrc.openidconnect.userinfo.domain.{Authority, DesUserInfo, NinoNotFoundException}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait DesConnector extends AuthorisedFunctions {

  val http: HttpGet

  def fetchUserInfo(authority: Authority)(implicit hc: HeaderCarrier): Future[Option[DesUserInfo]] = {
    val nothing = Future.successful(None)
    if (authority.nino.isDefined)
      authorised().retrieve(Retrievals.allItmpUserDetails) {
        case name ~ dateOfBirth ~ address =>
          Future.successful(Some(DesUserInfo(name, dateOfBirth, address)))
        case _ => nothing
      }.recoverWith {
        case ex: NotFoundException => nothing
      }
    else nothing
  }
}


object DesConnector extends DesConnector with ServicesConfig {
  override val http = WSHttp
  override def authConnector = new PlayAuthConnector {
    override val serviceUrl = baseUrl("auth")

    override def http = WSHttp
  }
}