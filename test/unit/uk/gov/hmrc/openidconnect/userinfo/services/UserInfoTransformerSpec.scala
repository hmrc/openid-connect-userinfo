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
import uk.gov.hmrc.play.http.{Token, HeaderCarrier}
import uk.gov.hmrc.play.http.logging.Authorization
import uk.gov.hmrc.play.test.UnitSpec

class UserInfoTransformerSpec extends UnitSpec with MockitoSugar {

  val ukCountryCode = 10
  val nino = "AB123456A"
  val authBearerToken = "AUTH_BEARER_TOKEN"
  val desUserInfo = DesUserInfo(DesUserName(Some("John"), Some("A"), Some("Smith")), Some(LocalDate.parse("1980-01-01")),
    DesAddress(Some("1 Station Road"), Some("Town Centre"), Some("London"), Some("England"), Some("NW1 6XE"), Some(ukCountryCode)))

  val userInfo = UserInfo(
    Some("John"),
    Some("Smith"),
    Some("A"),
    Some(Address("1 Station Road\nTown Centre\nLondon\nEngland\nNW1 6XE\nUnited Kingdom", Some("NW1 6XE"), Some("United Kingdom"))),
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
  }
}
