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

package unit.uk.gov.hmrc.openidconnect.userinfo.services

import org.mockito.BDDMockito.given
import org.mockito.Matchers.any
import org.mockito.Mockito.{never, verify}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.openidconnect.userinfo.connectors.{AuthConnector, DesConnector, ThirdPartyDelegatedAuthorityConnector}
import uk.gov.hmrc.openidconnect.userinfo.data.UserInfoGenerator
import uk.gov.hmrc.openidconnect.userinfo.domain._
import uk.gov.hmrc.openidconnect.userinfo.services.{LiveUserInfoService, SandboxUserInfoService, UserInfoTransformer}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.Authorization
import uk.gov.hmrc.play.test.UnitSpec

class UserInfoServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  val nino = Nino("AB123456A")
  val authBearerToken = "AUTH_BEARER_TOKEN"
  val desUserInfo = DesUserInfo(DesUserName(Some("John"), None, Some("Smith")), None, DesAddress(Some("1 Station Road"), Some("Town Centre"), None, None, None, None))
  val enrolments = Seq(Enrolment("IR-SA", List(EnrolmentIdentifier("UTR", "174371121"))))
  val userInfo = UserInfo(Some("John"), Some("Smith"), None, Some(Address("1 Station Road\nTown Centre", None, None)),None, Some(nino).map(_.nino), Some(enrolments))

  trait Setup {
    implicit val headers = HeaderCarrier().copy(authorization = Some(Authorization(s"Bearer $authBearerToken")))

    val sandboxInfoService = new SandboxUserInfoService {
      override val userInfoGenerator = mock[UserInfoGenerator]
    }

    val liveInfoService = new LiveUserInfoService {
      override val authConnector: AuthConnector =  mock[AuthConnector]
      override val desConnector: DesConnector = mock[DesConnector]
      override val userInfoTransformer = mock[UserInfoTransformer]
      override val thirdPartyDelegatedAuthorityConnector = mock[ThirdPartyDelegatedAuthorityConnector]
    }
  }

  "LiveUserInfoService" should {

    "requests all available data" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers", "openid:hmrc_enrolments")
      given(liveInfoService.thirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(headers)).willReturn(scopes)
      given(liveInfoService.authConnector.fetchNino()(headers)).willReturn(Some(nino))
      given(liveInfoService.authConnector.fetchEnrolments()(headers)).willReturn(Some(enrolments))
      given(liveInfoService.desConnector.fetchUserInfo(Some(nino))(headers)).willReturn(Some(desUserInfo))
      given(liveInfoService.userInfoTransformer.transform(scopes, Some(desUserInfo), Some(nino), Some(enrolments))).willReturn(any[UserInfo], any[UserInfo])

      await(liveInfoService.fetchUserInfo())

      verify(liveInfoService.desConnector).fetchUserInfo(Some(nino))
      verify(liveInfoService.authConnector).fetchNino()
      verify(liveInfoService.authConnector).fetchEnrolments()
    }

    "return None when the NINO is not in the authority" in new Setup {
      val scopes = Set("address", "profile", "openid:gov-uk-identifiers", "openid:hmrc_enrolments")
      given(liveInfoService.thirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(headers)).willReturn(scopes)
      given(liveInfoService.authConnector.fetchNino()(headers)).willReturn(None)

      val result = await(liveInfoService.fetchUserInfo())

      result shouldBe None
    }

    "does not request DES::fetchUserInfo when the scopes does not contain 'address' nor 'profile'" in new Setup {

      val scopes = Set("openid:gov-uk-identifiers", "openid:hmrc_enrolments")
      given(liveInfoService.thirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(headers)).willReturn(scopes)

      given(liveInfoService.authConnector.fetchNino()(headers)).willReturn(Some(nino))
      given(liveInfoService.authConnector.fetchEnrolments()(headers)).willReturn(Some(enrolments))
      given(liveInfoService.userInfoTransformer.transform(scopes, None, Some(nino), Some(enrolments))).willReturn(any[UserInfo], any[UserInfo])

      await(liveInfoService.fetchUserInfo())

      verify(liveInfoService.desConnector, never).fetchUserInfo(any[Option[Nino]])(any[HeaderCarrier])
      verify(liveInfoService.authConnector).fetchNino()
      verify(liveInfoService.authConnector).fetchEnrolments()
    }

    "does not request AUTH::fetchNino nor DES::fetchUserInfo when the scopes does not contain 'address' nor 'profile' nor 'openid:gov-uk-identifiers'" in new Setup {

      val scopes = Set("openid:hmrc_enrolments")
      given(liveInfoService.thirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(headers)).willReturn(scopes)

      given(liveInfoService.authConnector.fetchEnrolments()(headers)).willReturn(Some(enrolments))
      given(liveInfoService.userInfoTransformer.transform(scopes, None, None, Some(enrolments))).willReturn(any[UserInfo], any[UserInfo])

      await(liveInfoService.fetchUserInfo())

      verify(liveInfoService.desConnector, never).fetchUserInfo(any[Option[Nino]])(any[HeaderCarrier])
      verify(liveInfoService.authConnector, never).fetchNino()
      verify(liveInfoService.authConnector).fetchEnrolments()
    }

    "does not request AUTH::fetchEnrloments when the scopes does not contain 'openid:hmrc_enrolments'" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers")
      given(liveInfoService.thirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(headers)).willReturn(scopes)

      given(liveInfoService.authConnector.fetchNino()(headers)).willReturn(Some(nino))
      given(liveInfoService.desConnector.fetchUserInfo(Some(nino))(headers)).willReturn(None)
      given(liveInfoService.userInfoTransformer.transform(scopes, None, None, Some(enrolments))).willReturn(any[UserInfo], any[UserInfo])

      await(liveInfoService.fetchUserInfo())

      verify(liveInfoService.authConnector, never).fetchEnrolments()
      verify(liveInfoService.authConnector).fetchNino()
      verify(liveInfoService.desConnector).fetchUserInfo(any[Option[Nino]])(any[HeaderCarrier])
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
