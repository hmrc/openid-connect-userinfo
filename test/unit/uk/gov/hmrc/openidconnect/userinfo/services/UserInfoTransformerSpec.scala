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

import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.LocalDate
import org.mockito.BDDMockito.given
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.openidconnect.userinfo.domain._
import uk.gov.hmrc.openidconnect.userinfo.services.{CountryService, UserInfoTransformer}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

class UserInfoTransformerSpec extends UnitSpec with MockitoSugar {

  val ukCountryCode = 10
  val nino = Nino("AB123456A")
  val desAddress: DesAddress = DesAddress(Some("1 Station Road"), Some("Town Centre"), Some("London"), Some("England"), Some("NW1 6XE"), Some(ukCountryCode))
  val desUserInfo = DesUserInfo(DesUserName(Some("John"), Some("A"), Some("Smith")), Some(LocalDate.parse("1980-01-01")), desAddress)
  val enrolments = Seq(Enrolment("IR-SA", List(EnrolmentIdentifier("UTR", "174371121"))))

  val userAddress: Address = Address("1 Station Road\nTown Centre\nLondon\nEngland\nNW1 6XE\nUnited Kingdom", Some("NW1 6XE"), Some("United Kingdom"))
  val userInfo = UserInfo(
    Some("John"),
    Some("Smith"),
    Some("A"),
    Some(userAddress),
    Some(LocalDate.parse("1980-01-01")),
    Some("AB123456A"),
    Some(enrolments)
  )

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val transformer = new UserInfoTransformer {
      override val countryService = mock[CountryService]
    }
    given(transformer.countryService.getCountry(ukCountryCode)).willReturn(Some("United Kingdom"))
  }

  "transform" should {

    "return the full object when the delegated authority has scope 'address', 'profile', 'openid:gov-uk-identifiers' and 'openid:hrmc_enrolments'" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers", "openid:hmrc_enrolments")

      val result = await(transformer.transform(scopes, Some(desUserInfo), Some(nino), Some(enrolments)))

      result shouldBe userInfo
    }

    "return only the nino when des user info could not be retrieved" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers")

      val result = await(transformer.transform(scopes, None, Some(nino), None))

      result shouldBe UserInfo(None, None, None, None, None, Some(nino.map(_.nino)), None)
    }

    "does not return the address when the delegated authority does not have the scope 'address'" in new Setup {

      val scopes = Set("profile", "openid:gov-uk-identifiers", "openid:hmrc_enrolments")

      val result = await(transformer.transform(scopes, Some(desUserInfo), Some(nino), Some(enrolments)))

      result shouldBe userInfo.copy(address = None)
    }


    "does not return the enrolments when the delegated authority does not have the scope 'openid:hmrc_enrolments'" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers")

      val result = await(transformer.transform(scopes, Some(desUserInfo), Some(nino), Some(enrolments)))

      result shouldBe userInfo.copy(hmrc_enrolments = None)
    }

    "does not return the user profile when the delegated authority does not have the scope 'profile'" in new Setup {

      val scopes = Set("address", "openid:gov-uk-identifiers", "openid:hmrc_enrolments")

      val result = await(transformer.transform(scopes, Some(desUserInfo), Some(nino), Some(enrolments)))

      result shouldBe userInfo.copy(given_name = None, family_name = None, middle_name = None, birthdate = None)
    }

    "does not return the nino when the delegated authority does not have the scope 'openid:gov-uk-identifiers', 'openid:hmrc_enrolments'" in new Setup {

      val scopes = Set("address", "profile", "openid:hmrc_enrolments")

      val result = await(transformer.transform(scopes, Some(desUserInfo), Some(nino), Some(enrolments)))

      result shouldBe userInfo.copy(uk_gov_nino = None)
    }

    "return an empty object when the delegated authority does have only 'openid' scope" in new Setup {

      val scopes = Set("openid")

      val result = await(transformer.transform(scopes, Some(desUserInfo), Some(nino), Some(enrolments)))

      result shouldBe UserInfo(None, None, None, None, None, None, None)
    }

    "handle missing first line of address" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers", "openid:hmrc_enrolments")

      val desUserMissingline1 = desUserInfo.copy(address = desAddress.copy(line1=None))
      val result = await(transformer.transform(scopes, Some(desUserMissingline1), Some(nino), Some(enrolments)))
      val userInfoMissingLine1 = userInfo.copy(address = Some(userAddress.copy(formatted = "Town Centre\nLondon\nEngland\nNW1 6XE\nUnited Kingdom")))
      result shouldBe userInfoMissingLine1
    }

    "handle missing second line of address" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers", "openid:hmrc_enrolments")

      val desUserMissingLine2 = desUserInfo.copy(address = desAddress.copy(line2=None))
      val result = await(transformer.transform(scopes, Some(desUserMissingLine2), Some(nino),Some(enrolments)))
      val userInfoMissingLine2 = userInfo.copy(address = Some(userAddress.copy(formatted = "1 Station Road\nLondon\nEngland\nNW1 6XE\nUnited Kingdom")))
      result shouldBe userInfoMissingLine2
    }

    "handle missing third line of address" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers", "openid:hmrc_enrolments")

      val desUserMissingLine3 = desUserInfo.copy(address = desAddress.copy(line3=None))
      val result = await(transformer.transform(scopes, Some(desUserMissingLine3), Some(nino),Some(enrolments)))
      val userInfoMissingLine3 = userInfo.copy(address = Some(userAddress.copy(formatted = "1 Station Road\nTown Centre\nEngland\nNW1 6XE\nUnited Kingdom")))
      result shouldBe userInfoMissingLine3
    }

    "handle missing fourth line of address" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers")

      val desUserMissingLine4 = desUserInfo.copy(address = desAddress.copy(line4=None))
      val result = await(transformer.transform(scopes, Some(desUserMissingLine4), Some(nino),None))
      val userInfoMissingLine4 = userInfo.copy(address = Some(userAddress.copy(formatted = "1 Station Road\nTown Centre\nLondon\nNW1 6XE\nUnited Kingdom")),hmrc_enrolments = None)
      result shouldBe userInfoMissingLine4
    }

    "handle missing post code in address" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers")

      val desUserMissingPostCode = desUserInfo.copy(address = desAddress.copy(postcode = None))
      val result = await(transformer.transform(scopes, Some(desUserMissingPostCode), Some(nino),None))
      val userInfoMissingPostCode = userInfo.copy(address = Some(userAddress.copy(formatted = "1 Station Road\nTown Centre\nLondon\nEngland\nUnited Kingdom",postal_code = None)), hmrc_enrolments = None)
      result shouldBe userInfoMissingPostCode
    }
  }
}
