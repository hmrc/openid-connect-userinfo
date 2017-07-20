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

import org.joda.time.LocalDate
import org.mockito.BDDMockito.given
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.openidconnect.userinfo.config.{FeatureSwitch, UserInfoFeatureSwitches}
import uk.gov.hmrc.openidconnect.userinfo.domain._
import uk.gov.hmrc.openidconnect.userinfo.services.{CountryService, UserInfoTransformer}
import uk.gov.hmrc.play.http.{HeaderCarrier, Token}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class UserInfoTransformerSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val ukCountryCode = 10
  val nino = Nino("AB123456A")
  val desAddress: DesAddress = DesAddress(Some("1 Station Road"), Some("Town Centre"), Some("London"), Some("England"), Some("UK"), Some("NW1 6XE"), Some(ukCountryCode))
  val desUserInfo = DesUserInfo(DesUserName(Some("John"), Some("A"), Some("Smith")), Some(LocalDate.parse("1980-01-01")), desAddress)
  val enrolments = Seq(Enrolment("IR-SA", List(EnrolmentIdentifier("UTR", "174371121"))))

  val userAddress: Address = Address("1 Station Road\nTown Centre\nLondon\nEngland\nUK\nNW1 6XE\nUnited Kingdom\nGB", Some("NW1 6XE"), Some("United Kingdom"), Some("GB"))

  val authority: Authority = Authority(Some("weak"), Some(200), Some("AB123456A"), Some("/uri/to/userDetails"),
    Some("/uri/to/enrolments"), Some("Individual"), Some("1304372065861347"))

  val userDetails: UserDetails = UserDetails(None, None, None, None, None, None, Some("John.Smith@a.b.c.com"), Some("affinityGroup"), None, None,
    Some("User"), None, None)

  val ggToken = Token("ggToken")

  val government_gateway: GovernmentGatewayDetails = GovernmentGatewayDetails(Some("1304372065861347"), Some(Seq("User")), Some("affinityGroup"))

  val userInfo = UserInfo(
    Some("John"),
    Some("Smith"),
    Some("A"),
    Some(userAddress),
    Some("John.Smith@a.b.c.com"),
    Some(LocalDate.parse("1980-01-01")),
    Some("AB123456A"),
    Some(enrolments),
    Some(government_gateway)
  )


  override protected def beforeEach() = {
    FeatureSwitch.enable(UserInfoFeatureSwitches.countryCode)
    FeatureSwitch.enable(UserInfoFeatureSwitches.addressLine5)
  }

  override protected def afterEach() = {
    FeatureSwitch.disable(UserInfoFeatureSwitches.countryCode)
    FeatureSwitch.disable(UserInfoFeatureSwitches.addressLine5)
  }

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val transformer = new UserInfoTransformer {
      override val countryService = mock[CountryService]
    }
    given(transformer.countryService.getCountry(ukCountryCode)).willReturn(Some(Country(Some("United Kingdom"), Some("GB"))))
  }

  "transform" should {

    "return the full object when the delegated authority has scope 'address', 'profile', 'openid:gov-uk-identifiers', 'openid:hrmc_enrolments', 'email' and 'openid:government_gateway'" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers", "openid:hmrc_enrolments", "openid:government_gateway"
        , "email")

      val result = await(transformer.transform(scopes, Some(desUserInfo), Some(enrolments), Some(authority), Some(userDetails)))

      result shouldBe userInfo
    }

    "return only the nino when des user info could not be retrieved" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers")

      val result = await(transformer.transform(scopes, None, None, Option(authority), None))

      result shouldBe UserInfo(None, None, None, None, None, None, Some(nino.map(_.nino)), None, None)
    }

    "does not return the address when the delegated authority does not have the scope 'address'" in new Setup {

      val scopes = Set("profile", "openid:gov-uk-identifiers", "openid:hmrc_enrolments", "openid:government_gateway", "email")

      val result = await(transformer.transform(scopes, Some(desUserInfo), Some(enrolments), Some(authority), Some(userDetails)))

      result shouldBe userInfo.copy(address = None)
    }


    "does not return the enrolments when the delegated authority does not have the scope 'openid:hmrc_enrolments'" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers", "openid:government_gateway", "email")

      val result = await(transformer.transform(scopes, Some(desUserInfo), Some(enrolments), Some(authority), Some(userDetails)))

      result shouldBe userInfo.copy(hmrc_enrolments = None)
    }

    "does not return the user profile when the delegated authority does not have the scope 'profile'" in new Setup {

      val scopes = Set("address", "openid:gov-uk-identifiers", "openid:hmrc_enrolments", "openid:government_gateway", "email")

      val result = await(transformer.transform(scopes, Some(desUserInfo), Some(enrolments), Some(authority), Some(userDetails)))

      result shouldBe userInfo.copy(given_name = None, family_name = None, middle_name = None, birthdate = None)
    }

    "does not return the nino when the delegated authority does not have the scope 'openid:gov-uk-identifiers', 'openid:hmrc_enrolments'" in new Setup {

      val scopes = Set("address", "profile", "openid:hmrc_enrolments", "openid:government_gateway", "email")

      val result = await(transformer.transform(scopes, Some(desUserInfo), Some(enrolments), Some(authority), Some(userDetails)))

      result shouldBe userInfo.copy(uk_gov_nino = None)
    }

    "return an empty object when the delegated authority does have only 'openid' scope" in new Setup {

      val scopes = Set("openid")

      val result = await(transformer.transform(scopes, Some(desUserInfo), Some(enrolments), Some(authority), Some(userDetails)))

      result shouldBe UserInfo(None, None, None, None, None, None, None, None, None)
    }

    "handle missing first line of address" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers", "openid:hmrc_enrolments", "openid:government_gateway", "email")

      val desUserMissingline1 = desUserInfo.copy(address = desAddress.copy(line1 = None))
      val result = await(transformer.transform(scopes, Some(desUserMissingline1), Some(enrolments), Some(authority), Some(userDetails)))
      val userInfoMissingLine1 = userInfo.copy(address = Some(userAddress.copy(formatted = "Town Centre\nLondon\nEngland\nUK\nNW1 6XE\nUnited Kingdom\nGB")))
      result shouldBe userInfoMissingLine1
    }

    "handle missing second line of address" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers", "openid:hmrc_enrolments", "openid:government_gateway", "email")

      val desUserMissingLine2 = desUserInfo.copy(address = desAddress.copy(line2 = None))
      val result = await(transformer.transform(scopes, Some(desUserMissingLine2), Some(enrolments), Some(authority), Some(userDetails)))
      val userInfoMissingLine2 = userInfo.copy(address = Some(userAddress.copy(formatted = "1 Station Road\nLondon\nEngland\nUK\nNW1 6XE\nUnited Kingdom\nGB")))
      result shouldBe userInfoMissingLine2
    }

    "handle missing third line of address" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers", "openid:hmrc_enrolments", "openid:government_gateway", "email")

      val desUserMissingLine3 = desUserInfo.copy(address = desAddress.copy(line3 = None))
      val result = await(transformer.transform(scopes, Some(desUserMissingLine3), Some(enrolments), Some(authority), Some(userDetails)))
      val userInfoMissingLine3 = userInfo.copy(address = Some(userAddress.copy(formatted = "1 Station Road\nTown Centre\nEngland\nUK\nNW1 6XE\nUnited Kingdom\nGB")))
      result shouldBe userInfoMissingLine3
    }

    "handle missing fourth line of address" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers", "openid:government_gateway", "email")

      val desUserMissingLine4 = desUserInfo.copy(address = desAddress.copy(line4 = None))
      val result = await(transformer.transform(scopes, Some(desUserMissingLine4), None, Some(authority), Some(userDetails)))
      val userInfoMissingLine4 = userInfo.copy(address = Some(userAddress.copy(formatted = "1 Station Road\nTown Centre\nLondon\nUK\nNW1 6XE\nUnited Kingdom\nGB")), hmrc_enrolments = None)
      result shouldBe userInfoMissingLine4
    }

    "handle missing fifth line of address" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers", "openid:government_gateway", "email")

      val desUserMissingLine5 = desUserInfo.copy(address = desAddress.copy(line5 = None))
      val result = await(transformer.transform(scopes, Some(desUserMissingLine5), None, Some(authority), Some(userDetails)))
      val userInfoMissingLine5 = userInfo.copy(address = Some(userAddress.copy(formatted = "1 Station Road\nTown Centre\nLondon\nEngland\nNW1 6XE\nUnited Kingdom\nGB")), hmrc_enrolments = None)
      result shouldBe userInfoMissingLine5
    }

    "handle missing post code in address" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers", "openid:government_gateway")

      val desUserMissingPostCode = desUserInfo.copy(address = desAddress.copy(postcode = None))
      val result = await(transformer.transform(scopes, Some(desUserMissingPostCode), None, Some(authority), Some(userDetails)))
      val userInfoMissingPostCode = userInfo.copy(address = Some(userAddress.copy(formatted = "1 Station Road\nTown Centre\nLondon\nEngland\nUK\nUnited Kingdom\nGB", postal_code = None)), hmrc_enrolments = None, email = None)
      result shouldBe userInfoMissingPostCode
    }

    "not return country code when feature flag is off" in new Setup {

      FeatureSwitch.disable(UserInfoFeatureSwitches.countryCode)
      val scopes = Set("address", "profile", "openid:gov-uk-identifiers", "openid:hmrc_enrolments", "openid:government_gateway", "email")
      val result = await(transformer.transform(scopes, Some(desUserInfo), Some(enrolments), Some(authority), Some(userDetails)))

      val userInfoMissingCountryCode = userInfo.copy(address = Some(userAddress.copy(formatted = "1 Station Road\nTown Centre\nLondon\nEngland\nUK\nNW1 6XE\nUnited Kingdom",  country_code = None)))
      result shouldBe userInfoMissingCountryCode
    }

  }
}
