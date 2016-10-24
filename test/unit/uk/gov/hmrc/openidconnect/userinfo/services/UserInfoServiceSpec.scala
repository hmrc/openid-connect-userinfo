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

package unit.uk.gov.hmrc.openidconnect.userinfo.services

import org.mockito.BDDMockito.given
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.openidconnect.userinfo.connectors.{AuthConnector, DesConnector}
import uk.gov.hmrc.openidconnect.userinfo.data.UserInfoGenerator
import uk.gov.hmrc.openidconnect.userinfo.domain._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.openidconnect.userinfo.services.{UserInfoTransformer, SandboxUserInfoService, LiveUserInfoService}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future.failed

class UserInfoServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  val nino = "AB123456A"
  val desUserInfo = DesUserInfo(DesUserName(Some("John"), None, Some("Smith")), None, DesAddress(Some("1 Station Road"), Some("Town Centre"), None, None, None, None))
  val userInfo = UserInfo(Some("John"), Some("Smith"), None, Some(Address("1 Station Road\nTown Centre", None, None)),None, Some(nino))

  trait Setup {
    implicit val headers = HeaderCarrier()

    val sandboxInfoService = new SandboxUserInfoService {
      override val userInfoGenerator = mock[UserInfoGenerator]
    }

    val liveInfoService = new LiveUserInfoService {
      override val authConnector: AuthConnector =  mock[AuthConnector]
      override val desConnector: DesConnector = mock[DesConnector]
      override val userInfoTransformer = mock[UserInfoTransformer]
    }
  }

  "LiveUserInfoService" should {

    "return the userInfo" in new Setup {

      given(liveInfoService.authConnector.fetchNino()(headers)).willReturn(Nino(nino))
      given(liveInfoService.desConnector.fetchUserInfo(nino)(headers)).willReturn(Some(desUserInfo))
      given(liveInfoService.userInfoTransformer.transform(Some(desUserInfo), nino)(headers)).willReturn(userInfo)

      val result = await(liveInfoService.fetchUserInfo())

      result shouldBe Some(userInfo)
    }

    "return None when the NINO is not in the authority" in new Setup {
      given(liveInfoService.authConnector.fetchNino()(headers)).willReturn(failed(new NinoNotFoundException))

      val result = await(liveInfoService.fetchUserInfo())

      result shouldBe None
    }

    "return only the nino when there is no user information for this NINO" in new Setup {

      val userInfoWithNinoOnly = UserInfo(None, None, None, None, None, Some(nino))

      given(liveInfoService.authConnector.fetchNino()(headers)).willReturn(Nino(nino))
      given(liveInfoService.desConnector.fetchUserInfo(nino)(headers)).willReturn(None)
      given(liveInfoService.userInfoTransformer.transform(None, nino)(headers)).willReturn(userInfoWithNinoOnly)

      val result = await(liveInfoService.fetchUserInfo())

      result shouldBe Some(userInfoWithNinoOnly)
    }
  }

  "SandboxUserInfoService" should {
    "return generated UserInfo" in new Setup {
      given(sandboxInfoService.userInfoGenerator.userInfo).willReturn(userInfo)

      val result = await(sandboxInfoService.fetchUserInfo())

      result shouldBe Some(userInfo)
    }
  }

}