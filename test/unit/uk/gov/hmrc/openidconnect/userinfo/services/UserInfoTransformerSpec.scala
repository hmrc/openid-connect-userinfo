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

import org.joda.time.LocalDate
import org.mockito.BDDMockito.given
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.openidconnect.userinfo.connectors.ThirdPartyDelegatedAuthorityConnector
import uk.gov.hmrc.openidconnect.userinfo.domain._
import uk.gov.hmrc.openidconnect.userinfo.services.{CountryService, UserInfoTransformer}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.Authorization
import uk.gov.hmrc.play.test.UnitSpec

class UserInfoTransformerSpec extends UnitSpec with MockitoSugar {

  val ukCountryCode = 10
  val nino = "AB123456A"
  val authBearerToken = "AUTH_BEARER_TOKEN"
  val desAddress: DesAddress = DesAddress(Some("1 Station Road"), Some("Town Centre"), Some("London"), Some("England"), Some("NW1 6XE"), Some(ukCountryCode))
  val desUserInfo = DesUserInfo(DesUserName(Some("John"), Some("A"), Some("Smith")), Some(LocalDate.parse("1980-01-01")), desAddress)

  val userAddress: Address = Address("1 Station Road\nTown Centre\nLondon\nEngland\nNW1 6XE\nUnited Kingdom", Some("NW1 6XE"), Some("United Kingdom"))
  val userInfo = UserInfo(
    Some("John"),
    Some("Smith"),
    Some("A"),
    Some(userAddress),
    Some(LocalDate.parse("1980-01-01")),
    Some("AB123456A"))

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier().copy(authorization = Some(Authorization(s"Bearer $authBearerToken")))

    val transformer = new UserInfoTransformer {
      override val countryService = mock[CountryService]
      override val thirdPartyDelegatedAuthorityConnector = mock[ThirdPartyDelegatedAuthorityConnector]
    }
    given(transformer.countryService.getCountry(ukCountryCode)).willReturn(Some("United Kingdom"))
  }

  "transform" should {

    "return the full object when the delegated authority has scope 'address', 'profile' and 'openid:gov-uk-identifiers'" in new Setup {

      given(transformer.thirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(hc)).willReturn(Set("address", "profile", "openid:gov-uk-identifiers"))

      val result = await(transformer.transform(Some(desUserInfo), nino))

      result shouldBe userInfo
    }

    "return only the nino when des user info could not be retrieved" in new Setup {

      given(transformer.thirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(hc)).willReturn(Set("address", "profile", "openid:gov-uk-identifiers"))

      val result = await(transformer.transform(None, nino))

      result shouldBe UserInfo(None, None, None, None, None, Some(nino))
    }

    "does not return the address when the delegated authority does not have the scope 'address'" in new Setup {

      given(transformer.thirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(hc)).willReturn(Set("profile", "openid:gov-uk-identifiers"))

      val result = await(transformer.transform(Some(desUserInfo), nino))

      result shouldBe userInfo.copy(address = None)
    }

    "does not return the user profile when the delegated authority does not have the scope 'profile'" in new Setup {

      given(transformer.thirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(hc)).willReturn(Set("address", "openid:gov-uk-identifiers"))

      val result = await(transformer.transform(Some(desUserInfo), nino))

      result shouldBe userInfo.copy(given_name = None, family_name = None, middle_name = None, birthdate = None)
    }

    "does not return the nino when the delegated authority does not have the scope 'openid:gov-uk-identifiers'" in new Setup {

      given(transformer.thirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(hc)).willReturn(Set("address", "profile"))

      val result = await(transformer.transform(Some(desUserInfo), nino))

      result shouldBe userInfo.copy(uk_gov_nino = None)
    }

    "return an empty object when the delegated authority does have only 'openid' scope" in new Setup {

      given(transformer.thirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(hc)).willReturn(Set("openid"))

      val result = await(transformer.transform(Some(desUserInfo), nino))

      result shouldBe UserInfo(None, None, None, None, None, None)
    }

    "handle missing first line of address" in new Setup {

      given(transformer.thirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(hc)).willReturn(Set("address", "profile", "openid:gov-uk-identifiers"))

      val desUserMissingline1 = desUserInfo.copy(address = desAddress.copy(line1=None))
      val result = await(transformer.transform(Some(desUserMissingline1), nino))
      val userInfoMissingLine1 = userInfo.copy(address = Some(userAddress.copy(formatted = "Town Centre\nLondon\nEngland\nNW1 6XE\nUnited Kingdom")))
      result shouldBe userInfoMissingLine1
    }

    "handle missing second line of address" in new Setup {

      given(transformer.thirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(hc)).willReturn(Set("address", "profile", "openid:gov-uk-identifiers"))

      val desUserMissingLine2 = desUserInfo.copy(address = desAddress.copy(line2=None))
      val result = await(transformer.transform(Some(desUserMissingLine2), nino))
      val userInfoMissingLine2 = userInfo.copy(address = Some(userAddress.copy(formatted = "1 Station Road\nLondon\nEngland\nNW1 6XE\nUnited Kingdom")))
      result shouldBe userInfoMissingLine2
    }

    "handle missing third line of address" in new Setup {

      given(transformer.thirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(hc)).willReturn(Set("address", "profile", "openid:gov-uk-identifiers"))

      val desUserMissingLine3 = desUserInfo.copy(address = desAddress.copy(line3=None))
      val result = await(transformer.transform(Some(desUserMissingLine3), nino))
      val userInfoMissingLine3 = userInfo.copy(address = Some(userAddress.copy(formatted = "1 Station Road\nTown Centre\nEngland\nNW1 6XE\nUnited Kingdom")))
      result shouldBe userInfoMissingLine3
    }

    "handle missing fourth line of address" in new Setup {

      given(transformer.thirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(hc)).willReturn(Set("address", "profile", "openid:gov-uk-identifiers"))

      val desUserMissingLine4 = desUserInfo.copy(address = desAddress.copy(line4=None))
      val result = await(transformer.transform(Some(desUserMissingLine4), nino))
      val userInfoMissingLine4 = userInfo.copy(address = Some(userAddress.copy(formatted = "1 Station Road\nTown Centre\nLondon\nNW1 6XE\nUnited Kingdom")))
      result shouldBe userInfoMissingLine4
    }

    "handle missing post code in address" in new Setup {

      given(transformer.thirdPartyDelegatedAuthorityConnector.fetchScopes(authBearerToken)(hc)).willReturn(Set("address", "profile", "openid:gov-uk-identifiers"))

      val desUserMissingPostCode = desUserInfo.copy(address = desAddress.copy(postcode = None))
      val result = await(transformer.transform(Some(desUserMissingPostCode), nino))
      val userInfoMissingPostCode = userInfo.copy(address = Some(userAddress.copy(formatted = "1 Station Road\nTown Centre\nLondon\nEngland\nUnited Kingdom",
                                                                                  postal_code = None)))
      result shouldBe userInfoMissingPostCode
    }
  }
}
