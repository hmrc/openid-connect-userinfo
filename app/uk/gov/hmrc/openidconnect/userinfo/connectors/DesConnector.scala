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
import uk.gov.hmrc.openidconnect.userinfo.config.{AppContext, WSHttp}
import uk.gov.hmrc.openidconnect.userinfo.domain.DesUserInfo
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.logging.Authorization

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait DesConnector {

  val NINO_LENGTH = 8

  val http: HttpGet
  val serviceUrl: String
  val desEnvironment: String
  val desBearerToken: String

  def fetchUserInfo(nino: Option[Nino])(implicit hc: HeaderCarrier): Future[Option[DesUserInfo]] = {
    val newHc = hc.copy(authorization = Some(Authorization(s"Bearer $desBearerToken"))).withExtraHeaders("Environment" -> desEnvironment)

    nino map { ninoValue =>
      val url = s"$serviceUrl/pay-as-you-earn/individuals/${withoutSuffix(ninoValue.nino)}"
      Logger.debug(s"GET $url with environment=$desEnvironment")

      http.GET[DesUserInfo](url)(implicitly[HttpReads[DesUserInfo]], newHc) map (Some(_)) recover {
        case _: NotFoundException | _: BadRequestException =>
          Logger.debug(s"User information for nino $nino is not available in DES")
          None
      }
    } getOrElse {
      Future.successful(None)
    }
  }

  private def withoutSuffix(nino: String) = nino.take(NINO_LENGTH)
}


object DesConnector extends DesConnector with ServicesConfig {
  override val http = WSHttp
  override val serviceUrl = baseUrl("des")
  override val desEnvironment = AppContext.desEnvironment
  override val desBearerToken = AppContext.desBearerToken
}
