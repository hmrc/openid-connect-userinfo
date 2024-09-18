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

import domain._

import java.time.LocalDate
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import testSupport.UnitSpec
import uk.gov.hmrc.auth.core.retrieve.{GatewayInformation, ItmpAddress, ItmpName, MdtpInformation}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class UserInfoTransformerSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val ukCountryCode = 10
  val nino = Nino("AB123456A")
  val desAddress: ItmpAddress = ItmpAddress(Some("1 Station Road"),
                                            Some("Town Centre"),
                                            Some("London"),
                                            Some("England"),
                                            Some("UK"),
                                            Some("NW1 6XE"),
                                            Some("United Kingdom"),
                                            Some("GB")
                                           )
  val desUserInfo = DesUserInfo(Some(ItmpName(Some("John"), Some("A"), Some("Smith"))), Some(LocalDate.parse("1980-01-01")), Some(desAddress))
  val enrolments = Enrolments(Set(Enrolment("IR-SA", List(EnrolmentIdentifier("UTR", "174371121")), "Activated")))

  val gatewayInformation = GatewayInformation(Some("gateway-token-abc"))
  val mdtp = Mdtp("device-id1234", "session-id-123")
  val authMdtp = MdtpInformation("device-id1234", "session-id-123")
  val userAddress: Address =
    Address("1 Station Road\nTown Centre\nLondon\nEngland\nUK\nNW1 6XE\nUnited Kingdom", Some("NW1 6XE"), Some("United Kingdom"), Some("GB"))

  val authority: Authority = Authority("1304372065861347", Some("AB123456A"))

  val userDetails: UserDetails = UserDetails(
    email              = Some("John.Smith@a.b.c.com"),
    affinityGroup      = Some("affinityGroup"),
    name               = Some("John"),
    credentialRole     = Some("User"),
    agentCode          = Some("agent-code-12345"),
    agentId            = Some("agent-id-12345"),
    agentFriendlyName  = Some("agent-friendly-name"),
    gatewayInformation = Some(gatewayInformation),
    mdtpInformation    = Some(authMdtp),
    profile            = None,
    groupProfile       = None
  )

  val government_gateway: GovernmentGatewayDetails = GovernmentGatewayDetails(
    Some("1304372065861347"),
    Some(Seq("User")),
    Some("John"),
    Some("affinityGroup"),
    userDetails.agentCode,
    agent_id             = userDetails.agentId,
    agent_friendly_name  = userDetails.agentFriendlyName,
    gateway_token        = Some("gateway-token-abc"),
    unread_message_count = None,
    profile_uri          = None,
    group_profile_uri    = None
  )

  val userInfo = UserInfo(
    Some("John"),
    Some("Smith"),
    Some("A"),
    Some(userAddress),
    Some("John.Smith@a.b.c.com"),
    Some(LocalDate.parse("1980-01-01")),
    Some("AB123456A"),
    Some(enrolments.enrolments),
    Some(government_gateway),
    Some(mdtp)
  )

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val transformer = new UserInfoTransformer {}
  }

  "transform" should {

    "return the full object when the delegated authority has scope 'address', 'profile', 'openid:gov-uk-identifiers', 'openid:hrmc_enrolments', 'email' and 'openid:government-gateway'" in new Setup {

      val scopes =
        Set("address", "profile", "openid:gov-uk-identifiers", "openid:hmrc-enrolments", "openid:government-gateway", "email", "openid:mdtp")

      val result = await(transformer.transform(scopes, Some(authority), Some(desUserInfo), Some(enrolments), Some(userDetails)))

      result shouldBe userInfo
    }

    "return only the nino when des user info could not be retrieved" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers")

      val result = await(transformer.transform(scopes, Some(authority), None, None, None))

      result shouldBe UserInfo(None, None, None, None, None, None, Some(nino.map(_.nino)), None, None, None)
    }

    "does not return the address when the delegated authority does not have the scope 'address'" in new Setup {

      val scopes = Set("profile", "openid:gov-uk-identifiers", "openid:hmrc-enrolments", "openid:government-gateway", "email", "openid:mdtp")

      val result = await(transformer.transform(scopes, Some(authority), Some(desUserInfo), Some(enrolments), Some(userDetails)))

      result shouldBe userInfo.copy(address = None)
    }

    "does not return the enrolments when the delegated authority does not have the scope 'openid:hmrc-enrolments'" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers", "openid:government-gateway", "email", "openid:mdtp")

      val result = await(transformer.transform(scopes, Some(authority), Some(desUserInfo), Some(enrolments), Some(userDetails)))

      result shouldBe userInfo.copy(hmrc_enrolments = None)
    }

    "does not return the user profile when the delegated authority does not have the scope 'profile'" in new Setup {

      val scopes = Set("address", "openid:gov-uk-identifiers", "openid:hmrc-enrolments", "openid:government-gateway", "email", "openid:mdtp")

      val result = await(transformer.transform(scopes, Some(authority), Some(desUserInfo), Some(enrolments), Some(userDetails)))

      result shouldBe userInfo.copy(given_name = None, family_name = None, middle_name = None, birthdate = None)
    }

    "does not return the nino when the delegated authority does not have the scope 'openid:gov-uk-identifiers', 'openid:hmrc-enrolments'" in new Setup {

      val scopes = Set("address", "profile", "openid:hmrc-enrolments", "openid:government-gateway", "email", "openid:mdtp")

      val result = await(transformer.transform(scopes, Some(authority), Some(desUserInfo), Some(enrolments), Some(userDetails)))

      result shouldBe userInfo.copy(uk_gov_nino = None)
    }

    "return an empty object when the delegated authority does have only 'openid' scope" in new Setup {

      val scopes = Set("openid")

      val result = await(transformer.transform(scopes, Some(authority), Some(desUserInfo), Some(enrolments), Some(userDetails)))

      result shouldBe UserInfo(None, None, None, None, None, None, None, None, None, None)
    }

    "handle missing first line of address" in new Setup {

      val scopes =
        Set("address", "profile", "openid:gov-uk-identifiers", "openid:hmrc-enrolments", "openid:government-gateway", "email", "openid:mdtp")

      val desUserMissingline1 = desUserInfo.copy(address = Some(desAddress.copy(line1 = None)))
      val result = await(transformer.transform(scopes, Some(authority), Some(desUserMissingline1), Some(enrolments), Some(userDetails)))
      val userInfoMissingLine1 =
        userInfo.copy(address = Some(userAddress.copy(formatted = "Town Centre\nLondon\nEngland\nUK\nNW1 6XE\nUnited Kingdom")))
      result shouldBe userInfoMissingLine1
    }

    "handle missing second line of address" in new Setup {

      val scopes =
        Set("address", "profile", "openid:gov-uk-identifiers", "openid:hmrc-enrolments", "openid:government-gateway", "email", "openid:mdtp")

      val desUserMissingLine2 = desUserInfo.copy(address = Some(desAddress.copy(line2 = None)))
      val result = await(transformer.transform(scopes, Some(authority), Some(desUserMissingLine2), Some(enrolments), Some(userDetails)))
      val userInfoMissingLine2 =
        userInfo.copy(address = Some(userAddress.copy(formatted = "1 Station Road\nLondon\nEngland\nUK\nNW1 6XE\nUnited Kingdom")))
      result shouldBe userInfoMissingLine2
    }

    "handle missing third line of address" in new Setup {

      val scopes =
        Set("address", "profile", "openid:gov-uk-identifiers", "openid:hmrc-enrolments", "openid:government-gateway", "email", "openid:mdtp")

      val desUserMissingLine3 = desUserInfo.copy(address = Some(desAddress.copy(line3 = None)))
      val result = await(transformer.transform(scopes, Some(authority), Some(desUserMissingLine3), Some(enrolments), Some(userDetails)))
      val userInfoMissingLine3 =
        userInfo.copy(address = Some(userAddress.copy(formatted = "1 Station Road\nTown Centre\nEngland\nUK\nNW1 6XE\nUnited Kingdom")))
      result shouldBe userInfoMissingLine3
    }

    "handle missing fourth line of address" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers", "openid:government-gateway", "email", "openid:mdtp")

      val desUserMissingLine4 = desUserInfo.copy(address = Some(desAddress.copy(line4 = None)))
      val result = await(transformer.transform(scopes, Some(authority), Some(desUserMissingLine4), None, Some(userDetails)))
      val userInfoMissingLine4 =
        userInfo.copy(address         = Some(userAddress.copy(formatted = "1 Station Road\nTown Centre\nLondon\nUK\nNW1 6XE\nUnited Kingdom")),
                      hmrc_enrolments = None
                     )
      result shouldBe userInfoMissingLine4
    }

    "handle missing fifth line of address" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers", "openid:government-gateway", "email", "openid:mdtp")

      val desUserMissingLine5 = desUserInfo.copy(address = Some(desAddress.copy(line5 = None)))
      val result = await(transformer.transform(scopes, Some(authority), Some(desUserMissingLine5), None, Some(userDetails)))
      val userInfoMissingLine5 =
        userInfo.copy(address         = Some(userAddress.copy(formatted = "1 Station Road\nTown Centre\nLondon\nEngland\nNW1 6XE\nUnited Kingdom")),
                      hmrc_enrolments = None
                     )
      result shouldBe userInfoMissingLine5
    }

    "handle missing post code in address" in new Setup {

      val scopes = Set("address", "profile", "openid:gov-uk-identifiers", "openid:government-gateway", "openid:mdtp")

      val desUserMissingPostCode = desUserInfo.copy(address = Some(desAddress.copy(postCode = None)))
      val result = await(transformer.transform(scopes, Some(authority), Some(desUserMissingPostCode), None, Some(userDetails)))
      val userInfoMissingPostCode = userInfo.copy(
        address         = Some(userAddress.copy(formatted = "1 Station Road\nTown Centre\nLondon\nEngland\nUK\nUnited Kingdom", postal_code = None)),
        hmrc_enrolments = None,
        email           = None
      )
      result shouldBe userInfoMissingPostCode
    }

    "return object containing country code if country code is defined in DES response" in new Setup {

      val scopes =
        Set("address", "profile", "openid:gov-uk-identifiers", "openid:hmrc-enrolments", "openid:government-gateway", "email", "openid:mdtp")
      val result = await(transformer.transform(scopes, Some(authority), Some(desUserInfo), Some(enrolments), Some(userDetails)))

      result.address                    should be(defined)
      result.address.get.country_code shouldBe desUserInfo.address.flatMap(_.countryCode)
    }

    "return object not containing country code if country code isn't defined in DES response" in new Setup {

      val scopes =
        Set("address", "profile", "openid:gov-uk-identifiers", "openid:hmrc-enrolments", "openid:government-gateway", "email", "openid:mdtp")
      val result = await(
        transformer.transform(scopes,
                              Some(authority),
                              Some(desUserInfo.copy(address = Some(desAddress.copy(countryCode = None)))),
                              Some(enrolments),
                              Some(userDetails)
                             )
      )

      result.address                    should be(defined)
      result.address.get.country_code shouldBe None
    }

    "return object containing line 5 if line 5 is defined in DES response" in new Setup {

      val scopes =
        Set("address", "profile", "openid:gov-uk-identifiers", "openid:hmrc-enrolments", "openid:government-gateway", "email", "openid:mdtp")
      val result = await(transformer.transform(scopes, Some(authority), Some(desUserInfo), Some(enrolments), Some(userDetails)))

      val userInfoWithFormattedAddress =
        userInfo.copy(address = Some(userAddress.copy(formatted = "1 Station Road\nTown Centre\nLondon\nEngland\nUK\nNW1 6XE\nUnited Kingdom")))
      result shouldBe userInfoWithFormattedAddress
    }

    "return object not containing line 5 if line 5 isn't defined in DES response" in new Setup {

      val scopes =
        Set("address", "profile", "openid:gov-uk-identifiers", "openid:hmrc-enrolments", "openid:government-gateway", "email", "openid:mdtp")
      val result = await(
        transformer.transform(scopes,
                              Some(authority),
                              Some(desUserInfo.copy(address = Some(desAddress.copy(line5 = None)))),
                              Some(enrolments),
                              Some(userDetails)
                             )
      )

      val userInfoWithFormattedAddress =
        userInfo.copy(address = Some(userAddress.copy(formatted = "1 Station Road\nTown Centre\nLondon\nEngland\nNW1 6XE\nUnited Kingdom")))
      result shouldBe userInfoWithFormattedAddress
    }
  }
}
