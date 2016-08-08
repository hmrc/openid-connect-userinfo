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

package services

import data.UserInfoGenerator
import domain._
import uk.gov.hmrc.play.http.{HeaderCarrier, NotImplementedException}

import scala.concurrent.Future

trait UserInfoService {
  val userInfoGenerator: UserInfoGenerator = UserInfoGenerator
  def fetchUserInfo()(implicit hc: HeaderCarrier): Future[UserInfo]
}

trait LiveUserInfoService extends UserInfoService {
  override def fetchUserInfo()(implicit hc: HeaderCarrier): Future[UserInfo] = {
    Future.failed(new NotImplementedException("endpoint not implemented"))
  }
}

trait SandboxUserInfoService extends UserInfoService {
  override def fetchUserInfo()(implicit hc: HeaderCarrier): Future[UserInfo] = {
    Future.successful(userInfoGenerator.userInfo.sample.getOrElse(throw new RuntimeException("Failed to generate user information")))
  }
}

object LiveUserInfoService extends LiveUserInfoService
object SandboxUserInfoService extends SandboxUserInfoService