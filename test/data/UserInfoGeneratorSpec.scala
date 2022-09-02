/*
 * Copyright 2022 HM Revenue & Customs
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

package data

import org.scalatest.BeforeAndAfterEach
import config.{FeatureSwitch, UserInfoFeatureSwitches}
import domain.UserInfo
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import testSupport.UnitSpec

import java.time.LocalDate

class UserInfoGeneratorSpec extends UnitSpec with ScalaCheckPropertyChecks with BeforeAndAfterEach {
  val ninoPattern = "^[A-CEGHJ-NOPR-TW-Z]{2}[0-9]{6}[ABCD\\s]{1}$".r
  val from = LocalDate.of(1939, 12, 27)
  val until = LocalDate.of(1998, 12, 29)

  override protected def beforeEach(): Unit = {
    FeatureSwitch.enable(UserInfoFeatureSwitches.countryCode)
    FeatureSwitch.enable(UserInfoFeatureSwitches.addressLine5)
  }

  override protected def afterEach(): Unit = {
    FeatureSwitch.disable(UserInfoFeatureSwitches.countryCode)
    FeatureSwitch.disable(UserInfoFeatureSwitches.addressLine5)
  }

  "userInfo" should {
    "generate an OpenID Connect compliant UserInfo response v1.0" in forAll(TestUserInfoGenerator.userInfoV1_0) { userInfo: UserInfo =>
      TestUserInfoGenerator.firstNames should contain(userInfo.given_name)
      TestUserInfoGenerator.middleNames should contain(userInfo.middle_name)
      TestUserInfoGenerator.lastNames should contain(userInfo.family_name)
      userInfo.address shouldBe TestUserInfoGenerator.fullAddress
      assertValidDob(userInfo.birthdate.getOrElse(fail(s"Generated user's dob is not defined")))
      assertValidNino(userInfo.uk_gov_nino.getOrElse(fail(s"Generated user's NINO is not defined")))
      userInfo.government_gateway.get.profile_uri should not be defined
      userInfo.government_gateway.get.group_profile_uri should not be defined
    }

    "generate an OpenID Connect compliant UserInfo response v1.1" in forAll(TestUserInfoGenerator.userInfoV1_1) { userInfo: UserInfo =>
      TestUserInfoGenerator.firstNames should contain(userInfo.given_name)
      TestUserInfoGenerator.middleNames should contain(userInfo.middle_name)
      TestUserInfoGenerator.lastNames should contain(userInfo.family_name)
      userInfo.address shouldBe TestUserInfoGenerator.fullAddress
      assertValidDob(userInfo.birthdate.getOrElse(fail(s"Generated user's dob is not defined")))
      assertValidNino(userInfo.uk_gov_nino.getOrElse(fail(s"Generated user's NINO is not defined")))
      userInfo.government_gateway.get.profile_uri shouldBe defined
      userInfo.government_gateway.get.group_profile_uri shouldBe defined
    }

    "generate an OpenID Connect compliant UserInfo response without country code when feature flag is disabled" in forAll({
      FeatureSwitch.disable(UserInfoFeatureSwitches.countryCode)
      TestUserInfoGenerator.userInfoV1_0
    }) { userInfo: UserInfo =>
      userInfo.address shouldBe TestUserInfoGenerator.addressWithToggleableFeatures(true, false)
    }

    "generate an OpenID Connect UserInfo response without addressLine5 when feature flag is disabled" in forAll({
      FeatureSwitch.disable(UserInfoFeatureSwitches.addressLine5)
      TestUserInfoGenerator.userInfoV1_0
    }) { userInfo: UserInfo =>
      userInfo.address shouldBe TestUserInfoGenerator.addressWithToggleableFeatures(false, true)
    }
  }

  private def assertValid(name: String, expected: List[String], actual: Option[String]): Unit = {
    expected should contain(actual.getOrElse(fail(s"Generated user's $name is not defined")))
  }

  private def assertValidDob(dob: LocalDate): Unit = {

    dob.isAfter(from) && dob.isBefore(until) match {
      case true  =>
      case false => fail(s"Generated user's dob: $dob is not within valid range: 1940-01-01 / 1998-12-28")
    }
  }

  private def assertValidNino(nino: String) = {
    ninoPattern.findFirstIn(nino) match {
      case Some(s) =>
      case None    => fail(s"Generated invalid user's NINO: $nino")
    }
  }
}

object TestUserInfoGenerator extends UserInfoGenerator
