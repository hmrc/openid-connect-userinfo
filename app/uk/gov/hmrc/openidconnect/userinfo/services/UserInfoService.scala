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

package uk.gov.hmrc.openidconnect.userinfo.services

import play.api.Logger
import uk.gov.hmrc.openidconnect.userinfo.connectors.{AuthConnector, DesConnector}
import uk.gov.hmrc.openidconnect.userinfo.data.UserInfoGenerator
import uk.gov.hmrc.openidconnect.userinfo.domain._
import uk.gov.hmrc.play.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

trait UserInfoService {
  def fetchUserInfo()(implicit hc: HeaderCarrier): Future[Option[UserInfo]]
}

trait LiveUserInfoService extends UserInfoService {
  val authConnector: AuthConnector
  val desConnector: DesConnector
  val userInfoTransformer: UserInfoTransformer

  override def fetchUserInfo()(implicit hc: HeaderCarrier): Future[Option[UserInfo]] = {

    val future: Future[UserInfo] = for {
      nino <- authConnector.fetchNino()
      desUserInfo <- desConnector.fetchUserInfo(nino.nino)
      userInfo <- userInfoTransformer.transform(desUserInfo, nino.nino)
    } yield userInfo

    future map (Some(_)) recover {
      case NinoNotFoundException() =>
        Logger.debug("Nino not present in Bearer Token")
        None
    }
  }
}

trait SandboxUserInfoService extends UserInfoService {
  val userInfoGenerator: UserInfoGenerator

  override def fetchUserInfo()(implicit hc: HeaderCarrier): Future[Option[UserInfo]] = {
    Future.successful(userInfoGenerator.userInfo.sample)
  }
}

object LiveUserInfoService extends LiveUserInfoService {
  override val desConnector = DesConnector
  override val authConnector = AuthConnector
  override val userInfoTransformer = UserInfoTransformer
}

object SandboxUserInfoService extends SandboxUserInfoService {
  override val userInfoGenerator = UserInfoGenerator
}
