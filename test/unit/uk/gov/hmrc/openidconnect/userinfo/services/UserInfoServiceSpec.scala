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
import uk.gov.hmrc.openidconnect.userinfo.connectors.{ThirdPartyDelegatedAuthorityConnector, AuthConnector, DesConnector}
import uk.gov.hmrc.openidconnect.userinfo.data.UserInfoGenerator
import uk.gov.hmrc.openidconnect.userinfo.domain._
import org.joda.time.LocalDate
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.openidconnect.userinfo.services.{CountryService, SandboxUserInfoService, LiveUserInfoService}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import scala.concurrent.Future.failed

class UserInfoServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  trait Setup {
    implicit val headers = HeaderCarrier()

    val sandboxInfoService = new SandboxUserInfoService {
      override val userInfoGenerator = mock[UserInfoGenerator]
    }

    val liveInfoService = new LiveUserInfoService {
      override val authConnector: AuthConnector =  mock[AuthConnector]
      override val desConnector: DesConnector = mock[DesConnector]
      override val countryService: CountryService = mock[CountryService]
    }
  }

  "LiveUserInfoService" should {
    val nino = "AB123456A"
    val countryCode = 10

    "return the userInfo" in new Setup {
      val desUserInfo = DesUserInfo(nino, DesUserName("John", None, "Smith"), None,
        DesAddress("1 Station Road", "Town Centre", None, None, None, None))

      given(liveInfoService.authConnector.fetchNino()(headers)).willReturn(Nino(nino))
      given(liveInfoService.desConnector.fetchUserInfo(nino)(headers)).willReturn(desUserInfo)

      val result = await(liveInfoService.fetchUserInfo())

      result shouldBe Some(UserInfo("John", "Smith", None, Address("1 Station Road\nTown Centre", None, None),None, nino))
    }

    "return the userInfo with all datas" in new Setup {
      val desUserInfo = DesUserInfo(nino, DesUserName("John", Some("A"), "Smith"), Some(LocalDate.parse("1980-01-01")),
        DesAddress("1 Station Road", "Town Centre", Some("London"), Some("England"), Some("NW1 6XE"), Some(countryCode)))

      given(liveInfoService.authConnector.fetchNino()(headers)).willReturn(Nino(nino))
      given(liveInfoService.desConnector.fetchUserInfo(nino)(headers)).willReturn(desUserInfo)
      given(liveInfoService.countryService.getCountry(countryCode)).willReturn(Some("United Kingdom"))

      val result = await(liveInfoService.fetchUserInfo())

      result shouldBe Some(UserInfo("John", "Smith", Some("A"),
        Address("1 Station Road\nTown Centre\nLondon\nEngland\nNW1 6XE\nUnited Kingdom", Some("NW1 6XE"), Some("United Kingdom")),
        Some(LocalDate.parse("1980-01-01")),
        nino))
    }

    "return None when the NINO is not in the authority" in new Setup {
      given(liveInfoService.authConnector.fetchNino()(headers)).willReturn(failed(new NinoNotFoundException))

      val result = await(liveInfoService.fetchUserInfo())

      result shouldBe None
    }

    "return None when there is no user information for this NINO" in new Setup {

      given(liveInfoService.authConnector.fetchNino()(headers)).willReturn(Nino(nino))
      given(liveInfoService.desConnector.fetchUserInfo(nino)(headers)).willReturn(failed(new UserInfoNotFoundException))

      val result = await(liveInfoService.fetchUserInfo())

      result shouldBe None
    }
  }

  "SandboxUserInfoService" should {
    "return generated UserInfo" in new Setup {

      val userInfo = UserInfo("John", "Smith", Some("Hannibal"),
        Address("221B BAKER STREET\nLONDON NW1 9NT\nUnited Kingdom", Some("NW1 9NT"), Some("United Kingdom")),
        Some(new LocalDate(1982,11, 15)), "AB123456A")

      given(sandboxInfoService.userInfoGenerator.userInfo).willReturn(userInfo)

      val result = await(sandboxInfoService.fetchUserInfo())

      result shouldBe Some(userInfo)
    }
  }

}