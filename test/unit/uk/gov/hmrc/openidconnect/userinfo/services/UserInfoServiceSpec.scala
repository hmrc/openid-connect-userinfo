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
import uk.gov.hmrc.auth.core.retrieve.{ItmpAddress, ItmpName, MdtpInformation}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.openidconnect.userinfo.connectors.{AuthConnector, ThirdPartyDelegatedAuthorityConnector}
import uk.gov.hmrc.openidconnect.userinfo.data.UserInfoGenerator
import uk.gov.hmrc.openidconnect.userinfo.domain._
import uk.gov.hmrc.openidconnect.userinfo.services.{LiveUserInfoService, SandboxUserInfoService, UserInfoTransformer}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, Token }
import uk.gov.hmrc.http.logging.Authorization

class UserInfoServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  val nino = Nino("AB123456A")
  val authBearerToken = "AUTH_BEARER_TOKEN"
  val desUserInfo = DesUserInfo(ItmpName(Some("John"), None, Some("Smith")), None, ItmpAddress(Some("1 Station Road"), Some("Town Centre"), None, None, None, None, None, None))
  val enrolments = Seq(Enrolment("IR-SA", List(EnrolmentIdentifier("UTR", "174371121"))))
  val authority: Authority = Authority(Some("weak"),Some(200),Some("AB123456A"),Some("/uri/to/userDetails"),
    Some("/uri/to/enrolments"),Some("Individual"))

  val userDetails: UserDetails = UserDetails(None, None, None, None, None, None, None, Some("affinityGroup"), None, None,
    Some("User"), None, None, None, None, None, None)

  val governmentGateway: GovernmentGatewayDetails =  GovernmentGatewayDetails(Some("32131"),Some(Seq("User")),
    Some("affinityGroup"), Some("agent-code-12345"), Some("agent-id-12345"), Some("agent-friendly-name"), Some("gateway-token-val"), Some(11))
  val mdtp = Mdtp("device-id-12", "session-id-133")

  val ggToken = Token("ggToken")

  val userInfo = UserInfo(Some("John"), Some("Smith"), None, Some(Address("1 Station Road\nTown Centre", None, None, None)),
    None, None, Some(nino).map(_.nino), Some(enrolments), Some(governmentGateway), Some(mdtp))

  trait Setup {
    implicit val headers = HeaderCarrier().copy(authorization = Some(Authorization(s"Bearer $authBearerToken")))

    val sandboxInfoService = new SandboxUserInfoService {
      override val userInfoGenerator = mock[UserInfoGenerator]
    }

    val liveInfoService = new LiveUserInfoService {
      override val authConnector: AuthConnector =  mock[AuthConnector]
      override val userInfoTransformer = mock[UserInfoTransformer]
      override val thirdPartyDelegatedAuthorityConnector = mock[ThirdPartyDelegatedAuthorityConnector]
    }
  }

  "LiveUserInfoService" should {

    "requests all available data" in new Setup {

      val scopes = Set("openid", "address", "profile", "openid:gov-uk-identifiers", "openid:hmrc-enrolments", "email", "openid:government-gateway")
      given(liveInfoService.thirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(headers)).willReturn(scopes)
      given(liveInfoService.authConnector.fetchAuthority()(headers)).willReturn(Some(authority))
      given(liveInfoService.authConnector.fetchEnrolments(authority)(headers)).willReturn(Some(enrolments))
      given(liveInfoService.authConnector.fetchUserDetails()(headers)).willReturn(Some(userDetails))
      given(liveInfoService.authConnector.fetchDesUserInfo(authority)(headers)).willReturn(Some(desUserInfo))
      given(liveInfoService.userInfoTransformer.transform(scopes, Some(desUserInfo), Some(enrolments), Some(authority),None)).willReturn(any[UserInfo], any[UserInfo])

      await(liveInfoService.fetchUserInfo())

      verify(liveInfoService.authConnector).fetchDesUserInfo(authority)
      verify(liveInfoService.authConnector).fetchEnrolments(authority)
      verify(liveInfoService.authConnector).fetchAuthority()
      verify(liveInfoService.authConnector).fetchUserDetails()(any[HeaderCarrier])
    }

    "return None when the NINO is not in the authority" in new Setup {
      val scopes = Set("address", "profile", "openid:gov-uk-identifiers", "openid:hmrc-enrolments")
      given(liveInfoService.thirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(headers)).willReturn(scopes)
      given(liveInfoService.authConnector.fetchAuthority()(headers)).willReturn(Future(Option(authority.copy(nino = None))))
      given(liveInfoService.authConnector.fetchEnrolments(any())(any())).willReturn(Future(None))

      val result = await(liveInfoService.fetchUserInfo())

      result shouldBe None
    }

    "does not request DES::fetchUserInfo when the scopes does not contain 'address' nor 'profile'" in new Setup {

      val scopes = Set("openid:gov-uk-identifiers", "openid:hmrc-enrolments")
      given(liveInfoService.thirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(headers)).willReturn(scopes)
      given(liveInfoService.authConnector.fetchAuthority()(headers)).willReturn(Future(Option(authority)))
      given(liveInfoService.authConnector.fetchEnrolments(any())(any())).willReturn(Future(None))
      given(liveInfoService.userInfoTransformer.transform(scopes, None, Some(enrolments), Some(authority), Some(userDetails))).willReturn(any[UserInfo], any[UserInfo])

      await(liveInfoService.fetchUserInfo())

      verify(liveInfoService.authConnector, never).fetchDesUserInfo(any[Authority])(any[HeaderCarrier])
      verify(liveInfoService.authConnector).fetchEnrolments(authority)
    }

    "does not request AUTH::fetchNino nor DES::fetchUserInfo when the scopes does not contain 'address' nor 'profile' nor 'openid:gov-uk-identifiers'" in new Setup {

      val scopes = Set("openid:hmrc-enrolments")
      given(liveInfoService.thirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(headers)).willReturn(scopes)

      given(liveInfoService.authConnector.fetchAuthority()(headers)).willReturn(Option(authority))
      given(liveInfoService.authConnector.fetchEnrolments(authority)(headers)).willReturn(Some(enrolments))
      given(liveInfoService.userInfoTransformer.transform(scopes, None, Some(enrolments), Some(authority), None)).willReturn(any[UserInfo], any[UserInfo])

      await(liveInfoService.fetchUserInfo())

      verify(liveInfoService.authConnector, never).fetchDesUserInfo(any[Authority])(any[HeaderCarrier])
      verify(liveInfoService.authConnector).fetchEnrolments(authority)
    }

    "does not request AUTH::fetchEnrloments when the scopes does not contain 'openid:hmrc-enrolments'" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers")
      given(liveInfoService.thirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(headers)).willReturn(scopes)

      given(liveInfoService.authConnector.fetchAuthority()(headers)).willReturn(Future(Option(authority)))
      given(liveInfoService.authConnector.fetchDesUserInfo(authority)(headers)).willReturn(None)
      given(liveInfoService.userInfoTransformer.transform(scopes, None, Some(enrolments), None, None)).willReturn(any[UserInfo], any[UserInfo])

      await(liveInfoService.fetchUserInfo())

      verify(liveInfoService.authConnector, never).fetchEnrolments(any[Authority])(any[HeaderCarrier])
      verify(liveInfoService.authConnector).fetchDesUserInfo(any[Authority])(any[HeaderCarrier])
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
