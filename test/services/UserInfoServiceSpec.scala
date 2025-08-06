/*
 * Copyright 2024 HM Revenue & Customs
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

import connectors.{AuthConnector, AuthConnectorV1, ThirdPartyDelegatedAuthorityConnector, TrustedHelperConnector}
import controllers.Version_1_0
import data.UserInfoGenerator
import domain.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import testSupport.UnitSpec
import uk.gov.hmrc.auth.core.retrieve.{ItmpAddress, ItmpName}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{Authorization, BadRequestException, HeaderCarrier, UnauthorizedException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class UserInfoServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  val nino: Nino = Nino("AB123456A")
  val authorizationTokens = "Bearer AUTH_TOKENS"
  val accessToken = "ACCESS_TOKEN"
  val otherHeaders: Seq[(String, String)] = Seq(("X-Client-Authorization-Token", accessToken))
  val desUserInfo: DesUserInfo = DesUserInfo(Some(ItmpName(Some("John"), None, Some("Smith"))),
                                             None,
                                             Some(ItmpAddress(Some("1 Station Road"), Some("Town Centre"), None, None, None, None, None, None))
                                            )
  val enrolments: Enrolments = Enrolments(Set(Enrolment("IR-SA", List(EnrolmentIdentifier("UTR", "174371121")), "Activated")))
  val authority: Authority = Authority("32131", Some("AB123456A"))

  val userDetails: UserDetails =
    UserDetails(None, None, None, None, None, None, None, None, Some("affinityGroup"), None, None, Some("User"), None, None, None, None, None, None)

  val governmentGateway: GovernmentGatewayDetails = GovernmentGatewayDetails(
    Some("32131"),
    Some(scala.collection.immutable.Seq("User")),
    Some("John"),
    Some("affinityGroup"),
    Some("agent-code-12345"),
    Some("agent-id-12345"),
    Some("agent-friendly-name"),
    Some("gateway-token-val"),
    Some(11),
    None,
    None
  )
  val mdtp: Mdtp = Mdtp("device-id-12", "session-id-133")

  val testTrustedHelper = TrustedHelper(
    principal_name  = "John Smith",
    attorney_name   = "Jane Doe",
    return_link_url = "/trusted-helpers/redirect-to-trusted-helpers",
    principal_nino  = "AA000001A"
  )

  val userInfo: UserInfo = UserInfo(
    Some("John"),
    Some("Smith"),
    None,
    Some(Address("1 Station Road\nTown Centre", None, None, None)),
    None,
    None,
    Some(nino).map(_.nino),
    Some(enrolments.enrolments),
    Some(governmentGateway),
    Some(mdtp),
    Some(testTrustedHelper),
    None,
    None
  )

  trait Setup {
    implicit val headers: HeaderCarrier = HeaderCarrier().copy(authorization = Some(Authorization(authorizationTokens)), otherHeaders = otherHeaders)

    val mockAuthConnector: AuthConnector = mock[AuthConnectorV1]
    val mockUserInfoGenerator: UserInfoGenerator = mock[UserInfoGenerator]
    val mockUserInfoTransformer: UserInfoTransformer = mock[UserInfoTransformer]
    val mockThirdPartyDelegatedAuthorityConnector: ThirdPartyDelegatedAuthorityConnector = mock[ThirdPartyDelegatedAuthorityConnector]
    val mockTrustedHelperConnector: TrustedHelperConnector = mock[TrustedHelperConnector]

    val sandboxInfoService = new SandboxUserInfoService(mockUserInfoGenerator)
    val liveInfoService =
      new LiveUserInfoService(mockAuthConnector, mockUserInfoTransformer, mockThirdPartyDelegatedAuthorityConnector, mockTrustedHelperConnector)
  }

  "LiveUserInfoService" should {

    "requests all available data including trusted helper when access token in otherHeaders" in new Setup {

      val scopes = Set("openid",
                       "address",
                       "profile",
                       "openid:gov-uk-identifiers",
                       "openid:hmrc-enrolments",
                       "email",
                       "openid:government-gateway",
                       "openid:trusted-helper"
                      )
      when(mockThirdPartyDelegatedAuthorityConnector.fetchScopes(eqTo(accessToken))(eqTo(headers), any[ExecutionContext]))
        .thenReturn(Future.successful(scopes))
      when(mockAuthConnector.fetchAuthority()(eqTo(headers), any[ExecutionContext])).thenReturn(Future(Some(authority)))
      when(mockAuthConnector.fetchEnrolments()(eqTo(headers), any[ExecutionContext])).thenReturn(Future.successful(Some(enrolments)))
      when(mockAuthConnector.fetchDesUserInfo()(eqTo(headers), any[ExecutionContext])).thenReturn(Future.successful(Some(desUserInfo)))
      when(mockAuthConnector.fetchDetails()(eqTo(headers), any[ExecutionContext])).thenReturn(Future.successful(Some(userDetails)))
      when(mockTrustedHelperConnector.getDelegation()(eqTo(headers), any[ExecutionContext])).thenReturn(Future.successful(Some(testTrustedHelper)))
      when(
        mockUserInfoTransformer.transform(eqTo(scopes),
                                          eqTo(Some(authority)),
                                          eqTo(Some(desUserInfo)),
                                          eqTo(Some(enrolments)),
                                          eqTo(Some(userDetails)),
                                          eqTo(Some(testTrustedHelper))
                                         )
      )
        .thenReturn(UserInfo())

      await(liveInfoService.fetchUserInfo(Version_1_0))

      verify(mockAuthConnector).fetchDesUserInfo()
      verify(mockAuthConnector).fetchEnrolments()
      verify(mockAuthConnector).fetchAuthority()
      verify(mockAuthConnector).fetchDetails()(any[HeaderCarrier], any[ExecutionContext])
      verify(mockTrustedHelperConnector).getDelegation()(any[HeaderCarrier], any[ExecutionContext])
    }

    "should fail with UnauthorizedException when access token is not in the headers" in new Setup {
      val headersWithoutAccessToken = headers.copy(otherHeaders = Seq.empty)
      a[UnauthorizedException] should be thrownBy await(liveInfoService.fetchUserInfo(Version_1_0)(headersWithoutAccessToken))
    }

    "should fail with BadRequestException when the NINO is not in the authority and a scope that requires a NINO is requested UNICORN" in new Setup {
      val scopes = Set("address", "profile", "openid:gov-uk-identifiers", "openid:hmrc-enrolments")
      when(mockThirdPartyDelegatedAuthorityConnector.fetchScopes(eqTo(accessToken))(eqTo(headers), any[ExecutionContext]))
        .thenReturn(Future.successful(scopes))
      when(mockAuthConnector.fetchAuthority()(eqTo(headers), any[ExecutionContext])).thenReturn(Future(Some(authority.copy(nino = None))))
      when(mockAuthConnector.fetchEnrolments()(eqTo(headers), any[ExecutionContext])).thenReturn(Future(None))

      a[BadRequestException] should be thrownBy await(liveInfoService.fetchUserInfo(Version_1_0))
    }

    "does not request DES::fetchUserInfo when the scopes does not contain 'address' nor 'profile'" in new Setup {

      val scopes = Set("openid:gov-uk-identifiers", "openid:hmrc-enrolments")
      when(mockThirdPartyDelegatedAuthorityConnector.fetchScopes(eqTo(accessToken))(eqTo(headers), any[ExecutionContext]))
        .thenReturn(Future.successful(scopes))
      when(mockAuthConnector.fetchAuthority()(eqTo(headers), any[ExecutionContext])).thenReturn(Future(Some(authority)))
      when(mockAuthConnector.fetchEnrolments()(eqTo(headers), any[ExecutionContext])).thenReturn(Future(Some(enrolments)))
      when(mockUserInfoTransformer.transform(eqTo(scopes), eqTo(Some(authority)), eqTo(None), eqTo(Some(enrolments)), eqTo(None), eqTo(None)))
        .thenReturn(UserInfo())

      await(liveInfoService.fetchUserInfo(Version_1_0))

      verify(mockAuthConnector, never).fetchDesUserInfo()(any[HeaderCarrier], any[ExecutionContext])
      verify(mockAuthConnector).fetchEnrolments()
      verify(mockAuthConnector, never).fetchDetails()(any[HeaderCarrier], any[ExecutionContext])
    }

    "does not request AUTH::fetchNino nor DES::fetchUserInfo when the scopes does not contain 'address' nor 'profile' nor 'openid:gov-uk-identifiers'" in new Setup {

      val scopes = Set("openid:hmrc-enrolments")
      when(mockThirdPartyDelegatedAuthorityConnector.fetchScopes(eqTo(accessToken))(eqTo(headers), any[ExecutionContext]))
        .thenReturn(Future.successful(scopes))

      when(mockAuthConnector.fetchAuthority()(eqTo(headers), any[ExecutionContext])).thenReturn(Future(Some(authority)))
      when(mockAuthConnector.fetchEnrolments()(eqTo(headers), any[ExecutionContext])).thenReturn(Future.successful(Some(enrolments)))
      when(mockUserInfoTransformer.transform(eqTo(scopes), eqTo(Some(authority)), eqTo(None), eqTo(Some(enrolments)), eqTo(None), eqTo(None)))
        .thenReturn(UserInfo())

      await(liveInfoService.fetchUserInfo(Version_1_0))

      verify(mockAuthConnector, never).fetchDesUserInfo()(any[HeaderCarrier], any[ExecutionContext])
      verify(mockAuthConnector).fetchEnrolments()
      verify(mockAuthConnector, never).fetchDetails()(any[HeaderCarrier], any[ExecutionContext])
    }

    "does not request AUTH::fetchEnrolments when the scopes does not contain 'openid:hmrc-enrolments'" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers")
      when(mockThirdPartyDelegatedAuthorityConnector.fetchScopes(eqTo(accessToken))(eqTo(headers), any[ExecutionContext]))
        .thenReturn(Future.successful(scopes))

      when(mockAuthConnector.fetchAuthority()(eqTo(headers), any[ExecutionContext])).thenReturn(Future(Some(authority)))
      when(mockAuthConnector.fetchDesUserInfo()(eqTo(headers), any[ExecutionContext])).thenReturn(Future.successful(None))
      when(mockUserInfoTransformer.transform(eqTo(scopes), eqTo(Some(authority)), eqTo(None), eqTo(None), eqTo(None), eqTo(None)))
        .thenReturn(UserInfo())

      await(liveInfoService.fetchUserInfo(Version_1_0))

      verify(mockAuthConnector, never).fetchEnrolments()(any[HeaderCarrier], any[ExecutionContext])
      verify(mockAuthConnector).fetchDesUserInfo()(any[HeaderCarrier], any[ExecutionContext])
      verify(mockAuthConnector, never).fetchDetails()(any[HeaderCarrier], any[ExecutionContext])
    }

    "does not request TrustedHelperConnector::getDelegation when the scopes does not contain 'openid:trusted-helper'" in new Setup {

      val scopes = Set("openid:gov-uk-identifiers", "openid:hmrc-enrolments")
      when(mockThirdPartyDelegatedAuthorityConnector.fetchScopes(eqTo(accessToken))(eqTo(headers), any[ExecutionContext]))
        .thenReturn(Future.successful(scopes))
      when(mockAuthConnector.fetchAuthority()(eqTo(headers), any[ExecutionContext])).thenReturn(Future(Some(authority)))
      when(mockAuthConnector.fetchEnrolments()(eqTo(headers), any[ExecutionContext])).thenReturn(Future(Some(enrolments)))
      when(mockUserInfoTransformer.transform(eqTo(scopes), eqTo(Some(authority)), eqTo(None), eqTo(Some(enrolments)), eqTo(None), eqTo(None)))
        .thenReturn(UserInfo())

      await(liveInfoService.fetchUserInfo(Version_1_0))

      verify(mockTrustedHelperConnector, never).getDelegation()(any[HeaderCarrier], any[ExecutionContext])
    }
  }

  "SandboxUserInfoService" should {
    "return generated UserInfo v1.0" in new Setup {
      when(mockUserInfoGenerator.userInfoV1_0()).thenReturn(userInfo)

      val result: UserInfo = await(sandboxInfoService.fetchUserInfo(Version_1_0))

      result shouldBe userInfo
    }
  }
}
